package com.glassroom.music.ui.theme

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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

private val PaperColorScheme = lightColorScheme(
    primary = Color(0xFF2E251D),
    onPrimary = Color(0xFFFAF6EE),
    primaryContainer = Color(0xFFF6ECDA),
    onPrimaryContainer = PaperTextPrimary,
    secondary = PaperAccentTerracotta,
    onSecondary = Color.White,
    background = PaperBgTop,
    onBackground = PaperTextPrimary,
    surface = Color(0xFFFFFDF8),
    onSurface = PaperTextPrimary,
    surfaceVariant = Color(0xFFEEE5D3),
    onSurfaceVariant = PaperTextSecondary,
    outline = Color(0xFFD8CAB4)
)

@Composable
fun GlassroomTheme(
    darkTheme: Boolean = false,
    paperTheme: Boolean = false,
    glassIntensity: Float = 0.75f,
    content: @Composable () -> Unit
) {
    val effectiveDark = darkTheme && !paperTheme
    val colorScheme = when {
        paperTheme -> PaperColorScheme
        effectiveDark -> DarkColorScheme
        else -> LightColorScheme
    }
    val animatedIntensity by animateFloatAsState(
        targetValue = glassIntensity.coerceIn(0.2f, 1.0f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "veloura_glass_intensity"
    )
    val glassConfig = GlassConfig(
        isDark = effectiveDark,
        isPaper = paperTheme,
        intensity = animatedIntensity
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !effectiveDark
                controller.isAppearanceLightNavigationBars = !effectiveDark
            }
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

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
