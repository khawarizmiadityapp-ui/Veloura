package com.glassroom.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glassroom.music.ui.theme.LocalGlassConfig

@Composable
fun rememberGlassShimmerBrush(): Brush {
    val glassConfig = LocalGlassConfig.current
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val colors = if (glassConfig.isDark) {
        listOf(
            Color(0x0FFFFFFF),
            Color(0x28FFFFFF),
            Color(0x0FFFFFFF)
        )
    } else {
        listOf(
            Color(0x33FFFFFF),
            Color(0x80FFFFFF),
            Color(0x33FFFFFF)
        )
    }

    return Brush.linearGradient(
        colors = colors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun GlassSkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    shapeRadius: Dp = 20.dp
) {
    val glassConfig = LocalGlassConfig.current
    val brush = rememberGlassShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(shapeRadius))
            .background(brush = brush)
            .border(
                width = 1.dp,
                brush = glassConfig.borderBrush,
                shape = RoundedCornerShape(shapeRadius)
            )
    )
}

@Composable
fun GlassSongSkeleton(
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalGlassConfig.current
    val brush = rememberGlassShimmerBrush()
    val bg = if (glassConfig.isDark) Color(0x1F1E293B) else Color(0x66FFFFFF)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
        }
    }
}
