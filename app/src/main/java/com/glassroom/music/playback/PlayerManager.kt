package com.glassroom.music.playback

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.glassroom.music.data.local.OfflineAudioManager
import com.glassroom.music.data.local.PreferencesManager
import com.glassroom.music.data.remote.NetworkClient
import com.glassroom.music.domain.model.AudioStreamException
import com.glassroom.music.domain.model.Lyrics
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GlassRepeatMode {
    OFF,
    ALL,
    ONE
}

class PlayerManager(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val preferencesManager: PreferencesManager? = null,
    private val offlineAudioManager: OfflineAudioManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    @Volatile
    private var isGaplessEnabled: Boolean = true

    @Volatile
    private var isAutoplayEnabled: Boolean = true

    private var prewarmJob: Job? = null
    private var autoplayJob: Job? = null
    private var persistJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastPrewarmedTrackId: String? = null
    private var consecutiveErrorCount: Int = 0

    private val transitionWakeLock: PowerManager.WakeLock? by lazy {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Veloura::TrackTransition")?.apply {
                setReferenceCounted(false)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun acquireTransitionWakeLock() {
        try {
            transitionWakeLock?.let { wl ->
                if (!wl.isHeld) {
                    wl.acquire(30_000L)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseTransitionWakeLock() {
        try {
            transitionWakeLock?.let { wl ->
                if (wl.isHeld) {
                    wl.release()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun maxAutoSkipErrors(): Int = _queue.value.size.coerceIn(1, 3)

    init {
        try {
            val serviceIntent = Intent(context, MusicService::class.java)
            context.startService(serviceIntent)
        } catch (_: Exception) {
        }

        preferencesManager?.let { prefs ->
            scope.launch {
                launch {
                    prefs.gaplessFlow.collect { isGaplessEnabled = it }
                }
                launch {
                    prefs.autoplayFlow.collect { isAutoplayEnabled = it }
                }
                launch {
                    val savedShuffle = prefs.shuffleFlow.first()
                    _isShuffle.value = savedShuffle
                    val savedRepeatName = prefs.repeatModeFlow.first()
                    _repeatMode.value = GlassRepeatMode.entries.firstOrNull { it.name == savedRepeatName }
                        ?: GlassRepeatMode.OFF

                    val snapshot = withContext(Dispatchers.IO) { prefs.loadPlaybackSession() }
                    if (snapshot != null && _queue.value.isEmpty() && _currentTrack.value == null) {
                        val restoredTrack = snapshot.queue[snapshot.currentIndex]
                        _queue.value = snapshot.queue
                        _currentIndex.value = snapshot.currentIndex
                        _currentTrack.value = restoredTrack
                        _currentPosition.value = snapshot.positionMs
                        _duration.value = if (restoredTrack.duration > 0) restoredTrack.duration * 1000L else 0L
                        if (_isShuffle.value) {
                            rebuildShuffleOrder(snapshot.queue.size, snapshot.currentIndex)
                        }
                        fetchLyrics(restoredTrack)
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val defaultUserAgent =
            "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

        // Leave userAgent null on DefaultHttpDataSource so per-URL User-Agent headers
        // injected by ResolvingDataSource are not overwritten during connection setup.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(null)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val baseDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(baseDataSourceFactory) { dataSpec ->
            val uriStr = dataSpec.uri.toString()
            if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                val streamUa = NetworkClient.getStreamUserAgent(uriStr, defaultUserAgent)
                val headers = HashMap(dataSpec.httpRequestHeaders)
                headers["User-Agent"] = streamUa
                dataSpec.buildUpon()
                    .setHttpRequestHeaders(headers)
                    .build()
            } else {
                dataSpec
            }
        }

        val mediaSourceFactory =
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(resolvingDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true) // true handles audio focus automatically
            .setHandleAudioBecomingNoisy(true) // pauses when headphones unplugged
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private val _sleepStopAfterCurrentTrack = MutableStateFlow(false)
    val sleepStopAfterCurrentTrack: StateFlow<Boolean> = _sleepStopAfterCurrentTrack.asStateFlow()

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepStopAfterCurrentTrack.value = false
        if (minutes <= 0) {
            _sleepTimerRemainingMs.value = null
            return
        }
        val totalMs = minutes * 60_000L
        _sleepTimerRemainingMs.value = totalMs
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining -= 1000L
                _sleepTimerRemainingMs.value = remaining.coerceAtLeast(0L)
            }
            if (isActive) {
                _sleepTimerRemainingMs.value = null
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    _isPlaying.value = false
                }
            }
        }
    }

    fun setSleepAfterCurrentTrack(enabled: Boolean) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = null
        _sleepStopAfterCurrentTrack.value = enabled
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = null
        _sleepStopAfterCurrentTrack.value = false
    }

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _shuffleOrder = MutableStateFlow<List<Int>>(emptyList())
    val shuffleOrder: StateFlow<List<Int>> = _shuffleOrder.asStateFlow()

    private val _repeatMode = MutableStateFlow(GlassRepeatMode.OFF)
    val repeatMode: StateFlow<GlassRepeatMode> = _repeatMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var lyricsJob: Job? = null
    private var currentPlayToken: Long = 0L
    private var retriedPlayToken: Long = -1L

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _isLoading.value = true
                }
                Player.STATE_READY -> {
                    _isLoading.value = false
                    consecutiveErrorCount = 0
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                Player.STATE_ENDED -> {
                    _isLoading.value = false
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _isLoading.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val failedToken = currentPlayToken
            val failedTrack = _currentTrack.value
            val detailedError = formatPlaybackError(error)

            if (failedTrack != null && retriedPlayToken != failedToken) {
                retriedPlayToken = failedToken
                _isLoading.value = true
                _isPlaying.value = false
                _playbackError.value = null

                playJob?.cancel()
                playJob = scope.launch {
                    acquireTransitionWakeLock()
                    try {
                        val altResult = musicRepository.resolveAlternativeStreamUrl(
                            trackId = failedTrack.id,
                            title = failedTrack.title,
                            artist = failedTrack.artist
                        )
                        if (currentPlayToken != failedToken) return@launch

                        val altUrl = altResult.getOrNull().orEmpty()
                        if (altResult.isSuccess && isValidPlayableUrl(altUrl)) {
                            prepareAndPlayStream(failedTrack, altUrl)
                            return@launch
                        }

                        consecutiveErrorCount++
                        _isLoading.value = false
                        _isPlaying.value = false
                        _playbackError.value = detailedError
                        delay(1500)
                        if (currentPlayToken == failedToken && hasNextTrack() && consecutiveErrorCount < maxAutoSkipErrors()) {
                            skipNext()
                        }
                    } finally {
                        releaseTransitionWakeLock()
                    }
                }
                return
            }

            consecutiveErrorCount++
            _isLoading.value = false
            _isPlaying.value = false
            _playbackError.value = detailedError
            scope.launch {
                // If stream failed, skip to next after brief delay unless user switched track
                delay(1500)
                if (currentPlayToken == failedToken && hasNextTrack() && consecutiveErrorCount < maxAutoSkipErrors()) {
                    skipNext()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var ticks = 0
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _currentPosition.value = pos
                    val dur = exoPlayer.duration
                    if (dur > 0) {
                        _duration.value = dur
                        if (dur - pos in 1_000L..15_000L) {
                            prewarmNextTrackIfGapless()
                        }
                    }
                    if (++ticks % 20 == 0) {
                        persistSession()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        if (exoPlayer.mediaItemCount > 0) {
            _currentPosition.value = exoPlayer.currentPosition.coerceAtLeast(0L)
        }
        persistSession()
    }

    private fun persistSession() {
        val prefs = preferencesManager ?: return
        val q = _queue.value
        val idx = _currentIndex.value
        val pos = _currentPosition.value
        persistJob?.cancel()
        persistJob = scope.launch(Dispatchers.IO) {
            if (q.isNotEmpty()) {
                prefs.savePlaybackSession(q, idx, pos)
            }
        }
    }

    private fun rebuildShuffleOrder(size: Int, anchorIndex: Int) {
        if (size <= 0) {
            _shuffleOrder.value = emptyList()
            return
        }
        val validAnchor = anchorIndex.coerceIn(0, size - 1)
        val rest = (0 until size).filter { it != validAnchor }.shuffled()
        _shuffleOrder.value = listOf(validAnchor) + rest
    }

    private fun ensureShuffleOrder() {
        val qSize = _queue.value.size
        if (qSize <= 0) {
            _shuffleOrder.value = emptyList()
            return
        }
        val currentOrder = _shuffleOrder.value
        if (currentOrder.size != qSize || _currentIndex.value !in currentOrder) {
            rebuildShuffleOrder(qSize, _currentIndex.value)
        }
    }

    private fun getNextTrackIndex(): Int? {
        val q = _queue.value
        if (q.isEmpty()) return null
        if (_isShuffle.value) {
            val order = _shuffleOrder.value
            if (order.size != q.size) return null
            val pos = order.indexOf(_currentIndex.value)
            return if (pos >= 0 && pos + 1 < order.size) order[pos + 1] else null
        }
        val nextIdx = _currentIndex.value + 1
        return if (nextIdx < q.size) nextIdx else null
    }

    fun hasNextTrack(): Boolean {
        val q = _queue.value
        if (q.isEmpty()) return false
        if (_isShuffle.value) {
            ensureShuffleOrder()
        }
        return getNextTrackIndex() != null ||
            _repeatMode.value != GlassRepeatMode.OFF ||
            isAutoplayEnabled
    }

    fun resumeOrPlayCurrent() {
        consecutiveErrorCount = 0
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        if (exoPlayer.mediaItemCount == 0 && _currentTrack.value != null) {
            playCurrentIndex(startPositionMs = _currentPosition.value)
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        consecutiveErrorCount = 0
        val targetQueue = if (newQueue.isEmpty()) listOf(track) else newQueue
        val index = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _queue.value = targetQueue
        _currentIndex.value = index
        _currentTrack.value = track
        if (_isShuffle.value) {
            rebuildShuffleOrder(targetQueue.size, index)
        } else {
            _shuffleOrder.value = emptyList()
        }

        playCurrentIndex()
    }

    fun playQueue(newQueue: List<Track>, startIndex: Int = 0) {
        if (newQueue.isEmpty()) return
        consecutiveErrorCount = 0
        val validIndex = startIndex.coerceIn(0, newQueue.size - 1)
        _queue.value = newQueue
        _currentIndex.value = validIndex
        _currentTrack.value = newQueue[validIndex]
        if (_isShuffle.value) {
            rebuildShuffleOrder(newQueue.size, validIndex)
        } else {
            _shuffleOrder.value = emptyList()
        }

        playCurrentIndex()
    }

    fun jumpToQueueIndex(index: Int) {
        val q = _queue.value
        if (index !in q.indices) return
        consecutiveErrorCount = 0
        val prevIndex = _currentIndex.value
        _currentIndex.value = index
        if (_isShuffle.value) {
            ensureShuffleOrder()
            val order = _shuffleOrder.value
            val targetPos = order.indexOf(index)
            if (targetPos >= 0) {
                val prevPos = order.indexOf(prevIndex)
                val mutable = order.toMutableList()
                mutable.removeAt(targetPos)
                val insertPos = if (prevPos >= 0) (prevPos + 1).coerceIn(0, mutable.size) else 0
                mutable.add(insertPos, index)
                _shuffleOrder.value = mutable
            } else {
                rebuildShuffleOrder(q.size, index)
            }
        }
        playCurrentIndex()
    }

    fun playNext(track: Track) {
        val currentList = _queue.value.toMutableList()
        if (currentList.isEmpty()) {
            playTrack(track)
            return
        }
        val insertIndex = (_currentIndex.value + 1).coerceIn(0, currentList.size)
        currentList.add(insertIndex, track)
        _queue.value = currentList
        if (_isShuffle.value) {
            ensureShuffleOrder()
            val shifted = _shuffleOrder.value.map { idx -> if (idx >= insertIndex) idx + 1 else idx }.toMutableList()
            val curPos = shifted.indexOf(_currentIndex.value)
            val shuffleInsertPos = if (curPos >= 0) curPos + 1 else 0
            shifted.add(shuffleInsertPos.coerceIn(0, shifted.size), insertIndex)
            _shuffleOrder.value = shifted
        }
        persistSession()
    }

    fun addToQueue(track: Track) {
        val currentList = _queue.value.toMutableList()
        if (currentList.isEmpty()) {
            playTrack(track)
            return
        }
        currentList.add(track)
        _queue.value = currentList
        if (_isShuffle.value) {
            ensureShuffleOrder()
            _shuffleOrder.value = _shuffleOrder.value + currentList.lastIndex
        }
        persistSession()
    }

    private fun isValidPlayableUrl(url: String): Boolean {
        return url.isNotBlank() &&
            (url.startsWith("http://") || url.startsWith("https://")) &&
            !url.contains("commondatastorage.googleapis.com") &&
            !url.contains("codeskulptor")
    }

    private fun formatPlaybackError(error: PlaybackException): String {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return "Playback error: HTTP ${cause.responseCode} (${error.errorCodeName})"
            }
            if (cause is HttpDataSource.HttpDataSourceException) {
                val rootDetail = cause.cause?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: error.errorCodeName
                return "Playback error: Koneksi stream gagal ($rootDetail)"
            }
            cause = cause.cause
        }
        return "Playback error: ${error.localizedMessage ?: "Stream tidak dapat diputar"}"
    }

    private suspend fun prepareAndPlayStream(
        track: Track,
        resolvedUrl: String,
        startPositionMs: Long = 0L
    ) {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.thumbnail.isNotBlank()) android.net.Uri.parse(track.thumbnail) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(resolvedUrl)
            .setMediaMetadata(metadata)
            .build()

        withContext(Dispatchers.Main) {
            exoPlayer.setMediaItem(mediaItem)
            if (startPositionMs > 0L) {
                exoPlayer.seekTo(startPositionMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
            _isPlaying.value = true
            _currentPosition.value = startPositionMs
            _duration.value = if (track.duration > 0) track.duration * 1000L else 0L

            try {
                val serviceIntent = Intent(context, MusicService::class.java)
                context.startService(serviceIntent)
            } catch (_: Exception) {
            }
        }
    }

    private fun prewarmNextTrackIfGapless() {
        if (!isGaplessEnabled) return
        val q = _queue.value
        if (q.size <= 1) return
        if (_isShuffle.value) {
            ensureShuffleOrder()
        }
        val nextIndex = getNextTrackIndex()
            ?: if (_repeatMode.value == GlassRepeatMode.ALL && !_isShuffle.value) 0 else return
        val nextTrack = q.getOrNull(nextIndex) ?: return
        if (lastPrewarmedTrackId == nextTrack.id) return
        lastPrewarmedTrackId = nextTrack.id
        prewarmJob?.cancel()
        prewarmJob = scope.launch(Dispatchers.IO) {
            try {
                musicRepository.prewarmStreamUrl(nextTrack)
            } catch (_: Exception) {
            }
        }
    }

    private fun playCurrentIndex(startPositionMs: Long = 0L) {
        val currentList = _queue.value
        val index = _currentIndex.value
        if (index !in currentList.indices) return

        val track = currentList[index]
        val playToken = ++currentPlayToken
        lastPrewarmedTrackId = null
        _currentTrack.value = track
        _isLoading.value = true
        _playbackError.value = null
        _lyrics.value = null

        fetchLyrics(track)
        persistSession()

        playJob?.cancel()
        playJob = scope.launch {
            acquireTransitionWakeLock()
            try {
                // Save to recently played in local database
                playlistRepository.addToRecentlyPlayed(track)

                // Priority 0: Local offline audio file if user saved this track offline
                val offlineUri = offlineAudioManager?.getOfflineUri(track.id)
                if (!offlineUri.isNullOrBlank()) {
                    if (currentPlayToken != playToken) return@launch
                    prepareAndPlayStream(track, offlineUri, startPositionMs)
                    prewarmNextTrackIfGapless()
                    return@launch
                }

                // Resolve direct stream URL via repository/backend
                val isDirectPlayable = track.streamUrl.isNotBlank() &&
                        track.streamUrl.startsWith("http") &&
                        !track.streamUrl.contains("/api/stream") &&
                        !track.streamUrl.contains("googlevideo.com") &&
                        !track.streamUrl.contains("commondatastorage.googleapis.com") &&
                        !track.streamUrl.contains("codeskulptor")

                var resolvedUrl: String? = null
                var failureMessage: String? = null

                if (isDirectPlayable) {
                    resolvedUrl = track.streamUrl
                } else {
                    val res = musicRepository.resolveStreamUrl(track.id, track.title, track.artist)
                    if (currentPlayToken != playToken) return@launch
                    val direct = res.getOrNull().orEmpty()
                    if (res.isSuccess && isValidPlayableUrl(direct)) {
                        resolvedUrl = direct
                    } else {
                        // Retry once with alternative stream resolution before failing
                        val altRes = musicRepository.resolveAlternativeStreamUrl(track.id, track.title, track.artist)
                        if (currentPlayToken != playToken) return@launch
                        val altUrl = altRes.getOrNull().orEmpty()
                        if (altRes.isSuccess && isValidPlayableUrl(altUrl)) {
                            resolvedUrl = altUrl
                        } else {
                            val err = altRes.exceptionOrNull() ?: res.exceptionOrNull()
                            failureMessage = when (err) {
                                is AudioStreamException.YouTubeBotBlocked ->
                                    "YouTube menolak akses dari IP server cloud (Anti-Bot: 'Sign in to confirm you\\'re not a bot'). Silakan beralih ke backend lokal di Settings atau hosting VPS."
                                is AudioStreamException.BackendUnreachable ->
                                    err.message ?: "Tidak dapat terhubung ke server backend audio (${NetworkClient.getBaseUrl()})."
                                is AudioStreamException.TrackNotFound ->
                                    "Audio stream tidak ditemukan untuk lagu ini (${track.title})."
                                is AudioStreamException.ServerError ->
                                    "Server backend error (${err.httpCode}): ${err.message}"
                                else ->
                                    err?.message ?: if (res.isSuccess) "URL stream audio tidak valid untuk diputar."
                                    else "Gagal memutar lagu: audio stream tidak tersedia."
                            }
                        }
                    }
                }

                // Strictly prohibit dummy sample audio or empty stream
                if (resolvedUrl == null || !isValidPlayableUrl(resolvedUrl)) {
                    consecutiveErrorCount++
                    _isLoading.value = false
                    _isPlaying.value = false
                    _playbackError.value = failureMessage ?: "URL stream audio tidak valid atau dilarang."
                    delay(1500)
                    if (currentPlayToken == playToken && hasNextTrack() && consecutiveErrorCount < maxAutoSkipErrors()) {
                        skipNext()
                    }
                    return@launch
                }

                if (currentPlayToken != playToken) return@launch
                prepareAndPlayStream(track, resolvedUrl, startPositionMs)
                prewarmNextTrackIfGapless()
            } finally {
                releaseTransitionWakeLock()
            }
        }
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    fun fetchLyrics(track: Track) {
        lyricsJob?.cancel()
        lyricsJob = scope.launch {
            _isLyricsLoading.value = true
            val res = musicRepository.getLyrics(track.title, track.artist)
            _lyrics.value = res.getOrNull()
            _isLyricsLoading.value = false
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            resumeOrPlayCurrent()
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPosition.value = positionMs
        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(positionMs)
        }
        persistSession()
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_repeatMode.value == GlassRepeatMode.ONE) {
            if (exoPlayer.mediaItemCount > 0) {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            } else {
                playCurrentIndex()
            }
            return
        }

        if (_isShuffle.value) {
            ensureShuffleOrder()
        }

        val nextIndex = getNextTrackIndex()
        if (nextIndex != null) {
            _currentIndex.value = nextIndex
            playCurrentIndex()
        } else if (_repeatMode.value == GlassRepeatMode.ALL) {
            if (_isShuffle.value) {
                val remaining = q.indices.filter { it != _currentIndex.value }.shuffled()
                val nextRound = if (remaining.isNotEmpty()) remaining else listOf(0)
                _shuffleOrder.value = nextRound
                _currentIndex.value = nextRound.first()
            } else {
                _currentIndex.value = 0
            }
            playCurrentIndex()
        } else if (isAutoplayEnabled) {
            triggerAutoplaySimilarTrack(_currentTrack.value ?: q.last())
        }
    }

    fun skipPrevious() {
        if (exoPlayer.mediaItemCount > 0 && exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            _currentPosition.value = 0L
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        if (_isShuffle.value) {
            ensureShuffleOrder()
            val order = _shuffleOrder.value
            val pos = order.indexOf(_currentIndex.value)
            if (pos > 0) {
                _currentIndex.value = order[pos - 1]
                playCurrentIndex()
                return
            }
        } else if (_currentIndex.value > 0) {
            _currentIndex.value -= 1
            playCurrentIndex()
            return
        }

        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(0)
            _currentPosition.value = 0L
        } else {
            playCurrentIndex()
        }
    }

    fun toggleShuffle() {
        val newVal = !_isShuffle.value
        _isShuffle.value = newVal
        if (newVal) {
            rebuildShuffleOrder(_queue.value.size, _currentIndex.value)
        } else {
            _shuffleOrder.value = emptyList()
        }
        preferencesManager?.let { prefs ->
            scope.launch(Dispatchers.IO) { prefs.setShuffle(newVal) }
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            GlassRepeatMode.OFF -> GlassRepeatMode.ALL
            GlassRepeatMode.ALL -> GlassRepeatMode.ONE
            GlassRepeatMode.ONE -> GlassRepeatMode.OFF
        }
        _repeatMode.value = nextMode
        preferencesManager?.let { prefs ->
            scope.launch(Dispatchers.IO) { prefs.setRepeatMode(nextMode.name) }
        }
    }

    fun removeFromQueue(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            val oldCurrent = _currentIndex.value
            if (_isShuffle.value) {
                _shuffleOrder.value = _shuffleOrder.value
                    .filter { it != index }
                    .map { idx -> if (idx > index) idx - 1 else idx }
            }
            if (index < oldCurrent) {
                _currentIndex.value = oldCurrent - 1
            } else if (index == oldCurrent && currentList.isNotEmpty()) {
                val newIdx = index.coerceAtMost(currentList.size - 1)
                _currentIndex.value = newIdx
                if (_isShuffle.value) ensureShuffleOrder()
                playCurrentIndex()
                return
            }
            if (_isShuffle.value) ensureShuffleOrder()
            persistSession()
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _queue.value = currentList
            val remapIndex: (Int) -> Int = { idx ->
                when {
                    fromIndex == idx -> toIndex
                    fromIndex < idx && toIndex >= idx -> idx - 1
                    fromIndex > idx && toIndex <= idx -> idx + 1
                    else -> idx
                }
            }
            _currentIndex.value = remapIndex(_currentIndex.value)
            if (_isShuffle.value) {
                _shuffleOrder.value = _shuffleOrder.value.map(remapIndex)
            }
            persistSession()
        }
    }

    fun clearQueue() {
        playJob?.cancel()
        lyricsJob?.cancel()
        _queue.value = emptyList()
        _shuffleOrder.value = emptyList()
        _currentTrack.value = null
        _currentIndex.value = 0
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _isPlaying.value = false
        _isLoading.value = false
        preferencesManager?.let { prefs ->
            scope.launch(Dispatchers.IO) {
                prefs.savePlaybackSession(emptyList(), 0, 0L)
            }
        }
    }

    private fun triggerAutoplaySimilarTrack(seedTrack: Track) {
        val tokenSnapshot = currentPlayToken
        autoplayJob?.cancel()
        autoplayJob = scope.launch {
            acquireTransitionWakeLock()
            try {
                _isLoading.value = true
                val existingIds = _queue.value.map { it.id }.toSet()
                val cleanTitle = seedTrack.title
                    .replace(Regex("\\s*[\\(\\[].*?[\\)\\]]"), "")
                    .trim()
                val primaryQuery = listOf(seedTrack.artist, cleanTitle, "mix")
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val artistQuery = seedTrack.artist.trim()

                val searchCandidates = withContext(Dispatchers.IO) {
                    val primary = if (primaryQuery.isNotBlank()) {
                        musicRepository.search(primaryQuery, "songs").getOrNull().orEmpty()
                            .filter { it.id !in existingIds && it.title.isNotBlank() }
                    } else emptyList()

                    val byArtist = if (primary.isEmpty() && artistQuery.isNotBlank()) {
                        musicRepository.search(artistQuery, "songs").getOrNull().orEmpty()
                            .filter { it.id !in existingIds && it.title.isNotBlank() }
                    } else emptyList()

                    val rec = if (primary.isEmpty() && byArtist.isEmpty()) {
                        musicRepository.getRecommendations().getOrNull()?.get("trending").orEmpty()
                            .filter { it.id !in existingIds && it.title.isNotBlank() }
                    } else emptyList()

                    primary + byArtist + rec
                }

                if (currentPlayToken != tokenSnapshot) return@launch
                val nextPick = searchCandidates.firstOrNull()
                if (nextPick != null) {
                    val updated = _queue.value + nextPick
                    _queue.value = updated
                    if (_isShuffle.value) {
                        ensureShuffleOrder()
                        _shuffleOrder.value = _shuffleOrder.value + (updated.size - 1)
                    }
                    _currentIndex.value = updated.size - 1
                    playCurrentIndex()
                } else {
                    _isLoading.value = false
                    _isPlaying.value = false
                }
            } finally {
                releaseTransitionWakeLock()
            }
        }
    }

    private fun handleTrackEnded() {
        consecutiveErrorCount = 0
        if (_sleepStopAfterCurrentTrack.value) {
            _sleepStopAfterCurrentTrack.value = false
            _isPlaying.value = false
            return
        }
        when (_repeatMode.value) {
            GlassRepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            GlassRepeatMode.ALL -> {
                skipNext()
            }
            GlassRepeatMode.OFF -> {
                val q = _queue.value
                if (_isShuffle.value) {
                    ensureShuffleOrder()
                }
                if (getNextTrackIndex() != null) {
                    skipNext()
                } else if (isAutoplayEnabled && q.isNotEmpty()) {
                    triggerAutoplaySimilarTrack(_currentTrack.value ?: q.last())
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    fun release() {
        playJob?.cancel()
        lyricsJob?.cancel()
        progressJob?.cancel()
        prewarmJob?.cancel()
        autoplayJob?.cancel()
        persistJob?.cancel()
        sleepTimerJob?.cancel()
        releaseTransitionWakeLock()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
