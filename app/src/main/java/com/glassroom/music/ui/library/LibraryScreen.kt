package com.glassroom.music.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.theme.*

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val glassConfig = LocalGlassConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        GlassTopBar(
            title = "Library",
            subtitle = "Your Saved Sounds"
        )

        // Filter Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.tabs) { tab ->
                GlassChip(
                    text = tab,
                    isSelected = uiState.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (uiState.selectedTab) {
            "Liked Songs" -> {
                if (uiState.favoriteTracks.isEmpty()) {
                    EmptyLibraryState(
                        title = "No favorite songs yet",
                        subtitle = "Tap the heart icon on any song to save it here."
                    )
                } else {
                    TrackList(
                        tracks = uiState.favoriteTracks,
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = onTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite
                    )
                }
            }

            "Recently Played" -> {
                if (uiState.recentlyPlayed.isEmpty()) {
                    EmptyLibraryState(
                        title = "No listening history",
                        subtitle = "Tracks you play will show up here."
                    )
                } else {
                    TrackList(
                        tracks = uiState.recentlyPlayed,
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = onTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite
                    )
                }
            }

            "Artists" -> {
                val artists = uiState.favoriteTracks.map { it.artist }.distinct()
                if (artists.isEmpty()) {
                    EmptyLibraryState(
                        title = "No artists yet",
                        subtitle = "Favorite songs from artists to organize them here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(artists) { artist ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {}
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    if (glassConfig.isDark) listOf(Color(0xFF242E40), Color(0xFF161F2E))
                                                    else listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0))
                                                )
                                            )
                                            .border(1.dp, glassConfig.borderBrush, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = glassConfig.textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = artist,
                                        style = GlassTypography.titleMedium,
                                        color = glassConfig.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "Playlists" -> {
                if (uiState.playlists.isEmpty()) {
                    EmptyLibraryState(
                        title = "No playlists found",
                        subtitle = "Create playlists or import from Spotify in the Playlists tab."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.playlists,
                            key = { index, playlist -> "lib_pl_${playlist.id}_$index" }
                        ) { _, playlist ->
                            GlassCard(
                                onClick = { onPlaylistClick(playlist) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = GlassTypography.titleMedium,
                                            color = glassConfig.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${playlist.trackCount} tracks",
                                            style = GlassTypography.labelSmall,
                                            color = glassConfig.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onFavoriteClick: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = tracks,
            key = { index, track -> "lib_track_${track.id}_$index" }
        ) { _, track ->
            GlassSongCard(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                onClick = { onTrackClick(track, tracks) },
                onFavoriteClick = { onFavoriteClick(track) }
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(title: String, subtitle: String) {
    val glassConfig = LocalGlassConfig.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(6.dp, CircleShape, ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x140F172A))
                    .clip(CircleShape)
                    .background(brush = glassConfig.cardBrush)
                    .border(1.dp, glassConfig.borderBrush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = glassConfig.textTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = GlassTypography.headlineMedium,
                color = glassConfig.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = GlassTypography.bodyMedium,
                color = glassConfig.textSecondary
            )
        }
    }
}
