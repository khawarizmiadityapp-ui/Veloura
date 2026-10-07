package com.glassroom.music.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
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
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(artists) { artist ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = artist,
                                        style = GlassTypography.titleMedium,
                                        color = GlassTextPrimary
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
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.playlists) { playlist ->
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
                                            color = GlassTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${playlist.trackCount} tracks",
                                            style = GlassTypography.labelSmall,
                                            color = GlassTextSecondary
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tracks) { track ->
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = GlassTextTertiary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = GlassTypography.headlineMedium,
                color = GlassTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = GlassTypography.bodyMedium,
                color = GlassTextSecondary
            )
        }
    }
}
