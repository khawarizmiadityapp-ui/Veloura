package com.glassroom.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glassroom.music.playback.GlassRepeatMode
import com.glassroom.music.ui.theme.*

import androidx.compose.material3.CircularProgressIndicator

@Composable
fun GlassPlayerControl(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: GlassRepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val glassConfig = LocalGlassConfig.current

    // Rapid click debouncing for responsive yet stable playback controls
    var lastActionTime by remember { mutableLongStateOf(0L) }
    val debounceMs = 280L

    val safeOnPlayPause = {
        val now = System.currentTimeMillis()
        if (now - lastActionTime > debounceMs) {
            lastActionTime = now
            onPlayPause()
        }
    }

    val safeOnPrevious = {
        val now = System.currentTimeMillis()
        if (now - lastActionTime > debounceMs) {
            lastActionTime = now
            onPrevious()
        }
    }

    val safeOnNext = {
        val now = System.currentTimeMillis()
        if (now - lastActionTime > debounceMs) {
            lastActionTime = now
            onNext()
        }
    }

    // Spring press animation for play button
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "play_scale"
    )

    // Breathing pulse glow aura around play button when music is active
    val infiniteTransition = rememberInfiniteTransition(label = "play_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.22f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.65f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Shuffle & Repeat Active State
    val isRepeatActive = repeatMode != GlassRepeatMode.OFF
    val repeatIcon = when (repeatMode) {
        GlassRepeatMode.ONE -> Icons.Filled.RepeatOne
        else -> Icons.Filled.Repeat
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button with Active Glow & Indicator Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassIconButton(
                icon = Icons.Filled.Shuffle,
                onClick = onToggleShuffle,
                size = 46.dp,
                iconSize = 22.dp,
                tint = if (isShuffle) GlassAccentMint else glassConfig.textTertiary,
                backgroundBrush = if (isShuffle) {
                    Brush.verticalGradient(
                        if (glassConfig.isDark) listOf(Color(0x3310B981), Color(0x1F06B6D4))
                        else listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0))
                    )
                } else glassConfig.cardBrush
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isShuffle) GlassAccentMint else Color.Transparent)
            )
        }

        // Previous Button
        GlassIconButton(
            icon = Icons.Filled.SkipPrevious,
            onClick = safeOnPrevious,
            size = 52.dp,
            iconSize = 28.dp,
            tint = glassConfig.textPrimary,
            contentDescription = "Previous Track"
        )

        // Center Play / Pause Button with Floating Luminous Aura & Iridescent Glass Orb
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient breathing pulse glow
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (glassConfig.isDark) Color(0x6606B6D4) else Color(0x4D38BDF8),
                                    if (glassConfig.isDark) Color(0x338B5CF6) else Color(0x26818CF8),
                                    Color.Transparent
                                )
                            ),
                            alpha = pulseAlpha
                        )
                )
            }

            // Main Play Glass Orb
            Box(
                modifier = Modifier
                    .scale(playScale)
                    .size(74.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = if (glassConfig.isDark) Color(0x6606B6D4) else Color(0x330F172A),
                        spotColor = if (glassConfig.isDark) Color(0x4D8B5CF6) else Color(0x2638BDF8)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (glassConfig.isDark) listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            ) else listOf(
                                Color(0xFF1E293B),
                                Color(0xFF334155)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x80FFFFFF),
                                Color(0x33FFFFFF),
                                Color(0x1AFFFFFF)
                            )
                        ),
                        shape = CircleShape
                    )
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
                        scaleIn(tween(200)) togetherWith scaleOut(tween(150))
                    },
                    label = "play_pause_switch"
                ) { state ->
                    when (state) {
                        "loading" -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                        "pause" -> {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Next Button
        GlassIconButton(
            icon = Icons.Filled.SkipNext,
            onClick = safeOnNext,
            size = 52.dp,
            iconSize = 28.dp,
            tint = glassConfig.textPrimary,
            contentDescription = "Next Track"
        )

        // Repeat Button with Active Glow & Indicator Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassIconButton(
                icon = repeatIcon,
                onClick = onToggleRepeat,
                size = 46.dp,
                iconSize = 22.dp,
                tint = if (isRepeatActive) GlassAccentMint else glassConfig.textTertiary,
                backgroundBrush = if (isRepeatActive) {
                    Brush.verticalGradient(
                        if (glassConfig.isDark) listOf(Color(0x3310B981), Color(0x1F06B6D4))
                        else listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0))
                    )
                } else glassConfig.cardBrush
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isRepeatActive) GlassAccentMint else Color.Transparent)
            )
        }
    }
}
