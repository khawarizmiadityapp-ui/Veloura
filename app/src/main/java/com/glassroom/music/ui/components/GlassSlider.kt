package com.glassroom.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val sliderValue = if (isDragging) dragPosition else (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)

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
                thumbColor = Color(0xFF2C3E50),
                activeTrackColor = Color(0xFF2C3E50),
                inactiveTrackColor = Color(0x330F172A)
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .shadow(4.dp, CircleShape, ambientColor = Color(0x33000000))
                        .clip(CircleShape)
                        .background(Color(0xFF2C3E50))
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF2C3E50),
                        inactiveTrackColor = Color(0x260F172A)
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
                color = GlassTextTertiary,
                fontSize = 11.sp
            )
            Text(
                text = formatTime(totalSeconds),
                style = GlassTypography.labelSmall,
                color = GlassTextTertiary,
                fontSize = 11.sp
            )
        }
    }
}
