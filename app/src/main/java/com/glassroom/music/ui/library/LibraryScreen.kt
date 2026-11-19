package com.glassroom.music.ui.library

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    onTrackMoreClick: (Track) -> Unit = {},
    requestedTab: String? = null,
    onRequestedTabConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val glassConfig = LocalGlassConfig.current
    var selectedArtist by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            viewModel.selectTab(requestedTab)
            onRequestedTabConsumed()
        }
    }

    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab != "Artis") {
            selectedArtist = null
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        GlassTopBar(
            title = "Koleksi",
            subtitle = "Musik Tersimpanmu"
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
            "Lagu Disukai" -> {
                if (uiState.favoriteTracks.isEmpty()) {
                    EmptyLibraryState(
                        title = "Belum ada lagu disukai",
                        subtitle = "Ketuk ikon hati pada lagu mana saja untuk menyimpannya di sini."
                    )
                } else {
                    TrackList(
                        tracks = uiState.favoriteTracks,
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = onTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onMoreClick = onTrackMoreClick
                    )
                }
            }

            "Baru Diputar" -> {
                if (uiState.recentlyPlayed.isEmpty()) {
                    EmptyLibraryState(
                        title = "Belum ada riwayat putar",
                        subtitle = "Lagu yang baru kamu dengarkan akan muncul di sini."
                    )
                } else {
                    TrackList(
                        tracks = uiState.recentlyPlayed,
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = onTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onMoreClick = onTrackMoreClick
                    )
                }
            }

            "Offline" -> {
                if (uiState.offlineTracks.isEmpty()) {
                    EmptyLibraryState(
                        title = "Belum ada lagu offline",
                        subtitle = "Ketuk ikon titik tiga (⋮) pada lagu apa saja lalu pilih Simpan Offline."
                    )
                } else {
                    TrackList(
                        tracks = uiState.offlineTracks,
                        currentPlayingTrack = currentPlayingTrack,
                        isPlaying = isPlaying,
                        onTrackClick = onTrackClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onMoreClick = onTrackMoreClick
                    )
                }
            }

            "Artis" -> {
                val groupedByArtist = uiState.favoriteTracks
                    .filter { it.artist.isNotBlank() }
                    .groupBy { it.artist }
                    .entries
                    .sortedByDescending { it.value.size }
                    .toList()

                val activeArtistTracks = selectedArtist?.let { artistName ->
                    groupedByArtist.firstOrNull { it.key == artistName }?.value
                }

                if (groupedByArtist.isEmpty()) {
                    EmptyLibraryState(
                        title = "Belum ada artis",
                        subtitle = "Sukai beberapa lagu agar daftar artis terkelompok otomatis di sini."
                    )
                } else if (selectedArtist != null && activeArtistTracks != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            GlassIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                onClick = { selectedArtist = null },
                                size = 38.dp,
                                iconSize = 18.dp,
                                contentDescription = "Kembali ke Artis"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedArtist!!,
                                    style = GlassTypography.titleMedium,
                                    color = glassConfig.textPrimary
                                )
                                Text(
                                    text = "${activeArtistTracks.size} lagu disukai",
                                    style = GlassTypography.labelSmall,
                                    color = glassConfig.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TrackList(
                            tracks = activeArtistTracks,
                            currentPlayingTrack = currentPlayingTrack,
                            isPlaying = isPlaying,
                            onTrackClick = onTrackClick,
                            onFavoriteClick = viewModel::toggleFavorite,
                            onMoreClick = onTrackMoreClick
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(groupedByArtist) { (artist, artistTracks) ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedArtist = artist }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(glassConfig.placeholderBrush)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist,
                                            style = GlassTypography.titleMedium,
                                            color = glassConfig.textPrimary
                                        )
                                        Text(
                                            text = "${artistTracks.size} lagu",
                                            style = GlassTypography.labelSmall,
                                            color = glassConfig.textSecondary
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
                    }
                }
            }

            "Playlist" -> {
                if (uiState.playlists.isEmpty()) {
                    EmptyLibraryState(
                        title = "Belum ada playlist",
                        subtitle = "Buat playlist atau impor dari Spotify lewat menu Playlist."
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
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(glassConfig.placeholderBrush)
                                            .border(0.8.dp, glassConfig.borderBrush, RoundedCornerShape(14.dp)),
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
                                            Icon(
                                                imageVector = Icons.Filled.MusicNote,
                                                contentDescription = null,
                                                tint = glassConfig.textSecondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = playlist.name,
                                                style = GlassTypography.titleMedium,
                                                color = glassConfig.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (playlist.isSpotifyImport) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xCC10B981))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
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
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${playlist.trackCount} lagu",
                                            style = GlassTypography.labelSmall,
                                            color = glassConfig.textSecondary
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
            key = { index, track -> "lib_track_${track.id}_$index" }
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
                    .shadow(6.dp, CircleShape, ambientColor = glassConfig.shadowAmbient)
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
