package com.glassroom.music.data.local

import android.content.Context
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class OfflineAudioManager(
    context: Context,
    private val localDataSource: LocalDataSource
) {
    private val offlineDir = File(context.filesDir, "offline_audio").apply {
        if (!exists()) mkdirs()
    }

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

    private fun safeFileName(trackId: String): String {
        return trackId.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }

    private fun audioFileFor(trackId: String): File {
        return File(offlineDir, "${safeFileName(trackId)}.m4a")
    }

    private fun scanDownloadedIds(): Set<String> {
        val files = offlineDir.listFiles() ?: return emptySet()
        return files
            .filter { it.isFile && it.name.endsWith(".m4a") && it.length() > 1024L }
            .map { it.name.removeSuffix(".m4a") }
            .toSet()
    }

    private fun refreshDownloadedIds() {
        _downloadedTrackIds.value = scanDownloadedIds()
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
            localDataSource.upsertTrack(track)
            refreshDownloadedIds()
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

            val streamResult = musicRepository.resolveStreamUrl(track.id, track.title, track.artist)
            val streamUrl = streamResult.getOrNull()
                ?: return@withContext Result.failure(
                    streamResult.exceptionOrNull() ?: IllegalStateException("Gagal mendapatkan URL audio")
                )

            val defaultUa =
                "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
            val userAgent = NetworkClient.getStreamUserAgent(streamUrl, defaultUa)

            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", userAgent)
                .get()
                .build()

            downloadHttpClient.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    return@withContext Result.failure(
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

            if (!tmpFile.exists() || tmpFile.length() <= 1024L) {
                tmpFile.delete()
                return@withContext Result.failure(IllegalStateException("File audio unduhan kosong"))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }

            refreshDownloadedIds()
            Result.success(Unit)
        } catch (e: Exception) {
            tmpFile.delete()
            Result.failure(e)
        } finally {
            _downloadingTrackIds.update { it - track.id }
        }
    }

    suspend fun deleteOfflineTrack(trackId: String) = withContext(Dispatchers.IO) {
        val targetFile = audioFileFor(trackId)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val tmpFile = File(offlineDir, "${safeFileName(trackId)}.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        refreshDownloadedIds()
    }
}
