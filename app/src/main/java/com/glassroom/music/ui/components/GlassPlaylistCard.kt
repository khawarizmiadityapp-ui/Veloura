package com.glassroom.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.ui.theme.*

@Composable
fun GlassPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // Square Artwork with frosted border and Spotify Import badge
        val glassConfig = LocalGlassConfig.current
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x1A0F172A)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(if (glassConfig.isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0))
                .border(width = 1.dp, brush = glassConfig.borderBrush, shape = RoundedCornerShape(20.dp))
        ) {
            if (playlist.artwork.isNotBlank()) {
                AsyncImage(
                    model = playlist.artwork,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE0E7FF), Color(0xFFF1F5F9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = GlassTextTertiary,
                        modifier = Modifier.size(40.dp)
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
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Spotify Import",
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
            color = GlassTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Track Count & Description
        Text(
            text = "${playlist.trackCount} tracks",
            style = GlassTypography.labelSmall,
            color = GlassTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
