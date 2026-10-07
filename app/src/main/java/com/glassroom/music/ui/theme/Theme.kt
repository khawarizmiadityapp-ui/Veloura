package com.glassroom.music.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GlassAccentPrimary,
    onPrimary = Color.White,
    primaryContainer = GlassWhite85,
    onPrimaryContainer = GlassTextPrimary,
    secondary = GlassAccentCobalt,
    onSecondary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = GlassTextPrimary,
    surface = Color(0xF2FFFFFF),
    onSurface = GlassTextPrimary,
    surfaceVariant = Color(0x99FFFFFF),
    onSurfaceVariant = GlassTextSecondary,
    outline = GlassBorderWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF8FAFC),
    secondary = GlassAccentCobalt,
    onSecondary = Color.White,
    background = Color(0xFF0B0D11),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131720),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0x33FFFFFF),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x33FFFFFF)
)

@Composable
fun GlassroomTheme(
    darkTheme: Boolean = false,
    glassIntensity: Float = 0.75f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val glassConfig = GlassConfig(isDark = darkTheme, intensity = glassIntensity)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalGlassConfig provides glassConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GlassTypography,
            content = content
        )
    }
}
