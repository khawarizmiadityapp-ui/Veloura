package com.glassroom.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.theme.*

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTrackClick: (Track, List<Track>) -> Unit,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    onTrackMoreClick: (Track) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val glassConfig = LocalGlassConfig.current
    val focusManager = LocalFocusManager.current
    val handleTrackClick: (Track, List<Track>) -> Unit = { track, queue ->
        viewModel.recordCurrentSearchQuery()
        focusManager.clearFocus()
        onTrackClick(track, queue)
    }

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
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.onSearchSubmit()
                },
                placeholder = "Cari lagu, artis, album, atau playlist..."
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

        val hasAnyResults = uiState.results.isNotEmpty() ||
            (uiState.selectedCategory == "Playlist" && uiState.matchedPlaylists.isNotEmpty())

        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { GlassSongSkeleton() }
                }
            }

            !hasAnyResults && uiState.errorMessage != null && uiState.query.isNotBlank() -> {
                SearchErrorState(
                    message = uiState.errorMessage ?: "Gagal memuat hasil pencarian",
                    onRetry = viewModel::retrySearch
                )
            }

            !hasAnyResults && uiState.query.isNotBlank() -> {
                SearchEmptyState(
                    title = "Tidak ada hasil yang cocok",
                    subtitle = "Coba kata kunci lagu, artis, atau playlist lain."
                )
            }

            !hasAnyResults -> {
                SearchIdleDiscoveryContent(
                    searchHistory = uiState.searchHistory,
                    onSelectTerm = { term ->
                        focusManager.clearFocus()
                        viewModel.selectHistoryOrSuggestion(term)
                    },
                    onRemoveHistory = viewModel::removeHistoryItem,
                    onClearHistory = viewModel::clearHistory
                )
            }

            uiState.selectedCategory == "Artis" -> {
                val groupedArtists = remember(uiState.results) {
                    uiState.results
                        .filter { it.artist.isNotBlank() }
                        .groupBy { it.artist }
                        .entries
                        .sortedByDescending { it.value.size }
                        .toList()
                }
                val activeArtistTracks = uiState.selectedArtist?.let { artist ->
                    groupedArtists.firstOrNull { it.key == artist }?.value
                }

                if (uiState.selectedArtist != null && activeArtistTracks != null) {
                    DrillDownHeader(
                        title = uiState.selectedArtist!!,
                        subtitle = "${activeArtistTracks.size} lagu ditemukan",
                        onBack = { viewModel.selectArtist(null) }
                    )
                    SearchTrackList(
                        tracks = activeArtistTracks,
                        keyPrefix = "search_art_track",
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = handleTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onMoreClick = onTrackMoreClick
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = groupedArtists,
                            key = { idx, entry -> "search_artist_${entry.key}_$idx" }
                        ) { _, (artist, tracks) ->
                            GroupedSearchCard(
                                title = artist,
                                subtitle = "${tracks.size} lagu • ${tracks.firstOrNull()?.title.orEmpty()}",
                                thumbnail = tracks.firstOrNull()?.thumbnail.orEmpty(),
                                fallbackIcon = Icons.Filled.Person,
                                circularThumb = true,
                                onClick = { viewModel.selectArtist(artist) }
                            )
                        }
                    }
                }
            }

            uiState.selectedCategory == "Album" -> {
                val groupedAlbums = remember(uiState.results) {
                    uiState.results
                        .groupBy { track ->
                            if (track.album.isNotBlank() && !track.album.equals("YouTube Audio", ignoreCase = true)) {
                                track.album
                            } else {
                                track.title
                            }
                        }
                        .entries
                        .toList()
                }
                val activeAlbumTracks = uiState.selectedAlbum?.let { albumName ->
                    groupedAlbums.firstOrNull { it.key == albumName }?.value
                }

                if (uiState.selectedAlbum != null && activeAlbumTracks != null) {
                    DrillDownHeader(
                        title = uiState.selectedAlbum!!,
                        subtitle = "${activeAlbumTracks.size} lagu",
                        onBack = { viewModel.selectAlbum(null) }
                    )
                    SearchTrackList(
                        tracks = activeAlbumTracks,
                        keyPrefix = "search_alb_track",
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = handleTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onMoreClick = onTrackMoreClick
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = groupedAlbums,
                            key = { idx, entry -> "search_album_${entry.key}_$idx" }
                        ) { _, (albumTitle, tracks) ->
                            val first = tracks.first()
                            val subtitle = if (tracks.size > 1) {
                                "${first.artist} • ${tracks.size} lagu"
                            } else {
                                "${first.artist} • ${first.durationFormatted}"
                            }
                            GroupedSearchCard(
                                title = albumTitle,
                                subtitle = subtitle,
                                thumbnail = first.thumbnail,
                                fallbackIcon = Icons.Filled.Album,
                                circularThumb = false,
                                onClick = {
                                    if (tracks.size == 1) {
                                        handleTrackClick(first, uiState.results)
                                    } else {
                                        viewModel.selectAlbum(albumTitle)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            uiState.selectedCategory == "Playlist" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.matchedPlaylists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Playlist Tersimpan & Spotify",
                                style = GlassTypography.titleMedium,
                                color = glassConfig.textSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        itemsIndexed(
                            items = uiState.matchedPlaylists,
                            key = { idx, pl -> "search_saved_pl_${pl.id}_$idx" }
                        ) { _, playlist ->
                            GroupedSearchCard(
                                title = playlist.name,
                                subtitle = "${playlist.trackCount} lagu • ${if (playlist.isSpotifyImport) "Impor Spotify" else "Playlist Lokal"}",
                                thumbnail = playlist.artwork,
                                fallbackIcon = Icons.Filled.LibraryMusic,
                                circularThumb = false,
                                onClick = { onPlaylistClick(playlist) }
                            )
                        }
                    }

                    if (uiState.results.isNotEmpty()) {
                        item {
                            Text(
                                text = "Mix & Kompilasi YouTube",
                                style = GlassTypography.titleMedium,
                                color = glassConfig.textSecondary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        itemsIndexed(
                            items = uiState.results,
                            key = { index, track -> "search_pl_yt_${track.id}_$index" }
                        ) { _, track ->
                            GlassSongCard(
                                track = track,
                                isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                                onClick = { handleTrackClick(track, uiState.results) },
                                onFavoriteClick = { viewModel.toggleFavorite(track) },
                                onMoreClick = { onTrackMoreClick(track) }
                            )
                        }
                    }
                }
            }

            else -> {
                SearchTrackList(
                    tracks = uiState.results,
                    keyPrefix = "search",
                    currentPlayingTrack = currentPlayingTrack,
                    isPlaying = isPlaying,
                    onTrackClick = handleTrackClick,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onMoreClick = onTrackMoreClick
                )
            }
        }
    }
}

@Composable
private fun SearchTrackList(
    tracks: List<Track>,
    keyPrefix: String,
    currentPlayingTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = tracks,
            key = { index, track -> "${keyPrefix}_${track.id}_$index" }
        ) { _, track ->
            GlassSongCard(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                onClick = { onTrackClick(track, tracks) },
                onFavoriteClick = { onFavoriteClick(track) },
                onMoreClick = { onMoreClick(track) }
            )
        }
    }
}

