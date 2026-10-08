package com.glassroom.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glassroom.music.ui.theme.LocalGlassConfig

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundBrush: Brush? = null,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    val effectiveBrush = backgroundBrush ?: glassConfig.cardBrush
    val effectiveBorderBrush = borderBrush ?: if (borderColor != null) SolidColor(borderColor) else glassConfig.borderBrush

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x120F172A),
                spotColor = if (glassConfig.isDark) Color(0x26000000) else Color(0x0A0F172A)
            )
            .clip(shape)
            .background(brush = effectiveBrush, shape = shape)
            .border(
                width = borderWidth,
                brush = effectiveBorderBrush,
                shape = shape
            )
            .then(clickModifier)
    ) {
        content()
    }
}
