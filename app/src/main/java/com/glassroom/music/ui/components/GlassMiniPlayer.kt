package com.glassroom.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.theme.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassMiniPlayer(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Smooth progress interpolation to avoid discrete steps
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "mini_progress_anim"
    )

    // Debouncing rapid taps
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val debounceMs = 280L

    val safeOnClick = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > debounceMs) {
            lastTapTime = now
            onClick()
        }
    }

    val safeOnSwipeUp = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > debounceMs) {
            lastTapTime = now
            onSwipeUp()
        }
    }

    val safeOnPlayPause = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > debounceMs) {
            lastTapTime = now
            onPlayPause()
        }
    }

    val safeOnNext = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > debounceMs) {
            lastTapTime = now
            onNext()
        }
    }

    val draggableState = rememberDraggableState { delta ->
        if (delta < -12) { // Swiped up with gentle threshold
            safeOnSwipeUp()
        }
    }

    val glassConfig = LocalGlassConfig.current

    // Spring button press response
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playBtnScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mini_play_scale"
    )

    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()
    val nextBtnScale by animateFloatAsState(
        targetValue = if (isNextPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mini_next_scale"
    )

    // Artwork Shared Element modifier with fluid coordinated spring
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = if (glassConfig.isDark) Color(0x40000000) else Color(0x1F0F172A),
                spotColor = if (glassConfig.isDark) Color(0x2D000000) else Color(0x140F172A)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(brush = glassConfig.miniPlayerBrush)
            .border(
                width = 1.dp,
                brush = glassConfig.borderBrush,
                shape = RoundedCornerShape(26.dp)
            )
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = safeOnClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                // Artwork Container with Shared Element and Fallback Placeholder
                Box(
                    modifier = artworkSharedModifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                if (glassConfig.isDark) listOf(Color(0xFF26324A), Color(0xFF161F30))
                                else listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0))
                            )
                        )
                        .border(
                            width = 0.8.dp,
                            brush = if (glassConfig.isDark) Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x10FFFFFF))
                            ) else Brush.verticalGradient(
                                listOf(Color(0x80FFFFFF), Color(0x20FFFFFF))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = track.thumbnail,
                        transitionSpec = {
                            fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                        },
                        label = "mini_artwork_crossfade"
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
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = if (glassConfig.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Song Title & Artist with smooth slide/crossfade on track change
                AnimatedContent(
                    targetState = track.title to track.artist,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(260)) { height -> height / 2 } + fadeIn(tween(260)))
                            .togetherWith(slideOutVertically(animationSpec = tween(200)) { height -> -height / 2 } + fadeOut(tween(200)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "mini_player_text_anim"
                ) { (title, artist) ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = title,
                            style = GlassTypography.titleMedium,
                            color = glassConfig.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artist,
                            style = GlassTypography.bodyMedium,
                            color = glassConfig.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause Glass Orb Button with Loading Spinner support
                Box(
                    modifier = Modifier
                        .scale(playBtnScale)
                        .size(42.dp)
                        .shadow(6.dp, CircleShape, spotColor = Color(0x3338BDF8))
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                if (glassConfig.isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                else listOf(Color(0xFF1E293B), Color(0xFF334155))
                            )
                        )
                        .border(1.dp, Color(0x66FFFFFF), CircleShape)
                        .clickable(
                            interactionSource = playInteractionSource,
                            indication = null,
                            onClick = safeOnPlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = if (isLoading) "loading" else if (isPlaying) "pause" else "play",
                        transitionSpec = {
                            scaleIn(tween(200)) + fadeIn() togetherWith scaleOut(tween(150)) + fadeOut()
                        },
                        label = "mini_play_anim"
                    ) { state ->
                        when (state) {
                            "loading" -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            "pause" -> {
                                Icon(
                                    imageVector = Icons.Filled.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next Track Glass Button with tactile spring feedback
                Box(
                    modifier = Modifier
                        .scale(nextBtnScale)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (glassConfig.isDark) Color(0x1AFFFFFF) else Color(0x140F172A))
                        .clickable(
                            interactionSource = nextInteractionSource,
                            indication = null,
                            onClick = safeOnNext
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = glassConfig.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Slim luminous progress indicator on the bottom edge with rounded bottom corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(if (glassConfig.isDark) Color(0x1AFFFFFF) else Color(0x140F172A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(GlassAccentCobalt, GlassAccentMint)
                            )
                        )
                )
            }
        }
    }
}
