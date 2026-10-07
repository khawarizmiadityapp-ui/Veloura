package com.glassroom.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.theme.*

@Composable
fun GlassSongCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    val glassConfig = LocalGlassConfig.current

    val cardBg = if (isPlaying) {
        if (glassConfig.isDark) {
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF242C3A),
                    Color(0xFF1B212D)
                )
            )
        } else {
            Brush.horizontalGradient(
                listOf(
                    Color(0xF5FFFFFF),
                    Color(0xEBEEF2F7)
                )
            )
        }
    } else {
        glassConfig.cardBrush
    }

    val borderColor = if (isPlaying) GlassAccentCobalt else glassConfig.borderHighlight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPlaying) 6.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x100F172A)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(brush = cardBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = GlassTypography.titleMedium,
                    color = if (isPlaying) GlassAccentCobalt else glassConfig.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = GlassTypography.bodyMedium,
                    color = glassConfig.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            if (track.duration > 0) {
                Text(
                    text = track.durationFormatted,
                    style = GlassTypography.labelSmall,
                    color = GlassTextTertiary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Favorite Button
            if (onFavoriteClick != null) {
                val heartColor by animateColorAsState(
                    targetValue = if (track.isFavorite) GlassHeartActive else GlassTextTertiary,
                    label = "heart_color"
                )
                Icon(
                    imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = heartColor,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFavoriteClick)
                        .padding(6.dp)
                )
            }

            // More Options Button
            if (onMoreClick != null) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = GlassTextTertiary,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onMoreClick)
                        .padding(6.dp)
                )
            }
        }
    }
}
