package com.glassroom.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_card_press"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
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