@Composable
private fun DrillDownHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBack,
            size = 38.dp,
            iconSize = 18.dp,
            contentDescription = "Back"
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.titleMedium,
                color = glassConfig.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = GlassTypography.labelSmall,
                color = glassConfig.textSecondary
            )
        }
    }
}

@Composable
private fun GroupedSearchCard(
    title: String,
    subtitle: String,
    thumbnail: String,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    circularThumb: Boolean,
    onClick: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    val shape = if (circularThumb) CircleShape else RoundedCornerShape(14.dp)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(shape)
                    .background(glassConfig.placeholderBrush)
                    .border(0.8.dp, glassConfig.borderBrush, shape),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = glassConfig.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = GlassTypography.titleMedium,
                    color = glassConfig.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = GlassTypography.labelSmall,
                    color = glassConfig.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = glassConfig.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchIdleDiscoveryContent(
    searchHistory: List<String>,
    onSelectTerm: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    val quickSuggestions = listOf("Pop Indonesia", "Indie Senja", "Lo-Fi Chill", "Top Hits", "Acoustic")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Saran Cepat",
                style = GlassTypography.titleMedium,
                color = glassConfig.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickSuggestions) { term ->
                    GlassChip(
                        text = term,
                        isSelected = false,
                        onClick = { onSelectTerm(term) }
                    )
                }
            }
        }

        if (searchHistory.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pencarian Terakhir",
                        style = GlassTypography.titleMedium,
                        color = glassConfig.textSecondary
                    )
                    Text(
                        text = "Hapus Semua",
                        style = GlassTypography.labelSmall,
                        color = glassConfig.accentPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onClearHistory)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            itemsIndexed(
                items = searchHistory,
                key = { index, q -> "search_hist_${q}_$index" }
            ) { _, query ->
                GlassCard(
                    onClick = { onSelectTerm(query) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = query,
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Hapus riwayat",
                            tint = glassConfig.textTertiary,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable { onRemoveHistory(query) }
                                .padding(2.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(6.dp, CircleShape, ambientColor = glassConfig.shadowAmbient)
                                .clip(CircleShape)
                                .background(brush = glassConfig.cardBrush)
                                .border(1.dp, glassConfig.borderBrush, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = glassConfig.textTertiary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Cari apa saja",
                            style = GlassTypography.headlineMedium,
                            color = glassConfig.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jelajahi jutaan lagu, artis, album, atau ketuk saran cepat di atas.",
                            style = GlassTypography.bodyMedium,
                            color = glassConfig.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
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
                    .shadow(6.dp, CircleShape, ambientColor = glassConfig.shadowAmbient)
                    .clip(CircleShape)
                    .background(brush = glassConfig.cardBrush)
                    .border(1.dp, glassConfig.borderBrush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = glassConfig.accentPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pencarian terganggu",
                style = GlassTypography.headlineMedium,
                color = glassConfig.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = GlassTypography.bodyMedium,
                color = glassConfig.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            GlassButton(
                text = "Coba Lagi",
                isPrimary = true,
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun SearchEmptyState(title: String, subtitle: String) {
    val glassConfig = LocalGlassConfig.current
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
                    .shadow(6.dp, CircleShape, ambientColor = glassConfig.shadowAmbient)
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
                text = title,
                style = GlassTypography.headlineMedium,
                color = glassConfig.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = GlassTypography.bodyMedium,
                color = glassConfig.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
