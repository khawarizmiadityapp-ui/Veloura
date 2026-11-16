package com.glassroom.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Playlist
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.theme.GlassTypography
import com.glassroom.music.ui.theme.LocalGlassConfig

private enum class TrackOptionsStep {
    MAIN_MENU,
    PICK_PLAYLIST,
    CREATE_PLAYLIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    track: Track,
    playlists: List<Playlist>,
    existingPlaylistIds: Set<String>,
    isFavorite: Boolean,
    sourcePlaylistId: String? = null,
    onAddToPlaylist: (Playlist) -> Unit,
    onCreatePlaylistAndAdd: (name: String, description: String) -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glassConfig = LocalGlassConfig.current

    var currentStep by remember(track.id) { mutableStateOf(TrackOptionsStep.MAIN_MENU) }
    var newPlaylistName by remember(track.id) { mutableStateOf("") }
    var newPlaylistDesc by remember(track.id) { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassConfig.sheetContainerColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(glassConfig.inactiveTrackColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Selected Track Summary Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(glassConfig.placeholderBrush)
                        .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(track.thumbnail)
                                .crossfade(true)
                                .memoryCacheKey(track.id.ifBlank { track.thumbnail })
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
                            modifier = Modifier.size(22.dp)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = GlassTypography.bodyMedium,
                        color = glassConfig.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            when (currentStep) {
                TrackOptionsStep.MAIN_MENU -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrackOptionActionRow(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            title = "Tambah ke Playlist",
                            subtitle = "Simpan ke daftar putar pilihanmu",
                            iconTint = glassConfig.accentPrimary,
                            trailingChevron = true,
                            onClick = { currentStep = TrackOptionsStep.PICK_PLAYLIST }
                        )
                        TrackOptionActionRow(
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            title = "Putar Berikutnya",
                            subtitle = "Mainkan tepat setelah lagu ini",
                            iconTint = glassConfig.textPrimary,
                            onClick = {
                                onPlayNext()
                                onDismiss()
                            }
                        )
                        TrackOptionActionRow(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            title = "Tambahkan ke Antrean",
                            subtitle = "Masukkan ke akhir antrean pemutaran",
                            iconTint = glassConfig.textPrimary,
                            onClick = {
                                onAddToQueue()
                                onDismiss()
                            }
                        )
                        TrackOptionActionRow(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            title = if (isFavorite) "Hapus dari Favorit" else "Tambahkan ke Favorit",
                            subtitle = if (isFavorite) "Hapus lagu ini dari Liked Songs" else "Simpan ke Liked Songs",
                            iconTint = if (isFavorite) glassConfig.heartActive else glassConfig.textPrimary,
                            onClick = {
                                onToggleFavorite()
                                onDismiss()
                            }
                        )
                        if (sourcePlaylistId != null && onRemoveFromPlaylist != null) {
                            TrackOptionActionRow(
                                icon = Icons.Filled.DeleteOutline,
                                title = "Hapus dari Playlist Ini",
                                subtitle = "Keluarkan lagu dari daftar putar saat ini",
                                iconTint = Color(0xFFEF4444),
                                titleColor = Color(0xFFEF4444),
                                onClick = {
                                    onRemoveFromPlaylist()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                TrackOptionsStep.PICK_PLAYLIST -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = { currentStep = TrackOptionsStep.MAIN_MENU },
                            size = 36.dp,
                            iconSize = 18.dp,
                            contentDescription = "Back"
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pilih Playlist",
                                style = GlassTypography.headlineMedium,
                                color = glassConfig.textPrimary
                            )
                            Text(
                                text = "Pilih tujuan untuk menambahkan lagu ini",
                                style = GlassTypography.labelSmall,
                                color = glassConfig.textTertiary
                            )
                        }
                    }

                    TrackOptionActionRow(
                        icon = Icons.Filled.Add,
                        title = "Buat Playlist Baru",
                        subtitle = "Buat playlist baru lalu tambahkan lagu ini",
                        iconTint = glassConfig.accentPrimary,
                        trailingChevron = true,
                        onClick = { currentStep = TrackOptionsStep.CREATE_PLAYLIST }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada playlist. Ketuk 'Buat Playlist Baru' di atas.",
                                style = GlassTypography.bodyMedium,
                                color = glassConfig.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = playlists,
                                key = { index, pl -> "pick_pl_${pl.id}_$index" }
                            ) { _, playlist ->
                                val alreadyInPlaylist = playlist.id in existingPlaylistIds
                                GlassCard(
                                    onClick = {
                                        onAddToPlaylist(playlist)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(glassConfig.placeholderBrush)
                                                .border(0.8.dp, glassConfig.borderBrush, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (playlist.artwork.isNotBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(playlist.artwork)
                                                        .crossfade(true)
                                                        .memoryCacheKey(playlist.id)
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
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = GlassTypography.titleMedium,
                                                color = glassConfig.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${playlist.trackCount} lagu",
                                                style = GlassTypography.labelSmall,
                                                color = glassConfig.textSecondary
                                            )
                                        }
                                        if (alreadyInPlaylist) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Sudah ada",
                                                    tint = glassConfig.accentSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Sudah ada",
                                                    style = GlassTypography.labelSmall,
                                                    color = glassConfig.accentSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                                contentDescription = "Tambahkan",
                                                tint = glassConfig.accentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TrackOptionsStep.CREATE_PLAYLIST -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = { currentStep = TrackOptionsStep.PICK_PLAYLIST },
                            size = 36.dp,
                            iconSize = 18.dp,
                            contentDescription = "Back"
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Playlist Baru",
                            style = GlassTypography.headlineMedium,
                            color = glassConfig.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(glassConfig.inputSurfaceColor)
                            .border(1.dp, glassConfig.borderBrush, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (newPlaylistName.isEmpty()) {
                            Text(
                                text = "Nama playlist...",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(glassConfig.inputSurfaceColor)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            text = "Batal",
                            onClick = { currentStep = TrackOptionsStep.PICK_PLAYLIST },
                            modifier = Modifier.weight(1f)
                        )
                        GlassButton(
                            text = "Buat & Simpan",
                            isPrimary = true,
                            onClick = {
                                val trimmedName = newPlaylistName.trim()
                                if (trimmedName.isNotEmpty()) {
                                    onCreatePlaylistAndAdd(trimmedName, newPlaylistDesc.trim())
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackOptionActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    titleColor: Color? = null,
    trailingChevron: Boolean = false,
    onClick: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    GlassCard(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = GlassTypography.titleMedium,
                    color = titleColor ?: glassConfig.textPrimary
                )
                Text(
                    text = subtitle,
                    style = GlassTypography.labelSmall,
                    color = glassConfig.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingChevron) {
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
