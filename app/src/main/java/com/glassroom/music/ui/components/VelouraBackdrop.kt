package com.glassroom.music.ui.components

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
import com.glassroom.music.ui.theme.LocalGlassConfig

@Composable
fun VelouraBackdrop(
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalGlassConfig.current

    // Organic luminous breathing animations for authentic glass refraction depth
    val infiniteTransition = rememberInfiniteTransition(label = "glass_ambient")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.12f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_2"
    )
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse_3"
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
                // Orb 1: Glowing Electric Violet / Amethyst Aura (Top-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x738B5CF6), // Vibrant violet glow
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
                            Color(0x6606B6D4), // Electric cyan
                            Color(0x2E3B82F6), // Cobalt blur
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
                            Color(0x54EC4899), // Neon rose
                            Color(0x24F43F5E),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.76f, height * 0.84f),
                        radius = width * 0.68f * pulse3
                    )
                )

                // Orb 4: Subtle Emerald / Aquatic Specular Core (Top-Center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3810B981), // Ambient mint
                            Color.Transparent
                        ),
                        center = Offset(width * 0.45f, height * 0.32f),
                        radius = width * 0.50f
                    )
                )
            } else {
                // Light Mode: Ethereal Aurora Crystal Orbs
                // Orb 1: Luminous Sky Blue & Periwinkle (Top-Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x5238BDF8), // Radiant sky blue
                            Color(0x28818CF8), // Periwinkle mist
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
                            Color(0x45F472B6), // Coral rose
                            Color(0x1EFB7185),
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
                            Color(0x3D34D399), // Mint glow
                            Color(0x15059669),
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
                            Color(0x66FFFFFF),
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
