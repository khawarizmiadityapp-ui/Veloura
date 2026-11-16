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
        val dialogBrush = glassConfig.dialogBrush

        Box(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (glassConfig.isPaper) 14.dp else 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = glassConfig.shadowAmbient,
                    spotColor = glassConfig.shadowSpot
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
