package com.glassroom.music.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Lyrics
import com.glassroom.music.domain.model.Track
import com.glassroom.music.playback.PlayerManager
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.queue.QueueSheet
import com.glassroom.music.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullPlayerScreen(
    playerManager: PlayerManager,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCollapse: () -> Unit,
    onMoreOptions: ((Track) -> Unit)? = null,
    isTrackDownloaded: Boolean = false,
    isTrackDownloading: Boolean = false,
    onToggleOfflineDownload: ((Track) -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    onShareTrack: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
    val shuffleOrder by playerManager.shuffleOrder.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val lyrics by playerManager.lyrics.collectAsState()
    val isLyricsLoading by playerManager.isLyricsLoading.collectAsState()
    val isLoading by playerManager.isLoading.collectAsState()
    val playbackError by playerManager.playbackError.collectAsState()
    val sleepTimerRemainingMs by playerManager.sleepTimerRemainingMs.collectAsState()
    val sleepStopAfterTrack by playerManager.sleepStopAfterCurrentTrack.collectAsState()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var artworkDragX by remember { mutableFloatStateOf(0f) }
    val animatedArtworkOffsetX by animateFloatAsState(
        targetValue = artworkDragX,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "artwork_swipe_x"
    )

    var collapseDragY by remember { mutableFloatStateOf(0f) }
    val animatedCollapseY by animateFloatAsState(
        targetValue = collapseDragY,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
        label = "full_collapse_y"
    )

    // Keep collapseDragY in place while AnimatedVisibility slides FullPlayer out (prevents snap-back flicker)
    val triggerCollapse: () -> Unit = {
        onCollapse()
    }

    val swipeDownModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            collapseDragY = (collapseDragY + delta).coerceIn(0f, 360f)
        },
        onDragStopped = {
            if (collapseDragY > 100f) {
                triggerCollapse()
            } else {
                collapseDragY = 0f
            }
        }
    )

    val artworkScale: Float = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "artwork_float")
        val breathing by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.025f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale_anim"
        )
        breathing
    } else {
        val resting by animateFloatAsState(
            targetValue = 0.98f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "scale_rest_anim"
        )
        resting
    }

    val glassConfig = LocalGlassConfig.current
    val artworkColors = rememberAnimatedArtworkColors(track = currentTrack)
    val context = LocalContext.current

    val artworkSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = "track_artwork"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = 0.86f,
                        stiffness = 380f
                    )
                }
            )
        }
    } else {
        Modifier
    }

    if (currentTrack == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada lagu dipilih",
                style = GlassTypography.bodyLarge,
                color = glassConfig.textSecondary
            )
        }
        return
    }

    val activeTrack = currentTrack!!
    val handleShareTrack: () -> Unit = {
        if (onShareTrack != null) {
            onShareTrack(activeTrack)
        } else {
            shareTrack(context, activeTrack)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glassConfig.baseSolidColor)
    ) {
        VelouraBackdrop(
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            isFullPlayer = true,
            artworkColors = artworkColors
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        glassConfig.isPaper -> Color(0x28FAF6EE)
                        glassConfig.isDark -> Color(0x4D070A10)
                        else -> Color(0x40FFFFFF)
                    }
                )
        )
        // Soft ambient glow circle behind artwork
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(360.dp)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            artworkColors.primary.copy(
                                alpha = when {
                                    glassConfig.isPaper -> 0.14f
                                    glassConfig.isDark -> 0.50f
                                    else -> 0.32f
                                }
                            ),
                            artworkColors.secondary.copy(
                                alpha = when {
                                    glassConfig.isPaper -> 0.08f
                                    glassConfig.isDark -> 0.26f
                                    else -> 0.16f
                                }
                            ),
                            Color.Transparent
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer { translationY = animatedCollapseY }
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth >= 520.dp
            val isCompactHeight = maxHeight < 660.dp

            if (isLandscape) {
                val artworkSize = (maxHeight * 0.56f).coerceIn(160.dp, 248.dp)
                val lyricsHeight = (maxHeight * 0.64f).coerceIn(180.dp, 280.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    FullPlayerTopBar(
                        albumName = activeTrack.album,
                        showLyrics = showLyrics,
                        onCollapse = triggerCollapse,
                        onToggleLyrics = { showLyrics = !showLyrics },
                        onOpenQueue = { showQueueSheet = true },
                        modifier = swipeDownModifier
                    )

                    FullPlayerErrorBanner(
                        errorMessage = playbackError,
                        onDismiss = { playerManager.clearPlaybackError() }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(0.45f),
                            contentAlignment = Alignment.Center
                        ) {
                            FullPlayerCenterMedia(
                                track = activeTrack,
                                isPlaying = isPlaying,
                                showLyrics = showLyrics,
                                lyrics = lyrics,
                                isLyricsLoading = isLyricsLoading,
                                currentPositionFlow = playerManager.currentPosition,
                                artworkSize = artworkSize,
                                lyricsHeight = lyricsHeight,
                                artworkScale = artworkScale,
                                animatedArtworkOffsetX = animatedArtworkOffsetX,
                                artworkColors = artworkColors,
                                artworkSharedModifier = artworkSharedModifier,
                                swipeDownModifier = swipeDownModifier,
                                onArtworkDrag = { delta ->
                                    artworkDragX = (artworkDragX + delta).coerceIn(-180f, 180f)
                                },
                                onArtworkDragStop = {
                                    if (artworkDragX <= -95f) playerManager.skipNext()
                                    else if (artworkDragX >= 95f) playerManager.skipPrevious()
                                    artworkDragX = 0f
                                },
                                onSeekLyrics = { playerManager.seekTo(it) },
                                onRetryLyrics = { playerManager.fetchLyrics(activeTrack) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            FullPlayerMetadataRow(
                                track = activeTrack,
                                isFavorite = isFavorite,
                                onToggleFavorite = onToggleFavorite,
                                onArtistClick = onArtistClick?.let { callback ->
                                    { artistName ->
                                        triggerCollapse()
                                        callback(artistName)
                                    }
                                },
                                onMoreOptions = onMoreOptions,
                                modifier = swipeDownModifier
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FullPlayerSliderSection(
                                trackId = activeTrack.id,
                                currentPositionFlow = playerManager.currentPosition,
                                durationFlow = playerManager.duration,
                                onSeek = { playerManager.seekTo(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            GlassPlayerControl(
                                isPlaying = isPlaying,
                                isShuffle = isShuffle,
                                repeatMode = repeatMode,
                                isLoading = isLoading,
                                onPlayPause = { playerManager.togglePlayPause() },
                                onPrevious = { playerManager.skipPrevious() },
                                onNext = { playerManager.skipNext() },
                                onToggleShuffle = { playerManager.toggleShuffle() },
                                onToggleRepeat = { playerManager.toggleRepeat() }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FullPlayerUtilityPills(
                                sleepTimerRemainingMs = sleepTimerRemainingMs,
                                sleepStopAfterTrack = sleepStopAfterTrack,
                                isTrackDownloaded = isTrackDownloaded,
                                isTrackDownloading = isTrackDownloading,
                                showOfflineOption = onToggleOfflineDownload != null,
                                onOpenSleepTimer = { showSleepTimerDialog = true },
                                onToggleOfflineDownload = { onToggleOfflineDownload?.invoke(activeTrack) },
                                onShareTrack = handleShareTrack
                            )
                        }
                    }
                }
            } else {
                val artworkSize = (maxHeight * 0.34f).coerceIn(180.dp, 268.dp)
                val lyricsHeight = (maxHeight * 0.42f).coerceIn(220.dp, 350.dp)
                val verticalScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCompactHeight) Modifier.verticalScroll(verticalScrollState) else Modifier)
                        .padding(horizontal = 24.dp, vertical = if (isCompactHeight) 10.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isCompactHeight) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween
                ) {
                    FullPlayerTopBar(
                        albumName = activeTrack.album,
                        showLyrics = showLyrics,
                        onCollapse = triggerCollapse,
                        onToggleLyrics = { showLyrics = !showLyrics },
                        onOpenQueue = { showQueueSheet = true },
                        modifier = swipeDownModifier
                    )

                    FullPlayerErrorBanner(
                        errorMessage = playbackError,
                        onDismiss = { playerManager.clearPlaybackError() }
                    )

                    if (!isCompactHeight) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    FullPlayerCenterMedia(
                        track = activeTrack,
                        isPlaying = isPlaying,
                        showLyrics = showLyrics,
                        lyrics = lyrics,
                        isLyricsLoading = isLyricsLoading,
                        currentPositionFlow = playerManager.currentPosition,
                        artworkSize = artworkSize,
                        lyricsHeight = lyricsHeight,
                        artworkScale = artworkScale,
                        animatedArtworkOffsetX = animatedArtworkOffsetX,
                        artworkColors = artworkColors,
                        artworkSharedModifier = artworkSharedModifier,
                        swipeDownModifier = swipeDownModifier,
                        onArtworkDrag = { delta ->
                            artworkDragX = (artworkDragX + delta).coerceIn(-180f, 180f)
                        },
                        onArtworkDragStop = {
                            if (artworkDragX <= -95f) playerManager.skipNext()
                            else if (artworkDragX >= 95f) playerManager.skipPrevious()
                            artworkDragX = 0f
                        },
                        onSeekLyrics = { playerManager.seekTo(it) },
                        onRetryLyrics = { playerManager.fetchLyrics(activeTrack) }
                    )

                    if (!isCompactHeight) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    FullPlayerMetadataRow(
                        track = activeTrack,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onArtistClick = onArtistClick?.let { callback ->
                            { artistName ->
                                triggerCollapse()
                                callback(artistName)
                            }
                        },
                        onMoreOptions = onMoreOptions,
                        modifier = swipeDownModifier
                    )

                    if (!isCompactHeight) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    FullPlayerSliderSection(
                        trackId = activeTrack.id,
                        currentPositionFlow = playerManager.currentPosition,
                        durationFlow = playerManager.duration,
                        onSeek = { playerManager.seekTo(it) }
                    )

                    if (!isCompactHeight) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    GlassPlayerControl(
                        isPlaying = isPlaying,
                        isShuffle = isShuffle,
                        repeatMode = repeatMode,
                        isLoading = isLoading,
                        onPlayPause = { playerManager.togglePlayPause() },
                        onPrevious = { playerManager.skipPrevious() },
                        onNext = { playerManager.skipNext() },
                        onToggleShuffle = { playerManager.toggleShuffle() },
                        onToggleRepeat = { playerManager.toggleRepeat() }
                    )

                    if (!isCompactHeight) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    FullPlayerUtilityPills(
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        sleepStopAfterTrack = sleepStopAfterTrack,
                        isTrackDownloaded = isTrackDownloaded,
                        isTrackDownloading = isTrackDownloading,
                        showOfflineOption = onToggleOfflineDownload != null,
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        onToggleOfflineDownload = { onToggleOfflineDownload?.invoke(activeTrack) },
                        onShareTrack = handleShareTrack
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                sleepStopAfterTrack = sleepStopAfterTrack,
                onSelectMinutes = { mins ->
                    playerManager.setSleepTimer(mins)
                    showSleepTimerDialog = false
                },
                onSelectEndOfTrack = {
                    playerManager.setSleepAfterCurrentTrack(true)
                    showSleepTimerDialog = false
                },
                onCancelTimer = {
                    playerManager.cancelSleepTimer()
                    showSleepTimerDialog = false
                },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        if (showQueueSheet) {
            QueueSheet(
                queue = queue,
                currentIndex = currentIndex,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                isShuffle = isShuffle,
                shuffleOrder = shuffleOrder,
                onTrackSelect = { index ->
                    playerManager.jumpToQueueIndex(index)
                    showQueueSheet = false
                },
                onRemoveFromQueue = { playerManager.removeFromQueue(it) },
                onClearQueue = {
                    playerManager.clearQueue()
                    showQueueSheet = false
                },
                onDismiss = { showQueueSheet = false },
                onReorderQueue = { fromIdx, toIdx ->
                    playerManager.reorderQueue(fromIdx, toIdx)
                }
            )
        }
    }
}

@Composable
private fun FullPlayerTopBar(
    albumName: String,
    showLyrics: Boolean,
    onCollapse: () -> Unit,
    onToggleLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalGlassConfig.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.Filled.KeyboardArrowDown,
            onClick = onCollapse,
            size = 44.dp,
            iconSize = 28.dp,
            contentDescription = "Tutup Pemutar"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onCollapse)
                .padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(glassConfig.inactiveTrackColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "DIPUTAR DARI",
                style = GlassTypography.labelSmall,
                color = glassConfig.textTertiary,
                fontSize = 10.sp
            )
            Text(
                text = albumName.ifBlank { "Veloura Audio" },
                style = GlassTypography.labelLarge,
                color = glassConfig.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val lyricsBtnScale by animateFloatAsState(
                targetValue = if (showLyrics) 1.06f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "lyrics_btn_scale"
            )

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = lyricsBtnScale
                    scaleY = lyricsBtnScale
                }
            ) {
                GlassIconButton(
                    icon = Icons.Filled.Lyrics,
                    onClick = onToggleLyrics,
                    size = 44.dp,
                    iconSize = 22.dp,
                    tint = if (showLyrics) glassConfig.accentSecondary else glassConfig.textTertiary,
                    backgroundBrush = if (showLyrics) glassConfig.toggleActiveBrush else glassConfig.cardBrush,
                    contentDescription = "Lirik"
                )
            }

            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = onOpenQueue,
                size = 44.dp,
                iconSize = 22.dp,
                contentDescription = "Buka Antrean"
            )
        }
    }
}

