package com.glassroom.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.ui.theme.*

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val glassConfig = LocalGlassConfig.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = GlassTypography.labelSmall,
                        color = glassConfig.textTertiary
                    )
                }
                Text(
                    text = title,
                    style = GlassTypography.headlineLarge,
                    color = glassConfig.textPrimary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search songs, artists, albums...",
    onSearch: () -> Unit = {}
) {
    val glassConfig = LocalGlassConfig.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x120F172A),
                spotColor = if (glassConfig.isDark) Color(0x26000000) else Color(0x0A0F172A)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(brush = glassConfig.miniPlayerBrush)
            .border(
                width = 1.dp,
                color = glassConfig.borderHighlight,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = glassConfig.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = GlassTypography.bodyLarge,
                        color = glassConfig.textTertiary
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                    cursorBrush = SolidColor(glassConfig.textPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = glassConfig.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2C3E50),
                Color(0xFF34495E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xE6FFFFFF),
                Color(0xB3FFFFFF)
            )
        )
    }
    val textColor = if (isSelected) Color.White else GlassTextSecondary
    val borderColor = if (isSelected) Color(0x33FFFFFF) else GlassBorderWhite

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 4.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x100F172A)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(brush = bgBrush)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = GlassTypography.labelLarge,
            color = textColor,
            fontSize = 13.sp
        )
    }
}
