package com.glassroom.music.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    val glassConfig = LocalGlassConfig.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        // Veloura Branded Header with Crystal Logo Badge
        item {
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
                                .size(48.dp)
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
                                .padding(6.dp),
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
                        contentDescription = "Refresh Feed"
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
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Recently Played Section (if any)
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently Played",
                    subtitle = "Jump back in where you left off"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.recentlyPlayed,
                        key = { it.id }
                    ) { track ->
                        GlassPlaylistCard(
                            playlist = Playlist(
                                id = track.id,
                                name = track.title,
                                description = track.artist,
                                artwork = track.thumbnail,
                                trackCount = 1
                            ),
                            onClick = { onTrackClick(track, uiState.recentlyPlayed) },
                            size = 140.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Made For You Section
        if (uiState.madeForYou.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Made For You",
                    subtitle = "Curated based on your taste"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.madeForYou,
                        key = { it.id }
                    ) { track ->
                        GlassPlaylistCard(
                            playlist = Playlist(
                                id = track.id,
                                name = track.title,
                                description = track.artist,
                                artwork = track.thumbnail,
                                trackCount = 1
                            ),
                            onClick = { onTrackClick(track, uiState.madeForYou) },
                            size = 140.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Popular Songs Section
        item {
            SectionHeader(
                title = "Popular Songs",
                subtitle = "Top played in the room"
            )
        }
        if (uiState.isLoading && uiState.popularSongs.isEmpty()) {
            items(4) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    GlassSongSkeleton()
                }
            }
        } else {
            items(
                items = uiState.popularSongs,
                key = { it.id }
            ) { track ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    GlassSongCard(
                        track = track,
                        isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                        onClick = { onTrackClick(track, uiState.popularSongs) },
                        onFavoriteClick = { viewModel.toggleFavorite(track) }
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Your Playlists Section
        if (uiState.userPlaylists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Your Playlists",
                    subtitle = "Personal playlists & mixes"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.userPlaylists,
                        key = { it.id }
                    ) { playlist ->
                        GlassPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            size = 140.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Imported Playlists (from Spotify)
        if (uiState.importedPlaylists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Imported Playlists",
                    subtitle = "Synced from your Spotify library"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.importedPlaylists,
                        key = { it.id }
                    ) { playlist ->
                        GlassPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            size = 140.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Recommended / Trending Section
        if (uiState.trending.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recommended For You",
                    subtitle = "Fresh sounds picked for this session"
                )
            }
            items(
                items = uiState.trending,
                key = { it.id }
            ) { track ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
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

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val glassConfig = LocalGlassConfig.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = GlassTypography.headlineMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = glassConfig.textPrimary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = GlassTypography.labelSmall,
                color = glassConfig.textTertiary,
                fontSize = 12.sp
            )
        }
    }
}
