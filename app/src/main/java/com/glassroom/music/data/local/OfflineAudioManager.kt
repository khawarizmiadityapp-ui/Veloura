package com.glassroom.music.data.local

import android.content.Context
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class BatchDownloadProgress(
    val playlistId: String,
    val completed: Int,
    val total: Int,
    val currentTitle: String,
    val failedCount: Int = 0
)

class OfflineAudioManager(
    context: Context,
    private val localDataSource: LocalDataSource
) {
    private val offlineDir = File(context.filesDir, "offline_audio").apply {
        if (!exists()) mkdirs()
    }

    // Maps safe filenames back to original trackIds so non-alphanumeric IDs stay consistent in UI
    private val idMapPrefs = context.getSharedPreferences("veloura_offline_id_map", Context.MODE_PRIVATE)

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batchDownloadJob: Job? = null

    private val downloadHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val _downloadedTrackIds = MutableStateFlow(scanDownloadedIds())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _offlineStorageBytes = MutableStateFlow(computeStorageBytes())
    val offlineStorageBytes: StateFlow<Long> = _offlineStorageBytes.asStateFlow()

    private val _batchProgress = MutableStateFlow<BatchDownloadProgress?>(null)
    val batchProgress: StateFlow<BatchDownloadProgress?> = _batchProgress.asStateFlow()

    private val _batchCompletionEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val batchCompletionEvents: SharedFlow<String> = _batchCompletionEvents.asSharedFlow()

    private fun safeFileName(trackId: String): String {
        return trackId.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }

    private fun audioFileFor(trackId: String): File {
        return File(offlineDir, "${safeFileName(trackId)}.m4a")
    }

    private fun resolveTrackIdFromFile(file: File): String {
        val baseName = file.name.removeSuffix(".m4a")
        return idMapPrefs.getString(baseName, null) ?: baseName
    }

    private fun saveIdMapping(trackId: String) {
        val safeName = safeFileName(trackId)
        if (idMapPrefs.getString(safeName, null) != trackId) {
            idMapPrefs.edit().putString(safeName, trackId).apply()
        }
    }

    private fun scanOfflineFiles(): List<File> {
        val files = offlineDir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.name.endsWith(".m4a") && it.length() > 1024L }
    }

    private fun scanDownloadedIds(): Set<String> {
        return scanOfflineFiles()
            .map { resolveTrackIdFromFile(it) }
            .toSet()
    }

    private fun computeStorageBytes(): Long {
        return scanOfflineFiles().sumOf { it.length() }
    }

    private fun refreshOfflineState() {
        val validFiles = scanOfflineFiles()
        _downloadedTrackIds.value = validFiles.map { resolveTrackIdFromFile(it) }.toSet()
        _offlineStorageBytes.value = validFiles.sumOf { it.length() }
    }

    fun isDownloaded(trackId: String): Boolean {
        val file = audioFileFor(trackId)
        return file.exists() && file.length() > 1024L
    }

    fun getOfflineUri(trackId: String): String? {
        val file = audioFileFor(trackId)
        return if (file.exists() && file.length() > 1024L) {
            file.toURI().toString()
        } else {
            null
        }
    }

    suspend fun downloadTrack(
        track: Track,
        musicRepository: MusicRepository
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (isDownloaded(track.id)) {
            saveIdMapping(track.id)
            localDataSource.upsertTrack(track)
            refreshOfflineState()
            return@withContext Result.success(Unit)
        }

        if (track.id in _downloadingTrackIds.value) {
            return@withContext Result.success(Unit)
        }

        _downloadingTrackIds.update { it + track.id }
        val tmpFile = File(offlineDir, "${safeFileName(track.id)}.tmp")
        val targetFile = audioFileFor(track.id)

        try {
            localDataSource.upsertTrack(track)

            val defaultUa =
                "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

            fun downloadFromUrl(streamUrl: String): Result<Unit> {
                val userAgent = NetworkClient.getStreamUserAgent(streamUrl, defaultUa)
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", userAgent)
                    .get()
                    .build()

                downloadHttpClient.newCall(request).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) {
                        return Result.failure(
                            IllegalStateException("Unduhan gagal: HTTP ${response.code}")
                        )
                    }

                    body.byteStream().use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }
                }
                return Result.success(Unit)
            }

            val streamResult = musicRepository.resolveStreamUrl(track.id, track.title, track.artist)
            val initialUrl = streamResult.getOrNull()
            var downloadOutcome: Result<Unit> = if (!initialUrl.isNullOrBlank()) {
                downloadFromUrl(initialUrl)
            } else {
                Result.failure(
                    streamResult.exceptionOrNull() ?: IllegalStateException("Gagal mendapatkan URL audio")
                )
            }

            // Retry once with a fresh/alternative stream URL if cached URL expired (e.g., HTTP 403)
            if (downloadOutcome.isFailure) {
                tmpFile.delete()
                val altResult = musicRepository.resolveAlternativeStreamUrl(track.id, track.title, track.artist)
                val altUrl = altResult.getOrNull()
                if (!altUrl.isNullOrBlank()) {
                    downloadOutcome = downloadFromUrl(altUrl)
                }
            }

            if (downloadOutcome.isFailure) {
                tmpFile.delete()
                return@withContext downloadOutcome
            }

            if (!tmpFile.exists() || tmpFile.length() <= 1024L) {
                tmpFile.delete()
                return@withContext Result.failure(IllegalStateException("File audio unduhan kosong"))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }

            saveIdMapping(track.id)
            refreshOfflineState()
            Result.success(Unit)
        } catch (e: Exception) {
            tmpFile.delete()
            Result.failure(e)
        } finally {
            _downloadingTrackIds.update { it - track.id }
        }
    }

    fun startBatchDownload(
        playlistId: String,
        tracks: List<Track>,
        musicRepository: MusicRepository
    ) {
        if (batchDownloadJob?.isActive == true && _batchProgress.value?.playlistId == playlistId) {
            return
        }
        val pending = tracks.filterNot { isDownloaded(it.id) }
        if (pending.isEmpty()) return

        batchDownloadJob?.cancel()
        batchDownloadJob = managerScope.launch {
            var successCount = 0
            var failedCount = 0
            var cancelled = false
            try {
                pending.forEachIndexed { idx, track ->
                    ensureActive()
                    _batchProgress.value = BatchDownloadProgress(
                        playlistId = playlistId,
                        completed = idx,
                        total = pending.size,
                        currentTitle = track.title,
                        failedCount = failedCount
                    )
                    val res = downloadTrack(track, musicRepository)
                    if (res.isSuccess) {
                        successCount++
                    } else {
                        failedCount++
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw ce
            } finally {
                _batchProgress.value = null
                refreshOfflineState()
                if (!cancelled && (successCount > 0 || failedCount > 0)) {
                    val summary = when {
                        failedCount == 0 -> "Berhasil mengunduh $successCount lagu untuk offline"
                        successCount == 0 -> "Gagal mengunduh $failedCount lagu (periksa koneksi)"
                        else -> "$successCount lagu diunduh, $failedCount lagu gagal"
                    }
                    _batchCompletionEvents.tryEmit(summary)
                }
            }
        }
    }

    fun cancelBatchDownload() {
        batchDownloadJob?.cancel()
        batchDownloadJob = null
        _batchProgress.value = null
    }

    suspend fun deleteOfflineTrack(trackId: String) = withContext(Dispatchers.IO) {
        val safeName = safeFileName(trackId)
        val targetFile = audioFileFor(trackId)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val tmpFile = File(offlineDir, "${safeName}.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        idMapPrefs.edit().remove(safeName).apply()
        refreshOfflineState()
    }

    suspend fun deleteAllOfflineTracks(): Int = withContext(Dispatchers.IO) {
        cancelBatchDownload()
        val files = offlineDir.listFiles() ?: return@withContext 0
        var removedCount = 0
        files.forEach { file ->
            if (file.isFile) {
                val isAudio = file.name.endsWith(".m4a") && file.length() > 1024L
                if (file.delete() && isAudio) {
                    removedCount++
                }
            }
        }
        idMapPrefs.edit().clear().apply()
        refreshOfflineState()
        removedCount
    }
}
