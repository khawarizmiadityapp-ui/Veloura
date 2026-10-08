package com.glassroom.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glassroom.music.ui.theme.*

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "button_press")

    val glassConfig = LocalGlassConfig.current

    val bgBrush = if (isPrimary) {
        if (glassConfig.isDark) {
            Brush.horizontalGradient(
                colors = listOf(
                    GlassAccentCobalt,
                    Color(0xFF2563EB)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A)
                )
            )
        }
    } else {
        glassConfig.cardBrush
    }

    val contentColor = if (isPrimary) Color.White else glassConfig.textPrimary
    val borderBrush = if (isPrimary) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    } else {
        glassConfig.borderBrush
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPrimary) 8.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x140F172A),
                spotColor = if (glassConfig.isDark) Color(0x26000000) else Color(0x0F0F172A)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(brush = bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = GlassTypography.titleMedium,
                color = contentColor
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    backgroundBrush: Brush? = null
) {
    val glassConfig = LocalGlassConfig.current
    val effectiveTint = tint ?: glassConfig.textPrimary
    val effectiveBg = backgroundBrush ?: glassConfig.cardBrush
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "icon_press")

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x120F172A)
            )
            .clip(CircleShape)
            .background(brush = effectiveBg)
            .border(width = 1.dp, brush = glassConfig.borderBrush, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
