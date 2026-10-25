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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
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

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

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
            Text(text = "No track selected", style = GlassTypography.bodyLarge, color = glassConfig.textSecondary)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (glassConfig.isDark) Color(0xB3070A10) else Color(0x73FFFFFF))
    ) {
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
                            artworkColors.primary.copy(alpha = if (glassConfig.isDark) 0.50f else 0.32f),
                            artworkColors.secondary.copy(alpha = if (glassConfig.isDark) 0.26f else 0.16f),
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
                            .background(if (glassConfig.isDark) Color(0x33FFFFFF) else Color(0x2A0F172A))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PLAYING FROM",
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
                            tint = if (showLyrics) GlassAccentMint else glassConfig.textTertiary,
                            backgroundBrush = if (showLyrics) {
                                Brush.verticalGradient(
                                    if (glassConfig.isDark) listOf(Color(0x6606B6D4), Color(0x330891B2))
                                    else listOf(Color(0xFFCCFBF1), Color(0xFF99F6E4))
                                )
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
                                .size(285.dp)
                                .scale(artworkScale)
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    ambientColor = artworkColors.primary.copy(alpha = if (glassConfig.isDark) 0.55f else 0.25f),
                                    spotColor = artworkColors.secondary.copy(alpha = if (glassConfig.isDark) 0.40f else 0.18f)
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
                                    .background(if (glassConfig.isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)),
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
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = if (glassConfig.isDark) listOf(Color(0xFF26324A), Color(0xFF161F30))
                                                        else listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MusicNote,
                                                contentDescription = null,
                                                tint = if (glassConfig.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
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

            Spacer(modifier = Modifier.height(20.dp))

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

                Box(modifier = Modifier.scale(favScale)) {
                    GlassIconButton(
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        onClick = onToggleFavorite,
                        tint = if (isFavorite) GlassHeartActive else glassConfig.textTertiary,
                        backgroundBrush = if (isFavorite) {
                            Brush.verticalGradient(
                                if (glassConfig.isDark) listOf(Color(0x40EF4444), Color(0x20F43F5E))
                                else listOf(Color(0xFFFFE4E6), Color(0xFFFECDD3))
                            )
                        } else glassConfig.cardBrush,
                        size = 48.dp,
                        iconSize = 24.dp,
                        contentDescription = "Favorite"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seekbar with smoothly animated thumb and track
            GlassSlider(
                positionMs = currentPosition,
                durationMs = duration,
                onSeek = { playerManager.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
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
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}
