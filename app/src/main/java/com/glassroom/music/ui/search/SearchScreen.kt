package com.glassroom.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.theme.*

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTrackClick: (Track, List<Track>) -> Unit,
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
        // Search Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GlassSearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Search songs, artists, albums..."
            )
        }

        // Category Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.categories) { cat ->
                GlassChip(
                    text = cat,
                    isSelected = uiState.selectedCategory == cat,
                    onClick = { viewModel.onCategorySelect(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Results or Empty/Loading State
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { GlassSongSkeleton() }
            }
        } else if (uiState.results.isEmpty() && uiState.query.isNotBlank()) {
            // No Results Found Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
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
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching songs",
                        style = GlassTypography.headlineMedium,
                        color = glassConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try searching for a different track, artist, or album.",
                        style = GlassTypography.bodyMedium,
                        color = glassConfig.textSecondary
                    )
                }
            }
        } else if (uiState.results.isEmpty()) {
            // Initial Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
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
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search anything",
                        style = GlassTypography.headlineMedium,
                        color = glassConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explore millions of songs, artists, and playlists.",
                        style = GlassTypography.bodyMedium,
                        color = glassConfig.textSecondary
                    )
                }
            }
        } else {
            // Results List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiState.results,
                    key = { index, track -> "search_${track.id}_$index" }
                ) { _, track ->
                    GlassSongCard(
                        track = track,
                        isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                        onClick = { onTrackClick(track, uiState.results) },
                        onFavoriteClick = { viewModel.toggleFavorite(track) }
                    )
                }
            }
        }
    }
}
