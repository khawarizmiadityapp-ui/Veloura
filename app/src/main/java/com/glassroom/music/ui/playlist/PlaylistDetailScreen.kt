package com.glassroom.music.ui.playlist

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.ImportStatus
import com.glassroom.music.domain.model.SpotifyImportState
import com.glassroom.music.domain.model.Track
import com.glassroom.music.domain.repository.MusicRepository
import com.glassroom.music.domain.repository.PlaylistRepository
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.theme.*
import kotlinx.coroutines.Job
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
    onTrackMoreClick: ((Track) -> Unit)? = null,
    musicRepository: MusicRepository? = null,
    onShowMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val playlistWithTracks by playlistRepository.getPlaylistWithTracks(playlistId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val glassConfig = LocalGlassConfig.current

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Default") }
    var isReorderMode by remember { mutableStateOf(false) }
    var syncState by remember { mutableStateOf<SpotifyImportState?>(null) }
    var syncJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { syncJob?.cancel() }
    }

    val playlist = playlistWithTracks?.playlist
    val tracks = playlistWithTracks?.tracks ?: emptyList()

    val spotifySyncTarget = remember(playlist) {
        if (playlist == null || !playlist.isSpotifyImport) {
            null
        } else {
            val descUrl = Regex("""https?://(?:open\.)?spotify\.[^\s)]+""", RegexOption.IGNORE_CASE)
                .find(playlist.description)?.value
            when {
                descUrl != null -> descUrl
                Regex("""^[a-zA-Z0-9]{22}$""").matches(playlist.id) -> playlist.id
                else -> null
            }
        }
    }

    val displayedTracks = remember(tracks, searchQuery, sortMode, isReorderMode) {
        val filtered = if (!isReorderMode && searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            tracks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true)
            }
        } else {
            tracks
        }

        if (isReorderMode) {
            filtered
        } else {
            when (sortMode) {
                "Judul" -> filtered.sortedBy { it.title.lowercase() }
                "Artis" -> filtered.sortedBy { it.artist.lowercase() }
                "Durasi" -> filtered.sortedBy { it.duration }
                else -> filtered
            }
        }
    }

    val sortOptions = listOf("Default", "Judul", "Artis", "Durasi")

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
                contentDescription = "Kembali"
            )
            if (playlist != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (musicRepository != null && spotifySyncTarget != null) {
                        GlassIconButton(
                            icon = Icons.Filled.Sync,
                            onClick = {
                                if (syncJob?.isActive == true) return@GlassIconButton
                                syncJob = coroutineScope.launch {
                                    musicRepository.importSpotifyPlaylist(spotifySyncTarget).collect { state ->
                                        syncState = state
                                        if (state.status == ImportStatus.SUCCESS) {
                                            onShowMessage?.invoke("Playlist disinkronkan (${state.totalTracks} lagu)")
                                            syncState = null
                                        } else if (state.status == ImportStatus.ERROR) {
                                            onShowMessage?.invoke(state.errorMessage ?: "Gagal menyinkronkan playlist")
                                            syncState = null
                                        }
                                    }
                                }
                            },
                            contentDescription = "Sinkronkan Playlist Spotify",
                            tint = glassConfig.accentSecondary
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Filled.Edit,
                        onClick = { showEditDialog = true },
                        contentDescription = "Edit Playlist"
                    )
                    GlassIconButton(
                        icon = Icons.Filled.Delete,
                        onClick = { showDeleteConfirmDialog = true },
                        contentDescription = "Hapus Playlist",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        if (playlist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Playlist tidak ditemukan",
                    style = GlassTypography.bodyLarge,
                    color = glassConfig.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp)
            ) {
                // Playlist Artwork & Metadata Header
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
                                    elevation = if (glassConfig.isPaper) 10.dp else 16.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    ambientColor = glassConfig.shadowAmbient
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(glassConfig.placeholderBrush)
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
                                    tint = glassConfig.textSecondary,
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
                                text = "${tracks.size} lagu",
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
                                        text = "Impor Spotify",
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
                                text = "Putar Semua",
                                icon = Icons.Filled.PlayArrow,
                                isPrimary = true,
                                onClick = {
                                    if (displayedTracks.isNotEmpty()) onPlayAll(displayedTracks)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            GlassButton(
                                text = "Acak",
                                icon = Icons.Filled.Shuffle,
                                isPrimary = false,
                                onClick = {
                                    if (displayedTracks.isNotEmpty()) onPlayAll(displayedTracks.shuffled())
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Spotify Sync Inline Progress
                        syncState?.let { state ->
                            if (state.status == ImportStatus.READING_PLAYLIST || state.status == ImportStatus.SEARCHING_MATCHES) {
                                Spacer(modifier = Modifier.height(14.dp))
                                GlassCard(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (state.status == ImportStatus.READING_PLAYLIST) {
                                                "Membaca playlist terbaru dari Spotify..."
                                            } else {
                                                "Menyinkronkan ${state.matchedTracks}/${state.totalTracks}: ${state.currentSearchingTitle}"
                                            },
                                            style = GlassTypography.labelSmall,
                                            color = glassConfig.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { state.progressPercent },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(CircleShape),
                                            color = glassConfig.accentSecondary,
                                            trackColor = glassConfig.inactiveTrackColor
                                        )
                                    }
                                }
                            }
                        }

                        // Search & Sort / Reorder Toolbar
                        if (tracks.size >= 4 || searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            GlassSearchBar(
                                query = searchQuery,
                                onQueryChange = {
                                    if (isReorderMode) isReorderMode = false
                                    searchQuery = it
                                },
                                placeholder = "Cari lagu atau artis di playlist..."
                            )
                        }

                        if (tracks.size >= 2) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sortOptions) { option ->
                                    GlassChip(
                                        text = option,
                                        isSelected = !isReorderMode && sortMode == option,
                                        onClick = {
                                            isReorderMode = false
                                            sortMode = option
                                        }
                                    )
                                }
                                item {
                                    GlassChip(
                                        text = if (isReorderMode) "Selesai Urut" else "Atur Urutan",
                                        isSelected = isReorderMode,
                                        onClick = {
                                            isReorderMode = !isReorderMode
                                            if (isReorderMode) {
                                                searchQuery = ""
                                                sortMode = "Default"
                                            }
                                        }
                                    )
                                }
                            }

                            if (!isReorderMode && sortMode != "Default" && searchQuery.isBlank() && displayedTracks.size > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Simpan urutan $sortMode sebagai urutan default",
                                        style = GlassTypography.labelSmall,
                                        color = glassConfig.accentPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                val orderedIds = displayedTracks.map { it.id }
                                                coroutineScope.launch {
                                                    playlistRepository.reorderPlaylistTracks(playlistId, orderedIds)
                                                    sortMode = "Default"
                                                    onShowMessage?.invoke("Urutan playlist disimpan")
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (displayedTracks.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada lagu yang cocok dengan \"$searchQuery\"",
                                style = GlassTypography.bodyMedium,
                                color = glassConfig.textSecondary
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = displayedTracks,
                        key = { index, track -> "detail_track_${track.id}_$index" }
                    ) { index, track ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            if (isReorderMode) {
                                PlaylistReorderRow(
                                    track = track,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < displayedTracks.lastIndex,
                                    onMoveUp = {
                                        val mutable = tracks.toMutableList()
                                        if (index > 0 && index in mutable.indices) {
                                            val moved = mutable.removeAt(index)
                                            mutable.add(index - 1, moved)
                                            coroutineScope.launch {
                                                playlistRepository.reorderPlaylistTracks(
                                                    playlistId,
                                                    mutable.map { it.id }
                                                )
                                            }
                                        }
                                    },
                                    onMoveDown = {
                                        val mutable = tracks.toMutableList()
                                        if (index < mutable.lastIndex && index in mutable.indices) {
                                            val moved = mutable.removeAt(index)
                                            mutable.add(index + 1, moved)
                                            coroutineScope.launch {
                                                playlistRepository.reorderPlaylistTracks(
                                                    playlistId,
                                                    mutable.map { it.id }
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                GlassSongCard(
                                    track = track,
                                    isPlaying = currentPlayingTrack?.id == track.id && isPlaying,
                                    onClick = { onPlayTrack(track, displayedTracks) },
                                    onFavoriteClick = {
                                        coroutineScope.launch {
                                            playlistRepository.toggleFavorite(track)
                                        }
                                    },
                                    onMoreClick = {
                                        if (onTrackMoreClick != null) {
                                            onTrackMoreClick(track)
                                        } else {
                                            coroutineScope.launch {
                                                playlistRepository.removeTrackFromPlaylist(playlistId, track.id)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && playlist != null) {
        var editingName by remember(playlist.name) { mutableStateOf(playlist.name) }
        var editingDesc by remember(playlist.description) { mutableStateOf(playlist.description) }

        GlassDialog(onDismissRequest = { showEditDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Edit Playlist",
                    style = GlassTypography.headlineMedium,
                    color = glassConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassConfig.inputSurfaceColor)
                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (editingName.isEmpty()) {
                        Text(
                            text = "Nama playlist...",
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textTertiary
                        )
                    }
                    BasicTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                        cursorBrush = SolidColor(glassConfig.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassConfig.inputSurfaceColor)
                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (editingDesc.isEmpty()) {
                        Text(
                            text = "Deskripsi (opsional)",
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textTertiary
                        )
                    }
                    BasicTextField(
                        value = editingDesc,
                        onValueChange = { editingDesc = it },
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                        cursorBrush = SolidColor(glassConfig.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Batal",
                        onClick = { showEditDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Simpan",
                        isPrimary = true,
                        onClick = {
                            val trimmed = editingName.trim()
                            if (trimmed.isNotEmpty()) {
                                showEditDialog = false
                                coroutineScope.launch {
                                    playlistRepository.renamePlaylist(
                                        playlistId,
                                        trimmed,
                                        editingDesc.trim()
                                    )
                                    onShowMessage?.invoke("Playlist diperbarui")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog && playlist != null) {
        GlassDialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Hapus Playlist?",
                    style = GlassTypography.headlineMedium,
                    color = glassConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Playlist \"${playlist.name}\" dan ${tracks.size} lagu di dalamnya akan dihapus dari perpustakaanmu.",
                    style = GlassTypography.bodyMedium,
                    color = glassConfig.textSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Batal",
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Hapus",
                        isPrimary = true,
                        onClick = {
                            showDeleteConfirmDialog = false
                            coroutineScope.launch {
                                playlistRepository.deletePlaylist(playlistId)
                                onBack()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistReorderRow(
    track: Track,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    GlassCard(
        shape = RoundedCornerShape(18.dp),
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(glassConfig.placeholderBrush)
                    .border(0.8.dp, glassConfig.borderBrush, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (track.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.thumbnail)
                            .crossfade(true)
                            .memoryCacheKey(track.id)
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = glassConfig.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = GlassTypography.titleMedium,
                    color = glassConfig.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = GlassTypography.bodyMedium,
                    color = glassConfig.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .alpha(if (canMoveUp) 1f else 0.3f)
                        .clip(CircleShape)
                        .background(glassConfig.inactiveTrackColor)
                        .clickable(enabled = canMoveUp, onClick = onMoveUp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Naikkan",
                        tint = glassConfig.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .alpha(if (canMoveDown) 1f else 0.3f)
                        .clip(CircleShape)
                        .background(glassConfig.inactiveTrackColor)
                        .clickable(enabled = canMoveDown, onClick = onMoveDown),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Turunkan",
                        tint = glassConfig.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
