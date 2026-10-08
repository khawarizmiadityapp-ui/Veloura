package com.glassroom.music.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// White Transparent Glassroom Palette
val GlassWhite = Color(0xFFFFFFFF)
val GlassWhite95 = Color(0xF2FFFFFF)
val GlassWhite85 = Color(0xD9FFFFFF)
val GlassWhite70 = Color(0xB3FFFFFF)
val GlassWhite55 = Color(0x8CFFFFFF)
val GlassWhite40 = Color(0x66FFFFFF)
val GlassWhite25 = Color(0x40FFFFFF)
val GlassWhite15 = Color(0x26FFFFFF)

// Subtle borders
val GlassBorderWhite = Color(0x99FFFFFF)
val GlassBorderSubtle = Color(0x4DFFFFFF)
val GlassBorderHighlight = Color(0xCCFFFFFF)

// Text tokens
val GlassTextPrimary = Color(0xFF191C21)
val GlassTextSecondary = Color(0xFF5E6573)
val GlassTextTertiary = Color(0xFF8F98A8)
val GlassTextLight = Color(0xFFB0B7C3)

// Accents (frost, icy blue, lavender, glow)
val GlassAccentPrimary = Color(0xFF2B3A4A)
val GlassAccentCobalt = Color(0xFF3B82F6)
val GlassAccentMint = Color(0xFF10B981)
val GlassAccentGlow = Color(0x263B82F6)
val GlassHeartActive = Color(0xFFEF4444)

// Background Gradient Tokens
val GlassroomBgTop = Color(0xFFF9FAFD)
val GlassroomBgMid = Color(0xFFEFF3F8)
val GlassroomBgBottom = Color(0xFFE8EEF5)

val GlassroomBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        GlassroomBgTop,
        GlassroomBgMid,
        GlassroomBgBottom
    )
)

val GlassSurfaceGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xCCFFFFFF),
        Color(0x80FFFFFF)
    )
)

val GlassSurfaceCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xE6FFFFFF),
        Color(0x99FFFFFF)
    )
)

val GlassActiveCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xF5FFFFFF),
        Color(0xBFFFFFFF)
    )
)

// Dark Glass Palettes
val GlassDarkBgTop = Color(0xFF151922)
val GlassDarkBgMid = Color(0xFF0F1218)
val GlassDarkBgBottom = Color(0xFF090B0F)

val GlassDarkBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        GlassDarkBgTop,
        GlassDarkBgMid,
        GlassDarkBgBottom
    )
)

data class GlassConfig(
    val isDark: Boolean = false,
    val intensity: Float = 0.75f // 0.25 (crystal) to 0.95 (milky)
) {
    val cardBrush: Brush
        get() {
            val factor = intensity.coerceIn(0.2f, 1.0f)
            return if (isDark) {
                val topAlpha = (factor * 0.52f).coerceIn(0.20f, 0.62f)
                val bottomAlpha = (factor * 0.32f).coerceIn(0.10f, 0.42f)
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E2638).copy(alpha = topAlpha),
                        Color(0xFF0D121C).copy(alpha = bottomAlpha)
                    )
                )
            } else {
                val topAlpha = (factor * 0.70f).coerceIn(0.30f, 0.78f)
                val bottomAlpha = (factor * 0.42f).coerceIn(0.18f, 0.50f)
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF).copy(alpha = topAlpha),
                        Color(0xFFF1F5F9).copy(alpha = bottomAlpha)
                    )
                )
            }
        }

    val borderBrush: Brush
        get() = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.10f),
                    Color(0xFF000000).copy(alpha = 0.25f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.88f),
                    Color.White.copy(alpha = 0.40f),
                    Color(0xFFCBD5E1).copy(alpha = 0.25f)
                )
            )
        }

    val backgroundBrush: Brush
        get() = if (isDark) GlassDarkBackgroundBrush else GlassroomBackgroundBrush

    val textPrimary: Color
        get() = if (isDark) Color(0xFFF8FAFC) else GlassTextPrimary

    val textSecondary: Color
        get() = if (isDark) Color(0xFF94A3B8) else GlassTextSecondary

    val textTertiary: Color
        get() = if (isDark) Color(0xFF64748B) else GlassTextTertiary

    val borderHighlight: Color
        get() = if (isDark) Color(0x33FFFFFF) else GlassBorderHighlight

    val borderSubtle: Color
        get() = if (isDark) Color(0x1FFFFFFF) else GlassBorderSubtle

    val miniPlayerBrush: Brush
        get() {
            val factor = intensity.coerceIn(0.2f, 1.0f)
            return if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1B2230).copy(alpha = (factor * 0.82f).coerceIn(0.45f, 0.88f)),
                        Color(0xFF0E131C).copy(alpha = (factor * 0.68f).coerceIn(0.35f, 0.78f))
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = (factor * 0.82f).coerceIn(0.45f, 0.88f)),
                        Color(0xFFF8FAFC).copy(alpha = (factor * 0.62f).coerceIn(0.32f, 0.72f))
                    )
                )
            }
        }
}

val LocalGlassConfig = compositionLocalOf { GlassConfig() }
