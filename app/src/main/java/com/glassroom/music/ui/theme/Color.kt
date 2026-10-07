package com.glassroom.music.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// White Transparent Glassroom Palette
val GlassWhite = Color(0xFFFFFFFF)
val GlassWhite95 = Color(0xF2FFFFFF) // 95% opacity
val GlassWhite85 = Color(0xD9FFFFFF) // 85% opacity
val GlassWhite70 = Color(0xB3FFFFFF) // 70% opacity
val GlassWhite55 = Color(0x8CFFFFFF) // 55% opacity
val GlassWhite40 = Color(0x66FFFFFF) // 40% opacity
val GlassWhite25 = Color(0x40FFFFFF) // 25% opacity
val GlassWhite15 = Color(0x26FFFFFF) // 15% opacity

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

val GlassDarkFallbackBg = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E222B),
        Color(0xFF13161C),
        Color(0xFF0C0E12)
    )
)
