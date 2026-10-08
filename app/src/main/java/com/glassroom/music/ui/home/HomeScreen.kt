package com.glassroom.music.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.R
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onNavigateToSearch: () -> Unit,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Veloura Branded Header with Crystal Logo Badge
        item {
            val glassConfig = LocalGlassConfig.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Glass Encapsulated Logo Badge
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x1A0F172A)
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(brush = glassConfig.cardBrush)
                                .border(
                                    width = 1.dp,
                                    brush = glassConfig.borderBrush,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_veloura_symbol),
                                contentDescription = "Veloura Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column {
                            Text(
                                text = "VELOURA",
                                style = GlassTypography.titleLarge.copy(
                                    letterSpacing = 2.4.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = glassConfig.textPrimary
                            )
                            Text(
                                text = uiState.greeting,
                                style = GlassTypography.labelSmall,
                                color = glassConfig.textSecondary
                            )
                        }
                    }

                    GlassIconButton(
                        icon = Icons.Outlined.GraphicEq,
                        onClick = { viewModel.loadRecommendations() },
                        contentDescription = "Refresh"
                    )
                }
            }
        }

        // Prominent Search Bar (Instant Tap to Search Screen)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                GlassSearchBar(
                    placeholder = "Search songs, artists, albums...",
                    readOnly = true,
                    onClick = onNavigateToSearch
                )
            }
        }

        // Recently Played Section (if any)
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Played")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.recentlyPlayed) { track ->
                        GlassPlaylistCard(
                            playlist = Playlist(
                                id = track.id,
                                name = track.title,
                                description = track.artist,
                                artwork = track.thumbnail,
                                trackCount = 1
                            ),
                            onClick = { onTrackClick(track, uiState.recentlyPlayed) },
                            size = 130.dp
                        )
                    }
                }
            }
        }

        // Made For You Section
        if (uiState.madeForYou.isNotEmpty()) {
            item {
                SectionHeader(title = "Made For You")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.madeForYou) { track ->
                        GlassPlaylistCard(
                            playlist = Playlist(
                                id = track.id,
                                name = track.title,
                                description = track.artist,
                                artwork = track.thumbnail,
                                trackCount = 1
                            ),
                            onClick = { onTrackClick(track, uiState.madeForYou) },
                            size = 145.dp
                        )
                    }
                }
            }
        }

        // Popular Songs Section
        item {
            SectionHeader(title = "Popular Songs")
            if (uiState.isLoading && uiState.popularSongs.isEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(4) { GlassSongSkeleton() }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.popularSongs.forEach { track ->
                        GlassSongCard(
                            track = track,
                            isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                            onClick = { onTrackClick(track, uiState.popularSongs) },
                            onFavoriteClick = { viewModel.toggleFavorite(track) }
                        )
                    }
                }
            }
        }

        // Your Playlists Section
        if (uiState.userPlaylists.isNotEmpty()) {
            item {
                SectionHeader(title = "Your Playlists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.userPlaylists) { playlist ->
                        GlassPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }
        }

        // Imported Playlists (from Spotify)
        if (uiState.importedPlaylists.isNotEmpty()) {
            item {
                SectionHeader(title = "Imported Playlists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.importedPlaylists) { playlist ->
                        GlassPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }
        }

        // Recommended / Trending Section
        if (uiState.trending.isNotEmpty()) {
            item {
                SectionHeader(title = "Recommended")
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.trending.forEach { track ->
                        GlassSongCard(
                            track = track,
                            isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                            onClick = { onTrackClick(track, uiState.trending) },
                            onFavoriteClick = { viewModel.toggleFavorite(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = GlassTypography.headlineMedium,
        color = GlassTextPrimary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    )
}
