package com.glassroom.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import com.glassroom.music.ui.theme.GlassAccentCobalt
import com.glassroom.music.ui.theme.LocalGlassConfig

@Composable
fun VelouraBackdrop(
    modifier: Modifier = Modifier,
    currentTrack: Track? = null,
    isPlaying: Boolean = false
) {
    val glassConfig = LocalGlassConfig.current

    // Organic luminous breathing animations for authentic glass refraction depth
    val infiniteTransition = rememberInfiniteTransition(label = "glass_ambient")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.85f else 0.92f,
        targetValue = if (isPlaying) 1.20f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 1.15f else 1.05f,
        targetValue = if (isPlaying) 0.85f else 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_2"
    )
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_3"
    )

    // Dynamic track ambient hue shift that glides softly when current track changes
    val targetAccent = when {
        currentTrack == null -> Color(0xFF8B5CF6)
        else -> {
            val hash = kotlin.math.abs((currentTrack.title + currentTrack.artist).hashCode())
            when (hash % 5) {
                0 -> Color(0xFF8B5CF6) // Violet Amethyst
                1 -> Color(0xFF06B6D4) // Azure Cyan
                2 -> Color(0xFFEC4899) // Rose Sunset
                3 -> Color(0xFF10B981) // Emerald Mint
                else -> Color(0xFF3B82F6) // Cobalt Blue
            }
        }
    }

    val animatedAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "track_accent_ambient"
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
                Color(0xFFF8FAFC),
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

            if (glassConfig.isDark) {
                // Orb 1: Dynamic Track Accent Aurora (Top-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = 0.45f),
                            Color(0x3B6366F1), // Indigo depth
                            Color.Transparent
                        ),
                        center = Offset(width * 0.88f, height * 0.14f),
                        radius = width * 0.85f * pulse1
                    )
                )

                // Orb 2: Radiant Cyan / Azure Refraction Pool (Center-Left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x5506B6D4), // Electric cyan
                            Color(0x283B82F6), // Cobalt blur
                            Color.Transparent
                        ),
                        center = Offset(width * 0.10f, height * 0.52f),
                        radius = width * 0.78f * pulse2
                    )
                )

                // Orb 3: Luminous Sunset Magenta / Rose (Bottom-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x45EC4899), // Neon rose
                            Color(0x1EF43F5E),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.76f, height * 0.84f),
                        radius = width * 0.68f * pulse3
                    )
                )

                // Orb 4: Subtle Specular Core (Top-Center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2810B981), // Ambient mint
                            Color.Transparent
                        ),
                        center = Offset(width * 0.45f, height * 0.32f),
                        radius = width * 0.50f
                    )
                )
            } else {
                // Light Mode: Ethereal Aurora Crystal Orbs
                // Orb 1: Dynamic Track Ambient Aura (Top-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = 0.28f),
                            Color(0x20818CF8), // Periwinkle mist
                            Color.Transparent
                        ),
                        center = Offset(width * 0.88f, height * 0.12f),
                        radius = width * 0.82f * pulse1
                    )
                )

                // Orb 2: Warm Coral Blossom & Lavender Quartz (Center-Left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x35F472B6), // Coral rose
                            Color(0x15FB7185),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.08f, height * 0.48f),
                        radius = width * 0.74f * pulse2
                    )
                )

                // Orb 3: Aquatic Mint Frequency (Bottom-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2E34D399), // Mint glow
                            Color(0x12059669),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.78f, height * 0.82f),
                        radius = width * 0.62f * pulse3
                    )
                )

                // Top Specular Reflection Highlight (Optical crystal light catch)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x73FFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.04f),
                        radius = width * 0.85f
                    )
                )
            }
        }
    }
}
