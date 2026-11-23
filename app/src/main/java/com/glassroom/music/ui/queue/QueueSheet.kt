package com.glassroom.music.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.components.GlassCard
import com.glassroom.music.ui.components.GlassChip
import com.glassroom.music.ui.components.GlassIconButton
import com.glassroom.music.ui.components.GlassSongCard
import com.glassroom.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Track>,
    currentIndex: Int,
    currentTrack: Track?,
    isPlaying: Boolean,
    isShuffle: Boolean = false,
    shuffleOrder: List<Int> = emptyList(),
    onTrackSelect: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit,
    onReorderQueue: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glassConfig = LocalGlassConfig.current
    var isReordering by remember { mutableStateOf(false) }

    val upcomingEntries = remember(queue, currentIndex, isShuffle, shuffleOrder) {
        if (isShuffle && shuffleOrder.size == queue.size) {
            val pos = shuffleOrder.indexOf(currentIndex)
            val upcomingIndices = if (pos >= 0) shuffleOrder.drop(pos + 1) else shuffleOrder
            upcomingIndices.mapNotNull { idx ->
                queue.getOrNull(idx)?.let { track -> idx to track }
            }
        } else {
            queue.mapIndexedNotNull { index, track ->
                if (index > currentIndex) index to track else null
            }
        }
    }

    LaunchedEffect(isShuffle) {
        if (isShuffle && isReordering) {
            isReordering = false
        }
    }

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
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Antrean Putar",
                        style = GlassTypography.headlineMedium,
                        color = glassConfig.textPrimary
                    )
                    Text(
                        text = "${queue.size} lagu di antrean",
                        style = GlassTypography.labelSmall,
                        color = glassConfig.textTertiary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (queue.isNotEmpty()) {
                        GlassIconButton(
                            icon = Icons.Filled.DeleteSweep,
                            onClick = onClearQueue,
                            size = 38.dp,
                            iconSize = 18.dp,
                            contentDescription = "Clear Queue"
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Filled.Close,
                        onClick = onDismiss,
                        size = 38.dp,
                        iconSize = 18.dp,
                        contentDescription = "Close"
                    )
                }
            }

            // Now Playing Section
            if (currentTrack != null) {
                Text(
                    text = "Sedang Diputar",
                    style = GlassTypography.titleMedium,
                    color = glassConfig.textSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                GlassSongCard(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Up Next Header + Reorder Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Berikutnya (${upcomingEntries.size})",
                    style = GlassTypography.titleMedium,
                    color = glassConfig.textSecondary
                )
                if (isShuffle && upcomingEntries.isNotEmpty()) {
                    Text(
                        text = "Urutan acak aktif",
                        style = GlassTypography.labelSmall,
                        color = glassConfig.accentPrimary
                    )
                } else if (!isShuffle && upcomingEntries.size > 1) {
                    GlassChip(
                        text = if (isReordering) "Selesai" else "Atur Urutan",
                        isSelected = isReordering,
                        onClick = { isReordering = !isReordering }
                    )
                }
            }

            if (queue.isEmpty() || queue.size == 1 || upcomingEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (queue.size <= 1) "Tidak ada lagu berikutnya di antrean" else "Akhir antrean tercapai",
                        style = GlassTypography.bodyMedium,
                        color = glassConfig.textTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(
                        items = upcomingEntries,
                        key = { offset, (actualIndex, track) -> "queue_${actualIndex}_${track.id}_$offset" }
                    ) { offset, (actualIndex, track) ->
                        QueueUpcomingRow(
                            track = track,
                            isReordering = isReordering && !isShuffle,
                            canMoveUp = offset > 0,
                            canMoveDown = offset < upcomingEntries.lastIndex,
                            onSelect = { onTrackSelect(actualIndex) },
                            onMoveUp = { onReorderQueue(actualIndex, actualIndex - 1) },
                            onMoveDown = { onReorderQueue(actualIndex, actualIndex + 1) },
                            onRemove = { onRemoveFromQueue(actualIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueUpcomingRow(
    track: Track,
    isReordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val glassConfig = LocalGlassConfig.current

    GlassCard(
        onClick = if (isReordering) null else onSelect,
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

            if (track.duration > 0 && !isReordering) {
                Text(
                    text = track.durationFormatted,
                    style = GlassTypography.labelSmall,
                    color = glassConfig.textTertiary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            if (isReordering) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Hapus dari antrean",
                    tint = glassConfig.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
