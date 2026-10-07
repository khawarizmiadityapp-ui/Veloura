package com.glassroom.music.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glassroom.music.domain.model.Track
import com.glassroom.music.playback.PlayerManager
import com.glassroom.music.ui.components.GlassButton
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
    onTrackSelect: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF7FFFFFF),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x330F172A))
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
                        text = "Play Queue",
                        style = GlassTypography.headlineMedium,
                        color = GlassTextPrimary
                    )
                    Text(
                        text = "${queue.size} songs in line",
                        style = GlassTypography.labelSmall,
                        color = GlassTextTertiary
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
                    text = "Now Playing",
                    style = GlassTypography.titleMedium,
                    color = GlassTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                GlassSongCard(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Up Next Section
            Text(
                text = "Up Next",
                style = GlassTypography.titleMedium,
                color = GlassTextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (queue.isEmpty() || queue.size == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No more songs in queue",
                        style = GlassTypography.bodyMedium,
                        color = GlassTextTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queue) { index, track ->
                        if (index != currentIndex) {
                            GlassSongCard(
                                track = track,
                                isPlaying = false,
                                onClick = { onTrackSelect(index) },
                                onMoreClick = { onRemoveFromQueue(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}
