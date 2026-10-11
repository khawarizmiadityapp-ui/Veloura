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
                val topAlpha = (factor * 0.45f).coerceIn(0.20f, 0.55f)
                val bottomAlpha = (factor * 0.26f).coerceIn(0.10f, 0.36f)
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A364B).copy(alpha = topAlpha),
                        Color(0xFF121722).copy(alpha = bottomAlpha)
                    )
                )
            } else {
                val topAlpha = (factor * 0.78f).coerceIn(0.45f, 0.88f)
                val bottomAlpha = (factor * 0.46f).coerceIn(0.22f, 0.58f)
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF).copy(alpha = topAlpha),
                        Color(0xFFEDF2F7).copy(alpha = bottomAlpha)
                    )
                )
            }
        }

    val borderBrush: Brush
        get() = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.16f),
                    Color(0xFF818CF8).copy(alpha = 0.22f),
                    Color(0x33000000)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.52f),
                    Color(0xFF38BDF8).copy(alpha = 0.28f),
                    Color(0x260F172A)
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
        get() = if (isDark) Color(0x40FFFFFF) else GlassBorderHighlight

    val borderSubtle: Color
        get() = if (isDark) Color(0x24FFFFFF) else GlassBorderSubtle

    val miniPlayerBrush: Brush
        get() {
            val factor = intensity.coerceIn(0.2f, 1.0f)
            return if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF242E40).copy(alpha = (factor * 0.75f).coerceIn(0.48f, 0.85f)),
                        Color(0xFF101520).copy(alpha = (factor * 0.60f).coerceIn(0.36f, 0.72f))
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = (factor * 0.88f).coerceIn(0.60f, 0.94f)),
                        Color(0xFFF1F5F9).copy(alpha = (factor * 0.68f).coerceIn(0.42f, 0.80f))
                    )
                )
            }
        }
}

val LocalGlassConfig = compositionLocalOf { GlassConfig() }
