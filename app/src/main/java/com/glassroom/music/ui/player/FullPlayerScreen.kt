package com.glassroom.music.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
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
import coil.compose.AsyncImage
import com.glassroom.music.domain.model.Track
import com.glassroom.music.playback.GlassRepeatMode
import com.glassroom.music.playback.PlayerManager
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.queue.QueueSheet
import com.glassroom.music.ui.theme.*

@Composable
fun FullPlayerScreen(
    playerManager: PlayerManager,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
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
    val playbackError by playerManager.playbackError.collectAsState()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    // Artwork floating and subtle pulsing animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "artwork_float")
    val artworkScale by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 1.0f else 0.98f,
        targetValue = if (isPlaying) 1.03f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    val glassConfig = LocalGlassConfig.current

    if (currentTrack == null) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VelouraBackdrop()
            Text(text = "No track selected", style = GlassTypography.bodyLarge, color = glassConfig.textSecondary)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Atmospheric glowing backdrop
        VelouraBackdrop()

        // Soft ambient glow background circle behind artwork
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(340.dp)
                .scale(artworkScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (glassConfig.isDark) Color(0x4D8B5CF6) else Color(0x33A5B4FC),
                            if (glassConfig.isDark) Color(0x2606B6D4) else Color(0x1A93C5FD),
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
            // Top Bar: Collapse arrow down, playing from indicator, Queue button
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            modifier = Modifier
                                .size(285.dp)
                                .scale(artworkScale)
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    ambientColor = if (glassConfig.isDark) Color(0x668B5CF6) else Color(0x2A38BDF8),
                                    spotColor = if (glassConfig.isDark) Color(0x4D06B6D4) else Color(0x1F818CF8)
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
                                    .background(if (glassConfig.isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0))
                            ) {
                                AsyncImage(
                                    model = currentTrack?.thumbnail,
                                    contentDescription = currentTrack?.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Title, Artist & Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "",
                        style = GlassTypography.headlineLarge,
                        color = glassConfig.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTrack?.artist ?: "",
                        style = GlassTypography.bodyLarge,
                        color = glassConfig.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

            // Seekbar
            GlassSlider(
                positionMs = currentPosition,
                durationMs = duration,
                onSeek = { playerManager.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls
            GlassPlayerControl(
                isPlaying = isPlaying,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
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
