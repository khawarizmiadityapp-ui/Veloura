package com.glassroom.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.theme.ArtworkColors
import com.glassroom.music.ui.theme.LocalGlassConfig
import com.glassroom.music.ui.theme.rememberAnimatedArtworkColors

@Composable
fun VelouraBackdrop(
    modifier: Modifier = Modifier,
    currentTrack: Track? = null,
    isPlaying: Boolean = false,
    isFullPlayer: Boolean = false,
    artworkColors: ArtworkColors = rememberAnimatedArtworkColors(track = currentTrack)
) {
    val glassConfig = LocalGlassConfig.current

    // Organic luminous breathing animations for authentic glass refraction depth
    val infiniteTransition = rememberInfiniteTransition(label = "glass_ambient")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.88f else 0.94f,
        targetValue = if (isPlaying) 1.18f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 1.14f else 1.04f,
        targetValue = if (isPlaying) 0.88f else 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_2"
    )
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_3"
    )

    // Smooth animation when transitioning into the full player
    val playerBloomTransition by animateFloatAsState(
        targetValue = if (isFullPlayer) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "full_player_bloom"
    )

    val baseBackgroundBrush = if (glassConfig.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF07090E),
                Color(0xFF0B0F17),
                Color(0xFF05070A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF9FAFD),
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = baseBackgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val primary = artworkColors.primary
            val secondary = artworkColors.secondary
            val tertiary = artworkColors.tertiary

            if (glassConfig.isDark) {
                // Orb 1: Dynamic Track Accent Aurora (Top-Right)
                val orb1Alpha = (0.40f + 0.15f * playerBloomTransition).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = orb1Alpha),
                            secondary.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(width * (0.85f - 0.10f * playerBloomTransition), height * (0.16f + 0.12f * playerBloomTransition)),
                        radius = width * (0.82f + 0.25f * playerBloomTransition) * pulse1
                    )
                )

                // Orb 2: Ambient Secondary pool (Center-Left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.35f),
                            tertiary.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.12f, height * 0.48f),
                        radius = width * 0.74f * pulse2
                    )
                )

                // Orb 3: Luminous Tertiary Aurora (Bottom-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiary.copy(alpha = 0.30f),
                            primary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.76f, height * 0.84f),
                        radius = width * 0.65f * pulse3
                    )
                )

                // Orb 4: Central Artwork Backlight Bloom (Active during Full Player)
                if (playerBloomTransition > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.45f * playerBloomTransition),
                                secondary.copy(alpha = 0.22f * playerBloomTransition),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.42f),
                            radius = width * 0.88f * pulse1
                        )
                    )
                }

                // Orb 5: Subtle Specular Top Highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x22FFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.02f),
                        radius = width * 0.60f
                    )
                )
            } else {
                // Light Mode: White Transparent Glassroom Crystal Atmosphere
                // Orb 1: Dynamic Track Ambient Aura (Top-Right)
                val lightOrb1Alpha = (0.24f + 0.14f * playerBloomTransition).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = lightOrb1Alpha),
                            secondary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(width * (0.86f - 0.12f * playerBloomTransition), height * (0.14f + 0.14f * playerBloomTransition)),
                        radius = width * (0.80f + 0.20f * playerBloomTransition) * pulse1
                    )
                )

                // Orb 2: Warm Secondary Quartz (Center-Left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.22f),
                            tertiary.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.10f, height * 0.46f),
                        radius = width * 0.72f * pulse2
                    )
                )

                // Orb 3: Aquatic Mint / Soft Frequency (Bottom-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiary.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.78f, height * 0.80f),
                        radius = width * 0.60f * pulse3
                    )
                )

                // Orb 4: Central Artwork Backlight Bloom (Active during Full Player)
                if (playerBloomTransition > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.28f * playerBloomTransition),
                                secondary.copy(alpha = 0.12f * playerBloomTransition),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.42f),
                            radius = width * 0.82f * pulse1
                        )
                    )
                }

                // Top Specular Reflection Highlight (Optical crystal light catch)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x73FFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.03f),
                        radius = width * 0.80f
                    )
                )
            }
        }
    }
}
