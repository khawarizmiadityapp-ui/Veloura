package com.glassroom.music.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.*
import com.glassroom.music.ui.components.*
import com.glassroom.music.ui.home.SectionHeader
import com.glassroom.music.ui.theme.*

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val glassConfig = LocalGlassConfig.current

    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    val spotifyPlaylists = uiState.playlists.filter { it.isSpotifyImport }
    val localPlaylists = uiState.playlists.filter { !it.isSpotifyImport }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        GlassTopBar(
            title = "Playlist",
            subtitle = "Koleksi Daftar Putar"
        )

        // Action Buttons Row: [+ Impor Spotify] and [+ Buat Baru]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                text = "Impor Spotify",
                icon = Icons.Filled.CloudDownload,
                isPrimary = true,
                onClick = { viewModel.openImportDialog() },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Buat Baru",
                icon = Icons.Filled.Add,
                isPrimary = false,
                onClick = { viewModel.openCreateDialog() },
                modifier = Modifier.weight(0.7f)
            )
        }

        if (uiState.playlists.isEmpty()) {
            // Empty State
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
                            imageVector = Icons.Outlined.LibraryMusic,
                            contentDescription = null,
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada playlist",
                        style = GlassTypography.headlineMedium,
                        color = glassConfig.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Impor playlist dari Spotify atau buat daftar putarmu sendiri.",
                        style = GlassTypography.bodyMedium,
                        color = glassConfig.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 150.dp)
            ) {
                // Section: Imported from Spotify
                if (spotifyPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Impor dari Spotify",
                            subtitle = "Tersinkron langsung dari Spotify"
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(
                                items = spotifyPlaylists,
                                key = { index, p -> "spotify_pl_${p.id}_$index" }
                            ) { _, p ->
                                GlassPlaylistCard(
                                    playlist = p,
                                    onClick = { onPlaylistClick(p) },
                                    size = 140.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // Section: Your Playlists
                if (localPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Playlist Kamu",
                            subtitle = "Daftar putar buatan di Veloura"
                        )
                    }
                    itemsIndexed(
                        items = localPlaylists,
                        key = { index, p -> "local_pl_${p.id}_$index" }
                    ) { _, p ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                            GlassCard(
                                onClick = { onPlaylistClick(p) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(glassConfig.placeholderBrush)
                                            .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (p.artwork.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(p.artwork)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = p.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.MusicNote,
                                                contentDescription = null,
                                                tint = glassConfig.textTertiary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.name,
                                            style = GlassTypography.titleMedium,
                                            color = glassConfig.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${p.trackCount} lagu",
                                            style = GlassTypography.bodyMedium,
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

    // Spotify Import Glass Dialog
    if (uiState.showImportDialog) {
        val inputBg = glassConfig.inputSurfaceColor
        GlassDialog(onDismissRequest = { viewModel.closeImportDialog() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Impor Playlist",
                    style = GlassTypography.headlineMedium,
                    color = glassConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tempel tautan playlist Spotify untuk dicocokkan dan diimpor ke Veloura.",
                    style = GlassTypography.bodyMedium,
                    color = glassConfig.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // URL Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(inputBg)
                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (uiState.importUrlInput.isEmpty()) {
                        Text(
                            text = "https://open.spotify.com/playlist/...",
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textTertiary
                        )
                    }
                    BasicTextField(
                        value = uiState.importUrlInput,
                        onValueChange = viewModel::onImportUrlChange,
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                        cursorBrush = SolidColor(glassConfig.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import State & Progress Feedback
                when (uiState.importState.status) {
                    ImportStatus.READING_PLAYLIST -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = glassConfig.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Membaca playlist...",
                                style = GlassTypography.bodyMedium,
                                color = glassConfig.textPrimary
                            )
                        }
                    }

                    ImportStatus.SEARCHING_MATCHES -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Ditemukan ${uiState.importState.totalTracks} lagu",
                                style = GlassTypography.titleMedium,
                                color = glassConfig.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.importState.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = glassConfig.accentPrimary,
                                trackColor = glassConfig.inactiveTrackColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mencocokkan: ${uiState.importState.currentSearchingTitle}",
                                style = GlassTypography.labelSmall,
                                color = glassConfig.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Recent matched tracks list in dialog
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                uiState.importState.items.takeLast(4).forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = GlassAccentMint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${item.title} - ${item.artist}",
                                            style = GlassTypography.labelSmall,
                                            color = glassConfig.textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ImportStatus.SUCCESS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = GlassAccentMint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Playlist berhasil diimpor!",
                                style = GlassTypography.titleMedium,
                                color = GlassAccentMint
                            )
                        }
                    }

                    ImportStatus.ERROR -> {
                        Text(
                            text = uiState.importState.errorMessage ?: "Terjadi kesalahan saat mengimpor.",
                            style = GlassTypography.bodyMedium,
                            color = Color(0xFFDC2626)
                        )
                    }

                    ImportStatus.IDLE -> {}
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassButton(
                        text = if (uiState.importState.status == ImportStatus.SUCCESS) "Selesai" else "Batal",
                        onClick = { viewModel.closeImportDialog() }
                    )
                    if (uiState.importState.status != ImportStatus.SUCCESS) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassButton(
                            text = "IMPOR",
                            isPrimary = true,
                            onClick = { viewModel.startSpotifyImport() }
                        )
                    }
                }
            }
        }
    }

    // Create New Playlist Dialog
    if (uiState.showCreateDialog) {
        val inputBg = glassConfig.inputSurfaceColor
        GlassDialog(onDismissRequest = { viewModel.closeCreateDialog() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Playlist Baru",
                    style = GlassTypography.headlineMedium,
                    color = glassConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(inputBg)
                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (newPlaylistName.isEmpty()) {
                        Text(
                            text = "Nama Playlist",
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textTertiary
                        )
                    }
                    BasicTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                        cursorBrush = SolidColor(glassConfig.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(inputBg)
                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (newPlaylistDesc.isEmpty()) {
                        Text(
                            text = "Deskripsi (opsional)",
                            style = GlassTypography.bodyLarge,
                            color = glassConfig.textTertiary
                        )
                    }
                    BasicTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = glassConfig.textPrimary),
                        cursorBrush = SolidColor(glassConfig.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassButton(
                        text = "Batal",
                        onClick = { viewModel.closeCreateDialog() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        text = "Buat",
                        isPrimary = true,
                        onClick = {
                            viewModel.createNewPlaylist(newPlaylistName, newPlaylistDesc)
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                        }
                    )
                }
            }
        }
    }
}
