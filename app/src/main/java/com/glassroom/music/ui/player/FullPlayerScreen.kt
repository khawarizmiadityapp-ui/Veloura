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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Track
import com.glassroom.music.playback.GlassRepeatMode
import com.glassroom.music.playback.PlayerManager
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.queue.QueueSheet
import com.glassroom.music.ui.theme.*

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
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
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

    // Artwork floating and subtle pulsing animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "artwork_float")
    val artworkScale by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 1.0f else 0.98f,
        targetValue = if (isPlaying) 1.025f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    // Swipe down to collapse gesture
    val swipeDownState = rememberDraggableState { delta ->
        if (delta > 12) { // User swiped down smoothly
            onCollapse()
        }
    }

    val glassConfig = LocalGlassConfig.current
    val artworkColors = rememberAnimatedArtworkColors(track = currentTrack)

    // Artwork shared element modifier with fluid coordinated spring
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
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Belum ada lagu dipilih", style = GlassTypography.bodyLarge, color = glassConfig.textSecondary)
        }
        return
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
        // Soft ambient glow background circle behind artwork inspired by current album colors
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(360.dp)
                .scale(artworkScale)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Collapse arrow down, playing from indicator, Queue button (draggable downwards to collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(state = swipeDownState, orientation = Orientation.Vertical),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    onClick = onCollapse,
                    size = 44.dp,
                    iconSize = 28.dp,
                    contentDescription = "Collapse Player"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(onClick = onCollapse)
                        .padding(horizontal = 8.dp)
                ) {
                    // Subtle drag handle pill
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
                        text = if (currentTrack?.album.isNullOrBlank()) "Veloura Audio" else currentTrack!!.album,
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

                    Box(modifier = Modifier.scale(lyricsBtnScale)) {
                        GlassIconButton(
                            icon = Icons.Filled.Lyrics,
                            onClick = { showLyrics = !showLyrics },
                            size = 44.dp,
                            iconSize = 22.dp,
                            tint = if (showLyrics) glassConfig.accentSecondary else glassConfig.textTertiary,
                            backgroundBrush = if (showLyrics) {
                                glassConfig.toggleActiveBrush
                            } else glassConfig.cardBrush,
                            contentDescription = "Toggle Lyrics"
                        )
                    }

                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        onClick = { showQueueSheet = true },
                        size = 44.dp,
                        iconSize = 22.dp,
                        contentDescription = "Open Queue"
                    )
                }
            }

            // Playback Error Banner (Shown when backend is offline or stream fails)
            AnimatedVisibility(
                visible = playbackError != null,
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
                        .clickable { playerManager.clearPlaybackError() }
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
                            text = playbackError ?: "",
                            style = GlassTypography.labelSmall,
                            color = if (glassConfig.isDark) Color(0xFFFFD1D8) else Color(0xFF9F1239),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center: Animated switch between Artwork and Live Glass Lyrics View
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "artwork_lyrics_switch",
                modifier = Modifier.fillMaxWidth()
            ) { isLyricsMode ->
                if (isLyricsMode) {
                    GlassLyricsView(
                        lyrics = lyrics,
                        isLoading = isLyricsLoading,
                        currentPositionMs = currentPosition,
                        onSeek = { playerManager.seekTo(it) },
                        onRetry = { currentTrack?.let { playerManager.fetchLyrics(it) } }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = artworkSharedModifier
                                .size(268.dp)
                                .offset { IntOffset(animatedArtworkOffsetX.roundToInt(), 0) }
                                .scale(artworkScale)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        artworkDragX = (artworkDragX + delta).coerceIn(-180f, 180f)
                                    },
                                    onDragStopped = {
                                        if (artworkDragX <= -95f) {
                                            playerManager.skipNext()
                                        } else if (artworkDragX >= 95f) {
                                            playerManager.skipPrevious()
                                        }
                                        artworkDragX = 0f
                                    }
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
                                    targetState = currentTrack?.thumbnail,
                                    transitionSpec = {
                                        fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                                    },
                                    label = "full_artwork_crossfade"
                                ) { thumb ->
                                    if (!thumb.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(thumb)
                                                .crossfade(true)
                                                .memoryCacheKey(currentTrack?.id ?: thumb)
                                                .build(),
                                            contentDescription = currentTrack?.title,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Track Title, Artist & Favorite Button with smooth animation on change
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = (currentTrack?.title ?: "") to (currentTrack?.artist ?: ""),
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
                        Text(
                            text = artist,
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    Box(modifier = Modifier.scale(favScale)) {
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
                            contentDescription = "Favorite"
                        )
                    }

                    if (onMoreOptions != null && currentTrack != null) {
                        GlassIconButton(
                            icon = Icons.Filled.MoreVert,
                            onClick = { currentTrack?.let(onMoreOptions) },
                            size = 46.dp,
                            iconSize = 22.dp,
                            contentDescription = "Track Options"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seekbar with smoothly animated thumb and track
            GlassSlider(
                positionMs = currentPosition,
                durationMs = duration,
                onSeek = { playerManager.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Controls with loading indicator and debounced taps
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

            // Compact Utility Pill Row: Sleep Timer & Offline Audio Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isTimerActive = sleepStopAfterTrack || sleepTimerRemainingMs != null
                val timerLabel = when {
                    sleepStopAfterTrack -> "Akhir lagu"
                    sleepTimerRemainingMs != null -> {
                        val totalSec = ((sleepTimerRemainingMs ?: 0L) / 1000L).coerceAtLeast(0L)
                        val m = totalSec / 60L
                        val s = totalSec % 60L
                        "%d:%02d".format(m, s)
                    }
                    else -> "Sleep Timer"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = if (isTimerActive) glassConfig.toggleActiveBrush else glassConfig.cardBrush
                        )
                        .border(
                            width = 1.dp,
                            color = if (isTimerActive) glassConfig.accentSecondary else glassConfig.borderHighlight,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { showSleepTimerDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (isTimerActive) glassConfig.accentSecondary else glassConfig.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = timerLabel,
                            style = GlassTypography.labelSmall,
                            color = if (isTimerActive) glassConfig.accentSecondary else glassConfig.textSecondary
                        )
                    }
                }

                if (onToggleOfflineDownload != null && currentTrack != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = if (isTrackDownloaded) glassConfig.toggleActiveBrush else glassConfig.cardBrush
                            )
                            .border(
                                width = 1.dp,
                                color = if (isTrackDownloaded) glassConfig.accentSecondary else glassConfig.borderHighlight,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { currentTrack?.let(onToggleOfflineDownload) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isTrackDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = glassConfig.accentPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = if (isTrackDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = "Offline Audio",
                                    tint = if (isTrackDownloaded) glassConfig.accentSecondary else glassConfig.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = when {
                                    isTrackDownloading -> "Mengunduh..."
                                    isTrackDownloaded -> "Tersimpan"
                                    else -> "Offline"
                                },
                                style = GlassTypography.labelSmall,
                                color = if (isTrackDownloaded) glassConfig.accentSecondary else glassConfig.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        if (showSleepTimerDialog) {
            val presetMinutes = listOf(10, 15, 30, 45, 60)
            GlassDialog(onDismissRequest = { showSleepTimerDialog = false }) {
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
                                        onClick = {
                                            playerManager.setSleepTimer(mins)
                                            showSleepTimerDialog = false
                                        },
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
                            onClick = {
                                playerManager.setSleepAfterCurrentTrack(true)
                                showSleepTimerDialog = false
                            },
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
                                onClick = {
                                    playerManager.cancelSleepTimer()
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        GlassButton(
                            text = "Tutup",
                            isPrimary = true,
                            onClick = { showSleepTimerDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Play Queue Modal Bottom Sheet
        if (showQueueSheet) {
            QueueSheet(
                queue = queue,
                currentIndex = currentIndex,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTrackSelect = { index ->
                    playerManager.playQueue(queue, index)
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
