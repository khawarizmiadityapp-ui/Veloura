package com.glassroom.music.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun GlassroomTheme(
    darkTheme: Boolean = false, // White Glassroom is default
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GlassTypography,
        content = content
    )
}
