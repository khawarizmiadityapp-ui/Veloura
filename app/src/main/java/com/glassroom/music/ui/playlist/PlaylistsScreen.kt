package com.glassroom.music.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    val spotifyPlaylists = uiState.playlists.filter { it.isSpotifyImport }
    val localPlaylists = uiState.playlists.filter { !it.isSpotifyImport }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        GlassTopBar(
            title = "Playlists",
            subtitle = "Your Collections"
        )

        // Action Buttons Row: [+ Import Playlist] and [+ Create Playlist]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                text = "Import Playlist",
                icon = Icons.Filled.CloudDownload,
                isPrimary = true,
                onClick = { viewModel.openImportDialog() },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Create",
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
                    Icon(
                        imageVector = Icons.Outlined.LibraryMusic,
                        contentDescription = null,
                        tint = GlassTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No music here yet",
                        style = GlassTypography.headlineMedium,
                        color = GlassTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import a playlist from Spotify and make the room yours.",
                        style = GlassTypography.bodyMedium,
                        color = GlassTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Section: Imported from Spotify
                if (spotifyPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Imported from Spotify")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(spotifyPlaylists) { p ->
                                GlassPlaylistCard(
                                    playlist = p,
                                    onClick = { onPlaylistClick(p) }
                                )
                            }
                        }
                    }
                }

                // Section: Your Playlists
                if (localPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Your Playlists")
                    }
                    items(localPlaylists) { p ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
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
                                            .background(Color(0xFFE2E8F0))
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.name,
                                            style = GlassTypography.titleMedium,
                                            color = GlassTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${p.trackCount} tracks",
                                            style = GlassTypography.bodyMedium,
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

    // Spotify Import Glass Dialog
    if (uiState.showImportDialog) {
        GlassDialog(onDismissRequest = { viewModel.closeImportDialog() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Import Playlist",
                    style = GlassTypography.headlineMedium,
                    color = GlassTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Paste a Spotify playlist URL to match and import into Glassroom.",
                    style = GlassTypography.bodyMedium,
                    color = GlassTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // URL Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x66FFFFFF))
                        .border(1.dp, GlassBorderWhite, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (uiState.importUrlInput.isEmpty()) {
                        Text(
                            text = "https://open.spotify.com/playlist/...",
                            style = GlassTypography.bodyLarge,
                            color = GlassTextTertiary
                        )
                    }
                    BasicTextField(
                        value = uiState.importUrlInput,
                        onValueChange = viewModel::onImportUrlChange,
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = GlassTextPrimary),
                        cursorBrush = SolidColor(GlassTextPrimary),
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
                                color = Color(0xFF2C3E50)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Reading playlist...",
                                style = GlassTypography.bodyMedium,
                                color = GlassTextPrimary
                            )
                        }
                    }

                    ImportStatus.SEARCHING_MATCHES -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Found ${uiState.importState.totalTracks} tracks",
                                style = GlassTypography.titleMedium,
                                color = GlassTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.importState.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF2C3E50),
                                trackColor = Color(0x330F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Matching: ${uiState.importState.currentSearchingTitle}",
                                style = GlassTypography.labelSmall,
                                color = GlassTextSecondary
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
                                            color = GlassTextPrimary,
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
                                text = "Playlist imported successfully!",
                                style = GlassTypography.titleMedium,
                                color = GlassAccentMint
                            )
                        }
                    }

                    ImportStatus.ERROR -> {
                        Text(
                            text = uiState.importState.errorMessage ?: "Something went wrong.",
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
                        text = if (uiState.importState.status == ImportStatus.SUCCESS) "Done" else "Cancel",
                        onClick = { viewModel.closeImportDialog() }
                    )
                    if (uiState.importState.status != ImportStatus.SUCCESS) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassButton(
                            text = "IMPORT",
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
        GlassDialog(onDismissRequest = { viewModel.closeCreateDialog() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "New Playlist",
                    style = GlassTypography.headlineMedium,
                    color = GlassTextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x66FFFFFF))
                        .border(1.dp, GlassBorderWhite, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (newPlaylistName.isEmpty()) {
                        Text(
                            text = "Playlist Name",
                            style = GlassTypography.bodyLarge,
                            color = GlassTextTertiary
                        )
                    }
                    BasicTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = GlassTextPrimary),
                        cursorBrush = SolidColor(GlassTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x66FFFFFF))
                        .border(1.dp, GlassBorderWhite, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (newPlaylistDesc.isEmpty()) {
                        Text(
                            text = "Description (optional)",
                            style = GlassTypography.bodyLarge,
                            color = GlassTextTertiary
                        )
                    }
                    BasicTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        singleLine = true,
                        textStyle = GlassTypography.bodyLarge.copy(color = GlassTextPrimary),
                        cursorBrush = SolidColor(GlassTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = { viewModel.closeCreateDialog() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        text = "Create",
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
