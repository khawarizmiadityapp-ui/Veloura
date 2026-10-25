package com.glassroom.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.ui.theme.*

@Composable
fun GlassPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val glassConfig = LocalGlassConfig.current

    // Tactile spring press feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "playlist_card_press"
    )

    Column(
        modifier = modifier
            .scale(cardScale)
            .width(size)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(2.dp)
    ) {
        // Square Artwork with frosted border and Spotify Import badge
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x1A0F172A),
                    spotColor = if (glassConfig.isDark) Color(0x26000000) else Color(0x100F172A)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        if (glassConfig.isDark) listOf(Color(0xFF222B3D), Color(0xFF161F2E))
                        else listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = glassConfig.borderBrush,
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.artwork.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.artwork)
                        .crossfade(true)
                        .memoryCacheKey(playlist.id.ifBlank { playlist.artwork })
                        .build(),
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                if (glassConfig.isDark) listOf(Color(0xFF26324A), Color(0xFF161F30))
                                else listOf(Color(0xFFE0E7FF), Color(0xFFF1F5F9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = if (glassConfig.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Spotify Import Badge
            if (playlist.isSpotifyImport) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC10B981))
                        .border(0.8.dp, Color(0x80FFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Spotify",
                        style = GlassTypography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = playlist.name,
            style = GlassTypography.titleMedium,
            color = glassConfig.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle / Track Count
        val subtitle = if (playlist.description.isNotBlank() && playlist.description != playlist.name) {
            playlist.description
        } else {
            "${playlist.trackCount} tracks"
        }
        Text(
            text = subtitle,
            style = GlassTypography.labelSmall,
            color = glassConfig.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
