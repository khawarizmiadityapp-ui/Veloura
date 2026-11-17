package com.glassroom.music.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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

// Editorial Warm Paper Palette (Kertas)
val PaperBgTop = Color(0xFFFAF6EE)
val PaperBgMid = Color(0xFFF3ECE0)
val PaperBgBottom = Color(0xFFE9DFCD)

val PaperTextPrimary = Color(0xFF1F1B16)
val PaperTextSecondary = Color(0xFF5C5246)
val PaperTextTertiary = Color(0xFF8C8072)

val PaperAccentTerracotta = Color(0xFFB45309)
val PaperAccentSage = Color(0xFF2E6F4E)
val PaperHeartActive = Color(0xFFC2410C)

val PaperBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        PaperBgTop,
        PaperBgMid,
        PaperBgBottom
    )
)

data class GlassConfig(
    val isDark: Boolean = false,
    val isPaper: Boolean = false,
    val intensity: Float = 0.75f // Glass: 0.25..0.95 opacity | Paper: 0.25..0.95 warmth & grain
) {
    val cardBrush: Brush
        get() {
            val factor = intensity.coerceIn(0.2f, 1.0f)
            if (isPaper) {
                val top = lerp(Color(0xFFFFFEF9), Color(0xFFF9F1E2), factor)
                val bottom = lerp(Color(0xFFFBF6EC), Color(0xFFEEE2CD), factor)
                return Brush.verticalGradient(listOf(top, bottom))
            }
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
        get() = when {
            isPaper -> Brush.verticalGradient(
                listOf(
                    Color(0xFFF5EFE3),
                    Color(0xFFD8CAB4),
                    Color(0xFFC7B69C)
                )
            )
            isDark -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.16f),
                    Color(0xFF818CF8).copy(alpha = 0.22f),
                    Color(0x33000000)
                )
            )
            else -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.52f),
                    Color(0xFF38BDF8).copy(alpha = 0.28f),
                    Color(0x260F172A)
                )
            )
        }

    val backgroundBrush: Brush
        get() = when {
            isPaper -> PaperBackgroundBrush
            isDark -> GlassDarkBackgroundBrush
            else -> GlassroomBackgroundBrush
        }

    val baseSolidColor: Color
        get() = when {
            isPaper -> PaperBgTop
            isDark -> Color(0xFF070A10)
            else -> Color(0xFFF6F7FB)
        }

    val textPrimary: Color
        get() = when {
            isPaper -> PaperTextPrimary
            isDark -> Color(0xFFF8FAFC)
            else -> GlassTextPrimary
        }

    val textSecondary: Color
        get() = when {
            isPaper -> PaperTextSecondary
            isDark -> Color(0xFF94A3B8)
            else -> GlassTextSecondary
        }

    val textTertiary: Color
        get() = when {
            isPaper -> PaperTextTertiary
            isDark -> Color(0xFF64748B)
            else -> GlassTextTertiary
        }

    val borderHighlight: Color
        get() = when {
            isPaper -> Color(0xFFD8CAB4)
            isDark -> Color(0x40FFFFFF)
            else -> GlassBorderHighlight
        }

    val borderSubtle: Color
        get() = when {
            isPaper -> Color(0xFFE6DBC7)
            isDark -> Color(0x24FFFFFF)
            else -> GlassBorderSubtle
        }

    val miniPlayerBrush: Brush
        get() {
            val factor = intensity.coerceIn(0.2f, 1.0f)
            if (isPaper) {
                val top = lerp(Color(0xFFFFFEFA), Color(0xFFFAF3E4), factor)
                val bottom = lerp(Color(0xFFF9F2E4), Color(0xFFF0E4CF), factor)
                return Brush.verticalGradient(listOf(top, bottom))
            }
            return if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF222C3E).copy(alpha = (0.86f + factor * 0.10f).coerceIn(0.86f, 0.96f)),
                        Color(0xFF101520).copy(alpha = (0.82f + factor * 0.12f).coerceIn(0.82f, 0.94f))
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = (0.90f + factor * 0.08f).coerceIn(0.90f, 0.98f)),
                        Color(0xFFF1F5F9).copy(alpha = (0.84f + factor * 0.12f).coerceIn(0.84f, 0.96f))
                    )
                )
            }
        }

    val dialogBrush: Brush
        get() = when {
            isPaper -> Brush.verticalGradient(listOf(Color(0xFFFFFEF9), Color(0xFFF3EBDC)))
            isDark -> Brush.verticalGradient(listOf(Color(0xF018202F), Color(0xFA0F1420)))
            else -> Brush.verticalGradient(listOf(Color(0xF8FFFFFF), Color(0xEEF1F5F9)))
        }

    val sheetContainerColor: Color
        get() = when {
            isPaper -> Color(0xFFFAF5EA)
            isDark -> Color(0xF2121824)
            else -> Color(0xF7FFFFFF)
        }

    val accentPrimary: Color
        get() = when {
            isPaper -> PaperAccentTerracotta
            isDark -> Color(0xFF38BDF8)
            else -> Color(0xFF0284C7)
        }

    val accentSecondary: Color
        get() = if (isPaper) PaperAccentSage else GlassAccentMint

    val heartActive: Color
        get() = if (isPaper) PaperHeartActive else GlassHeartActive

    val primaryButtonBrush: Brush
        get() = when {
            isPaper -> Brush.horizontalGradient(listOf(Color(0xFF2E251D), Color(0xFF1C1712)))
            isDark -> Brush.horizontalGradient(listOf(GlassAccentCobalt, Color(0xFF2563EB)))
            else -> Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
        }

    val chipUnselectedBrush: Brush
        get() = when {
            isPaper -> Brush.verticalGradient(listOf(Color(0xFFFBF7EE), Color(0xFFEFE6D6)))
            isDark -> Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x14FFFFFF)))
            else -> Brush.verticalGradient(listOf(Color(0xE0FFFFFF), Color(0x99FFFFFF)))
        }

    val activePlayingCardBrush: Brush
        get() = when {
            isPaper -> Brush.horizontalGradient(listOf(Color(0xFFFFFBF2), Color(0xFFF6E7CE)))
            isDark -> Brush.horizontalGradient(listOf(Color(0xFF263248), Color(0xFF1B2332)))
            else -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF)))
        }

    val activePlayingBorderBrush: Brush
        get() = when {
            isPaper -> Brush.verticalGradient(listOf(PaperAccentTerracotta, PaperAccentTerracotta.copy(alpha = 0.55f)))
            else -> Brush.verticalGradient(listOf(GlassAccentCobalt, GlassAccentCobalt.copy(alpha = 0.5f)))
        }

    val placeholderBrush: Brush
        get() = when {
            isPaper -> Brush.linearGradient(listOf(Color(0xFFEFE6D6), Color(0xFFE2D6C1)))
            isDark -> Brush.linearGradient(listOf(Color(0xFF222B3D), Color(0xFF171D29)))
            else -> Brush.linearGradient(listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0)))
        }

    val shadowAmbient: Color
        get() = when {
            isPaper -> Color(0x1E3B2B17)
            isDark -> Color(0x33000000)
            else -> Color(0x140F172A)
        }

    val shadowSpot: Color
        get() = when {
            isPaper -> Color(0x123B2B17)
            isDark -> Color(0x26000000)
            else -> Color(0x0B0F172A)
        }

    val inputSurfaceColor: Color
        get() = when {
            isPaper -> Color(0xFFF1E9DA)
            isDark -> Color(0x331E293B)
            else -> Color(0x66FFFFFF)
        }

    val inactiveTrackColor: Color
        get() = when {
            isPaper -> Color(0x283B2B17)
            isDark -> Color(0x33FFFFFF)
            else -> Color(0x240F172A)
        }

    val activePillBg: Color
        get() = when {
            isPaper -> Color(0x22B45309)
            isDark -> Color(0x2638BDF8)
            else -> Color(0x1F0284C7)
        }

    val activePillBorder: Color
        get() = when {
            isPaper -> Color(0x45B45309)
            isDark -> Color(0x4038BDF8)
            else -> Color(0x2E0284C7)
        }

    val toggleActiveBrush: Brush
        get() = when {
            isPaper -> Brush.verticalGradient(listOf(Color(0xFFE3EFE7), Color(0xFFD1E4D7)))
            isDark -> Brush.verticalGradient(listOf(Color(0x3310B981), Color(0x1F06B6D4)))
            else -> Brush.verticalGradient(listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0)))
        }
}

val LocalGlassConfig = compositionLocalOf { GlassConfig() }
