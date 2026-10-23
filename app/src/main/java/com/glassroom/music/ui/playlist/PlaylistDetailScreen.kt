package com.glassroom.music.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.PlaylistRepository
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistRepository: PlaylistRepository,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val playlistWithTracks by playlistRepository.getPlaylistWithTracks(playlistId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val glassConfig = LocalGlassConfig.current

    val playlist = playlistWithTracks?.playlist
    val tracks = playlistWithTracks?.tracks ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Navigation TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                contentDescription = "Back"
            )
            if (playlist != null) {
                GlassIconButton(
                    icon = Icons.Filled.Delete,
                    onClick = {
                        coroutineScope.launch {
                            playlistRepository.deletePlaylist(playlistId)
                            onBack()
                        }
                    },
                    contentDescription = "Delete Playlist",
                    tint = Color(0xFFEF4444)
                )
            }
        }

        if (playlist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Playlist not found",
                    style = GlassTypography.bodyLarge,
                    color = glassConfig.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp)
            ) {
                // Header Artwork & Details
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    ambientColor = if (glassConfig.isDark) Color(0x40000000) else Color(0x240F172A)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        if (glassConfig.isDark) listOf(Color(0xFF222B3D), Color(0xFF161F2E))
                                        else listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = glassConfig.borderBrush,
                                    shape = RoundedCornerShape(26.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (playlist.artwork.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(playlist.artwork)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = playlist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = if (glassConfig.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = playlist.name,
                            style = GlassTypography.headlineLarge,
                            color = glassConfig.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (playlist.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlist.description,
                                style = GlassTypography.bodyMedium,
                                color = glassConfig.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${tracks.size} songs",
                                style = GlassTypography.labelSmall,
                                color = glassConfig.textTertiary
                            )
                            if (playlist.isSpotifyImport) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCC10B981))
                                        .border(0.8.dp, Color(0x80FFFFFF), RoundedCornerShape(6.dp))
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Play All and Shuffle All Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GlassButton(
                                text = "Play All",
                                icon = Icons.Filled.PlayArrow,
                                isPrimary = true,
                                onClick = { onPlayAll(tracks) },
                                modifier = Modifier.weight(1f)
                            )
                            GlassButton(
                                text = "Shuffle",
                                icon = Icons.Filled.Shuffle,
                                isPrimary = false,
                                onClick = { onPlayAll(tracks.shuffled()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Track Items
                itemsIndexed(
                    items = tracks,
                    key = { index, track -> "detail_track_${track.id}_$index" }
                ) { _, track ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        GlassSongCard(
                            track = track,
                            isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                            onClick = { onPlayTrack(track, tracks) },
                            onFavoriteClick = {
                                coroutineScope.launch {
                                    playlistRepository.toggleFavorite(track)
                                }
                            },
                            onMoreClick = {
                                coroutineScope.launch {
                                    playlistRepository.removeTrackFromPlaylist(playlistId, track.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
