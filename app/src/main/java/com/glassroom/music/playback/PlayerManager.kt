package com.glassroom.music.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
    private val playlistRepository: PlaylistRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true) // true handles audio focus automatically
            .setHandleAudioBecomingNoisy(true) // pauses when headphones unplugged
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

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(GlassRepeatMode.OFF)
    val repeatMode: StateFlow<GlassRepeatMode> = _repeatMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var progressJob: Job? = null

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
            _isLoading.value = false
            _isPlaying.value = false
            // Fallback play next or try resolved alternative
            scope.launch {
                // If stream failed, skip to next after brief delay
                delay(1000)
                if (_queue.value.isNotEmpty() && _currentIndex.value < _queue.value.size - 1) {
                    skipNext()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration
                    if (dur > 0) {
                        _duration.value = dur
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        _currentPosition.value = exoPlayer.currentPosition.coerceAtLeast(0L)
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        val targetQueue = if (newQueue.isEmpty()) listOf(track) else newQueue
        val index = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _queue.value = targetQueue
        _currentIndex.value = index
        _currentTrack.value = track

        playCurrentIndex()
    }

    fun playQueue(newQueue: List<Track>, startIndex: Int = 0) {
        if (newQueue.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, newQueue.size - 1)
        _queue.value = newQueue
        _currentIndex.value = validIndex
        _currentTrack.value = newQueue[validIndex]

        playCurrentIndex()
    }

    private fun playCurrentIndex() {
        val currentList = _queue.value
        val index = _currentIndex.value
        if (index !in currentList.indices) return

        val track = currentList[index]
        _currentTrack.value = track
        _isLoading.value = true

        scope.launch {
            // Save to recently played in local database
            playlistRepository.addToRecentlyPlayed(track)

            // Resolve direct stream URL via repository/backend
            val isDirectCdn = track.streamUrl.isNotBlank() &&
                    track.streamUrl.startsWith("http") &&
                    !track.streamUrl.contains("/api/stream") &&
                    !track.streamUrl.contains("commondatastorage.googleapis.com")

            val resolvedUrl = if (isDirectCdn) {
                track.streamUrl
            } else {
                val res = musicRepository.resolveStreamUrl(track.id)
                val direct = res.getOrNull()
                if (!direct.isNullOrBlank() && direct.startsWith("http") && !direct.contains("/api/stream")) {
                    direct
                } else {
                    track.streamUrl
                }
            }

            if (resolvedUrl.isBlank()) {
                _isLoading.value = false
                return@launch
            }

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
                exoPlayer.prepare()
                exoPlayer.play()
                _isPlaying.value = true
                _currentPosition.value = 0L
                _duration.value = if (track.duration > 0) track.duration * 1000L else 0L

                try {
                    val serviceIntent = android.content.Intent(context, MusicService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPosition.value = positionMs
        exoPlayer.seekTo(positionMs)
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_repeatMode.value == GlassRepeatMode.ONE) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
            return
        }

        val nextIndex = if (_isShuffle.value) {
            val randomIndices = q.indices.filter { it != _currentIndex.value }
            if (randomIndices.isNotEmpty()) randomIndices.random() else _currentIndex.value
        } else {
            _currentIndex.value + 1
        }

        if (nextIndex < q.size) {
            _currentIndex.value = nextIndex
            playCurrentIndex()
        } else if (_repeatMode.value == GlassRepeatMode.ALL) {
            _currentIndex.value = 0
            playCurrentIndex()
        }
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        val prevIndex = (_currentIndex.value - 1).coerceAtLeast(0)
        _currentIndex.value = prevIndex
        playCurrentIndex()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            GlassRepeatMode.OFF -> GlassRepeatMode.ALL
            GlassRepeatMode.ALL -> GlassRepeatMode.ONE
            GlassRepeatMode.ONE -> GlassRepeatMode.OFF
        }
    }

    fun removeFromQueue(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            if (index < _currentIndex.value) {
                _currentIndex.value -= 1
            } else if (index == _currentIndex.value && currentList.isNotEmpty()) {
                val newIdx = index.coerceAtMost(currentList.size - 1)
                _currentIndex.value = newIdx
                playCurrentIndex()
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _queue.value = currentList
            if (fromIndex == _currentIndex.value) {
                _currentIndex.value = toIndex
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentTrack.value = null
        _currentIndex.value = 0
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _isPlaying.value = false
    }

    private fun handleTrackEnded() {
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
                if (_currentIndex.value < q.size - 1) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