@Composable
private fun FullPlayerErrorBanner(
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (glassConfig.isDark) Color(0x4DF43F5E) else Color(0x26F43F5E))
                .border(1.dp, Color(0x66F43F5E), RoundedCornerShape(16.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = errorMessage ?: "",
                    style = GlassTypography.labelSmall,
                    color = if (glassConfig.isDark) Color(0xFFFFD1D8) else Color(0xFF9F1239),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FullPlayerSliderSection(
    trackId: String,
    currentPositionFlow: StateFlow<Long>,
    durationFlow: StateFlow<Long>,
    onSeek: (Long) -> Unit
) {
    val currentPosition by currentPositionFlow.collectAsState()
    val duration by durationFlow.collectAsState()
    GlassSlider(
        positionMs = currentPosition,
        durationMs = duration,
        onSeek = onSeek,
        trackId = trackId
    )
}

@Composable
private fun FullPlayerCenterMedia(
    track: Track,
    isPlaying: Boolean,
    showLyrics: Boolean,
    lyrics: Lyrics?,
    isLyricsLoading: Boolean,
    currentPositionFlow: StateFlow<Long>,
    artworkSize: Dp,
    lyricsHeight: Dp,
    artworkScale: Float,
    animatedArtworkOffsetX: Float,
    artworkColors: ArtworkColors,
    artworkSharedModifier: Modifier,
    swipeDownModifier: Modifier,
    onArtworkDrag: (Float) -> Unit,
    onArtworkDragStop: () -> Unit,
    onSeekLyrics: (Long) -> Unit,
    onRetryLyrics: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    AnimatedContent(
        targetState = showLyrics,
        transitionSpec = {
            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(250))
        },
        label = "artwork_lyrics_switch",
        modifier = Modifier.fillMaxWidth()
    ) { isLyricsMode ->
        if (isLyricsMode) {
            val currentPositionMs by currentPositionFlow.collectAsState()
            GlassLyricsView(
                lyrics = lyrics,
                isLoading = isLyricsLoading,
                currentPositionMs = currentPositionMs,
                onSeek = onSeekLyrics,
                onRetry = onRetryLyrics,
                isPlaying = isPlaying,
                containerHeight = lyricsHeight
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(swipeDownModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = artworkSharedModifier
                        .size(artworkSize)
                        .graphicsLayer {
                            translationX = animatedArtworkOffsetX
                            scaleX = artworkScale
                            scaleY = artworkScale
                        }
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState(onArtworkDrag),
                            onDragStopped = { onArtworkDragStop() }
                        )
                        .shadow(
                            elevation = if (glassConfig.isPaper) 16.dp else 28.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = if (glassConfig.isPaper) glassConfig.shadowAmbient else artworkColors.primary.copy(alpha = if (glassConfig.isDark) 0.55f else 0.25f),
                            spotColor = if (glassConfig.isPaper) glassConfig.shadowSpot else artworkColors.secondary.copy(alpha = if (glassConfig.isDark) 0.40f else 0.18f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(brush = glassConfig.cardBrush)
                        .border(
                            width = 1.5.dp,
                            brush = glassConfig.borderBrush,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                            .background(glassConfig.placeholderBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = track.thumbnail,
                            transitionSpec = {
                                fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                            },
                            label = "full_artwork_crossfade"
                        ) { thumb ->
                            if (thumb.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(thumb)
                                        .crossfade(true)
                                        .memoryCacheKey(track.id.ifBlank { thumb })
                                        .build(),
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(glassConfig.placeholderBrush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = glassConfig.textSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullPlayerMetadataRow(
    track: Track,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onArtistClick: ((String) -> Unit)?,
    onMoreOptions: ((Track) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalGlassConfig.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AnimatedContent(
            targetState = track.title to track.artist,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(280)) { height -> height / 2 } + fadeIn(tween(280)))
                    .togetherWith(slideOutVertically(animationSpec = tween(220)) { height -> -height / 2 } + fadeOut(tween(220)))
            },
            modifier = Modifier.weight(1f),
            label = "full_metadata_anim"
        ) { (title, artist) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = GlassTypography.headlineLarge,
                    color = glassConfig.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val canJumpArtist = onArtistClick != null && artist.isNotBlank()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (canJumpArtist) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onArtistClick?.invoke(artist) }
                            .padding(end = 4.dp, top = 2.dp, bottom = 2.dp)
                    } else {
                        Modifier
                    }
                ) {
                    Text(
                        text = artist,
                        style = GlassTypography.bodyLarge,
                        color = glassConfig.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (canJumpArtist) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Cari Artis",
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        val favScale by animateFloatAsState(
            targetValue = if (isFavorite) 1.15f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fav_scale"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = favScale
                    scaleY = favScale
                }
            ) {
                GlassIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    onClick = onToggleFavorite,
                    tint = if (isFavorite) glassConfig.heartActive else glassConfig.textTertiary,
                    backgroundBrush = if (isFavorite) {
                        Brush.verticalGradient(
                            when {
                                glassConfig.isPaper -> listOf(Color(0xFFFEE5D9), Color(0xFFFCD5C2))
                                glassConfig.isDark -> listOf(Color(0x40EF4444), Color(0x20F43F5E))
                                else -> listOf(Color(0xFFFFE4E6), Color(0xFFFECDD3))
                            }
                        )
                    } else glassConfig.cardBrush,
                    size = 46.dp,
                    iconSize = 23.dp,
                    contentDescription = "Favorit"
                )
            }

            if (onMoreOptions != null) {
                GlassIconButton(
                    icon = Icons.Filled.MoreVert,
                    onClick = { onMoreOptions(track) },
                    size = 46.dp,
                    iconSize = 22.dp,
                    contentDescription = "Opsi Lagu"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullPlayerUtilityPills(
    sleepTimerRemainingMs: Long?,
    sleepStopAfterTrack: Boolean,
    isTrackDownloaded: Boolean,
    isTrackDownloading: Boolean,
    showOfflineOption: Boolean,
    onOpenSleepTimer: () -> Unit,
    onToggleOfflineDownload: () -> Unit,
    onShareTrack: () -> Unit
) {
    val isTimerActive = sleepStopAfterTrack || sleepTimerRemainingMs != null
    val timerLabel = when {
        sleepStopAfterTrack -> "Akhir lagu"
        sleepTimerRemainingMs != null -> {
            val totalSec = (sleepTimerRemainingMs / 1000L).coerceAtLeast(0L)
            "%d:%02d".format(totalSec / 60L, totalSec % 60L)
        }
        else -> "Sleep Timer"
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FullPlayerUtilityPill(
            icon = Icons.Filled.Bedtime,
            label = timerLabel,
            isActive = isTimerActive,
            isLoading = false,
            onClick = onOpenSleepTimer
        )

        if (showOfflineOption) {
            FullPlayerUtilityPill(
                icon = if (isTrackDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                label = when {
                    isTrackDownloading -> "Mengunduh..."
                    isTrackDownloaded -> "Tersimpan"
                    else -> "Offline"
                },
                isActive = isTrackDownloaded,
                isLoading = isTrackDownloading,
                onClick = onToggleOfflineDownload
            )
        }

        FullPlayerUtilityPill(
            icon = Icons.Filled.Share,
            label = "Bagikan",
            isActive = false,
            isLoading = false,
            onClick = onShareTrack
        )
    }
}

@Composable
private fun FullPlayerUtilityPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(brush = if (isActive) glassConfig.toggleActiveBrush else glassConfig.cardBrush)
            .border(
                width = 1.dp,
                color = if (isActive) glassConfig.accentSecondary else glassConfig.borderHighlight,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 2.dp,
                    color = glassConfig.accentPrimary
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) glassConfig.accentSecondary else glassConfig.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = GlassTypography.labelSmall,
                color = if (isActive) glassConfig.accentSecondary else glassConfig.textSecondary
            )
        }
    }
}

@Composable
private fun SleepTimerDialog(
    sleepTimerRemainingMs: Long?,
    sleepStopAfterTrack: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    val presetMinutes = listOf(10, 15, 30, 45, 60)

    GlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Sleep Timer",
                style = GlassTypography.headlineMedium,
                color = glassConfig.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hentikan pemutaran musik secara otomatis",
                style = GlassTypography.bodyMedium,
                color = glassConfig.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presetMinutes.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { mins ->
                            GlassChip(
                                text = "$mins mnt",
                                isSelected = false,
                                onClick = { onSelectMinutes(mins) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                GlassChip(
                    text = "Setelah lagu ini selesai",
                    isSelected = sleepStopAfterTrack,
                    onClick = onSelectEndOfTrack,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sleepTimerRemainingMs != null || sleepStopAfterTrack) {
                    GlassButton(
                        text = "Matikan Timer",
                        onClick = onCancelTimer,
                        modifier = Modifier.weight(1f)
                    )
                }
                GlassButton(
                    text = "Tutup",
                    isPrimary = true,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
