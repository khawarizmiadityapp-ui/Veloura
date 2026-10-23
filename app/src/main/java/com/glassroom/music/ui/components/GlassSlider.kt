package com.glassroom.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
        val m = sec / 60
        val s = sec % 60
        return "%d:%02d".format(m, s)
    }

    val activeColor = if (glassConfig.isDark) Color(0xFF38BDF8) else Color(0xFF1E293B)
    val inactiveColor = if (glassConfig.isDark) Color(0x33FFFFFF) else Color(0x1F0F172A)

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
                        .size(14.dp)
                        .shadow(4.dp, CircleShape, ambientColor = if (glassConfig.isDark) Color(0x6638BDF8) else Color(0x33000000))
                        .clip(CircleShape)
                        .background(activeColor)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = activeColor,
                        inactiveTrackColor = inactiveColor
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
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
                color = glassConfig.textTertiary,
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
