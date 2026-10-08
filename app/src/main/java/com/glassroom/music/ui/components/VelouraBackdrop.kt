package com.glassroom.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

    val baseBackgroundBrush = if (glassConfig.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090B10),
                Color(0xFF0E131C),
                Color(0xFF060709)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF7F9FC),
                Color(0xFFEDF2F7),
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
                // Subtle cosmic ambient pools for dark glass
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3D1E3A8A), // Deep sapphire
                            Color(0x181E293B),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.15f),
                        radius = width * 0.75f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2E4338CA), // Deep indigo
                            Color(0x121E1B4B),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.12f, height * 0.55f),
                        radius = width * 0.65f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x280284C7), // Subtle cyan refraction
                            Color.Transparent
                        ),
                        center = Offset(width * 0.7f, height * 0.85f),
                        radius = width * 0.55f
                    )
                )
            } else {
                // Sophisticated optical crystal pools for light glass
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2938BDF8), // Soft sky crystal
                            Color(0x1060A5FA),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.88f, height * 0.12f),
                        radius = width * 0.75f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x22818CF8), // Ethereal lavender mist
                            Color(0x0C4F46E5),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.10f, height * 0.48f),
                        radius = width * 0.65f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x38FFFFFF), // Top specular reflection
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.05f),
                        radius = width * 0.8f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2034D399), // Subtle mint frequency warmth
                            Color.Transparent
                        ),
                        center = Offset(width * 0.75f, height * 0.82f),
                        radius = width * 0.55f
                    )
                )
            }
        }
    }
}
