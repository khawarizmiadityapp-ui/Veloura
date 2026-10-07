package com.glassroom.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
    modifier: Modifier = Modifier
) {
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(targetValue = if (isPlayPressed) 0.92f else 1f, label = "play_scale")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        GlassIconButton(
            icon = Icons.Filled.Shuffle,
            onClick = onToggleShuffle,
            size = 42.dp,
            iconSize = 20.dp,
            tint = if (isShuffle) Color(0xFF2C3E50) else GlassTextTertiary,
            backgroundBrush = if (isShuffle) {
                Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
            } else GlassSurfaceCardGradient
        )

        // Previous Button
        GlassIconButton(
            icon = Icons.Filled.SkipPrevious,
            onClick = onPrevious,
            size = 48.dp,
            iconSize = 26.dp,
            tint = GlassTextPrimary
        )

        // Center Play / Pause Button (Extra large glass button with depth)
        Box(
            modifier = Modifier
                .scale(playScale)
                .size(72.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x330F172A),
                    spotColor = Color(0x260F172A)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C3E50),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color(0x40FFFFFF),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = playInteractionSource,
                    indication = null,
                    onClick = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = isPlaying, label = "play_pause_anim") { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Next Button
        GlassIconButton(
            icon = Icons.Filled.SkipNext,
            onClick = onNext,
            size = 48.dp,
            iconSize = 26.dp,
            tint = GlassTextPrimary
        )

        // Repeat Button
        val repeatIcon = when (repeatMode) {
            GlassRepeatMode.ONE -> Icons.Filled.RepeatOne
            else -> Icons.Filled.Repeat
        }
        val isRepeatActive = repeatMode != GlassRepeatMode.OFF

        GlassIconButton(
            icon = repeatIcon,
            onClick = onToggleRepeat,
            size = 42.dp,
            iconSize = 20.dp,
            tint = if (isRepeatActive) Color(0xFF2C3E50) else GlassTextTertiary,
            backgroundBrush = if (isRepeatActive) {
                Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
            } else GlassSurfaceCardGradient
        )
    }
}
