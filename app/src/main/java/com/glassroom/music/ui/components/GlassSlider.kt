package com.glassroom.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSlider(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalGlassConfig.current
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val rawProgress = (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    // Smoothly interpolate progress when not actively dragging
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "slider_progress_anim"
    )

    val sliderValue = if (isDragging) dragPosition else animatedProgress

    val currentSeconds = if (isDragging) {
        ((dragPosition * safeDuration) / 1000).toInt()
    } else {
        (positionMs / 1000).toInt()
    }
    val totalSeconds = (durationMs / 1000).toInt()

    fun formatTime(sec: Int): String {
        if (sec <= 0) return "0:00"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    val activeColor = if (glassConfig.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val inactiveColor = if (glassConfig.isDark) Color(0x33FFFFFF) else Color(0x240F172A)

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumb_scale"
    )

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "track_height"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = {
                isDragging = true
                dragPosition = it
            },
            onValueChangeFinished = {
                isDragging = false
                val targetMs = (dragPosition * safeDuration).toLong()
                onSeek(targetMs)
            },
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = inactiveColor
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .scale(thumbScale)
                        .size(16.dp)
                        .shadow(
                            elevation = if (isDragging) 8.dp else 4.dp,
                            shape = CircleShape,
                            ambientColor = if (glassConfig.isDark) Color(0x8038BDF8) else Color(0x4D000000),
                            spotColor = if (glassConfig.isDark) Color(0x9938BDF8) else Color(0x400284C7)
                        )
                        .clip(CircleShape)
                        .background(activeColor)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(trackHeight),
                    colors = SliderDefaults.colors(
                        activeTrackColor = activeColor,
                        inactiveTrackColor = inactiveColor
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentSeconds),
                style = GlassTypography.labelSmall,
                color = if (isDragging) activeColor else glassConfig.textTertiary,
                fontSize = 11.sp
            )
            Text(
                text = formatTime(totalSeconds),
                style = GlassTypography.labelSmall,
                color = glassConfig.textTertiary,
                fontSize = 11.sp
            )
        }
    }
}
