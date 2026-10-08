package com.glassroom.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.glassroom.music.ui.theme.GlassBorderHighlight
import com.glassroom.music.ui.theme.GlassSurfaceCardGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        val glassConfig = com.glassroom.music.ui.theme.LocalGlassConfig.current
        val dialogBrush = if (glassConfig.isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xF018202F),
                    Color(0xFA0F1420)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xF8FFFFFF),
                    Color(0xEEF1F5F9)
                )
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = if (glassConfig.isDark) Color(0x66000000) else Color(0x330F172A),
                    spotColor = if (glassConfig.isDark) Color(0x4D000000) else Color(0x260F172A)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(brush = dialogBrush)
                .border(
                    width = 1.dp,
                    brush = glassConfig.borderBrush,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            content()
        }
    }
}
