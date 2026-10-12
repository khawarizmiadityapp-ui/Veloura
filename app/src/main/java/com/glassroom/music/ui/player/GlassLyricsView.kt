package com.glassroom.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.domain.model.Lyrics
import com.glassroom.music.ui.components.GlassButton
import com.glassroom.music.ui.theme.GlassAccentCobalt
import com.glassroom.music.ui.theme.GlassAccentMint
import com.glassroom.music.ui.theme.GlassTypography
import com.glassroom.music.ui.theme.LocalGlassConfig

@Composable
fun GlassLyricsView(
    lyrics: Lyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalGlassConfig.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = if (glassConfig.isDark) Color(0x668B5CF6) else Color(0x2A38BDF8),
                spotColor = if (glassConfig.isDark) Color(0x4D06B6D4) else Color(0x1F818CF8)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(brush = glassConfig.cardBrush)
            .border(
                width = 1.5.dp,
                brush = glassConfig.borderBrush,
                shape = RoundedCornerShape(28.dp)
            )
            .padding(16.dp)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(38.dp),
                        color = GlassAccentCobalt,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Mencari lirik lagu...",
                        style = GlassTypography.bodyMedium,
                        color = glassConfig.textSecondary
                    )
                }
            }

            lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainLyrics.isNullOrBlank()) -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (glassConfig.isDark) Color(0x33FFFFFF) else Color(0x200F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Lirik belum tersedia",
                        style = GlassTypography.titleMedium,
                        color = glassConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tidak dapat menemukan lirik untuk lagu ini",
                        style = GlassTypography.labelSmall,
                        color = glassConfig.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassButton(
                        text = "Coba Lagi",
                        onClick = onRetry
                    )
                }
            }

            lyrics.isSynced && lyrics.lines.isNotEmpty() -> {
                // Synced Lyrics with Live Auto-Scroll
                val listState = rememberLazyListState()

                // Find active line index
                val activeLineIndex = remember(lyrics.lines, currentPositionMs) {
                    val idx = lyrics.lines.indexOfLast { it.timestampMs <= currentPositionMs }
                    idx.coerceAtLeast(0)
                }

                // Smooth auto-scroll to active line
                LaunchedEffect(activeLineIndex) {
                    if (activeLineIndex in lyrics.lines.indices) {
                        listState.animateScrollToItem(
                            index = (activeLineIndex - 1).coerceAtLeast(0),
                            scrollOffset = 0
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GlassAccentMint)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE SYNCED LYRICS",
                                style = GlassTypography.labelSmall,
                                color = GlassAccentMint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Ketuk baris untuk melompat",
                            style = GlassTypography.labelSmall,
                            color = glassConfig.textTertiary,
                            fontSize = 10.sp
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp)
                    ) {
                        itemsIndexed(lyrics.lines) { index, line ->
                            val isActive = index == activeLineIndex
                            val scale by animateFloatAsState(
                                targetValue = if (isActive) 1.03f else 1.0f,
                                animationSpec = tween(200),
                                label = "lyric_scale"
                            )

                            val textColor = when {
                                isActive -> if (glassConfig.isDark) Color.White else Color(0xFF0F172A)
                                else -> if (glassConfig.isDark) Color(0x66FFFFFF) else Color(0x550F172A)
                            }

                            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            val fontSize = if (isActive) 20.sp else 16.sp

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSeek(line.timestampMs)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = line.text,
                                    color = textColor,
                                    fontSize = fontSize,
                                    fontWeight = fontWeight,
                                    lineHeight = (fontSize.value * 1.35).sp,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // Plain (Unsynced) Lyrics View
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GlassAccentCobalt)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PLAIN LYRICS",
                            style = GlassTypography.labelSmall,
                            color = GlassAccentCobalt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = lyrics.plainLyrics ?: "",
                        style = GlassTypography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            fontSize = 16.sp,
                            color = glassConfig.textPrimary
                        )
                    )
                }
            }
        }
    }
}
