package com.glassroom.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassroom.music.domain.model.Lyrics
import com.glassroom.music.ui.components.GlassButton
import com.glassroom.music.ui.theme.GlassAccentCobalt
import com.glassroom.music.ui.theme.GlassAccentMint
import com.glassroom.music.ui.theme.GlassTypography
import com.glassroom.music.ui.theme.LocalGlassConfig

@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean = true,
    color: Color = GlassAccentMint,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_bars")
    val height1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_3"
    )

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) height1.dp else 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) height2.dp else 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) height3.dp else 5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

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
            .height(350.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = if (glassConfig.isDark) Color(0x668B5CF6) else Color(0x2A38BDF8),
                spotColor = if (glassConfig.isDark) Color(0x4D06B6D4) else Color(0x1F818CF8)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(brush = glassConfig.cardBrush)
            .border(
                width = 1.5.dp,
                brush = glassConfig.borderBrush,
                shape = RoundedCornerShape(32.dp)
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
                        modifier = Modifier.size(42.dp),
                        color = GlassAccentCobalt,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                            .size(56.dp)
                            .shadow(8.dp, CircleShape, spotColor = Color(0x3338BDF8))
                            .clip(CircleShape)
                            .background(if (glassConfig.isDark) Color(0x33FFFFFF) else Color(0x200F172A))
                            .border(1.dp, Color(0x4DFFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = glassConfig.textTertiary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
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
                // Synced Lyrics with Bouncy Pop-Up Karaoke Running Animation
                val listState = rememberLazyListState()

                // Find active line index based on current playback timestamp
                val activeLineIndex = remember(lyrics.lines, currentPositionMs) {
                    val idx = lyrics.lines.indexOfLast { it.timestampMs <= currentPositionMs }
                    idx.coerceAtLeast(0)
                }

                // Smooth auto-scroll to keep active line centered
                LaunchedEffect(activeLineIndex) {
                    if (activeLineIndex in lyrics.lines.indices) {
                        listState.animateScrollToItem(
                            index = (activeLineIndex - 1).coerceAtLeast(0),
                            scrollOffset = 0
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header badge with live sound wave indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (glassConfig.isDark) Color(0x2610B981) else Color(0x1F10B981))
                                .border(1.dp, Color(0x4D10B981), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GlassAccentMint)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE KARAOKE SYNC",
                                style = GlassTypography.labelSmall,
                                color = GlassAccentMint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
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
                        contentPadding = PaddingValues(vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(lyrics.lines) { index, line ->
                            val isActive = index == activeLineIndex

                            // Spring Bouncy Pop-Up Scale for the currently sung line
                            val popScale by animateFloatAsState(
                                targetValue = if (isActive) 1.05f else 0.94f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "lyric_pop_scale"
                            )

                            // Animated opacity transition
                            val opacity by animateFloatAsState(
                                targetValue = if (isActive) 1.0f else 0.40f,
                                animationSpec = tween(durationMillis = 300),
                                label = "lyric_opacity"
                            )

                            val textColor = when {
                                isActive -> if (glassConfig.isDark) Color.White else Color(0xFF0F172A)
                                else -> if (glassConfig.isDark) Color(0x88FFFFFF) else Color(0x660F172A)
                            }

                            val activePillBrush = if (glassConfig.isDark) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x3806B6D4),
                                        Color(0x298B5CF6),
                                        Color(0x3310B981)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x3338BDF8),
                                        Color(0x22818CF8),
                                        Color(0x2B34D399)
                                    )
                                )
                            }

                            val activeBorderBrush = if (glassConfig.isDark) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x9906B6D4),
                                        Color(0x808B5CF6),
                                        Color(0x99FFFFFF)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x9938BDF8),
                                        Color(0x80818CF8),
                                        Color(0xB3FFFFFF)
                                    )
                                )
                            }

                            // Lyric item container with dynamic Pop-Up animation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(popScale)
                                    .then(
                                        if (isActive) {
                                            Modifier
                                                .shadow(
                                                    elevation = 14.dp,
                                                    shape = RoundedCornerShape(18.dp),
                                                    ambientColor = if (glassConfig.isDark) Color(0x4D06B6D4) else Color(0x2638BDF8),
                                                    spotColor = if (glassConfig.isDark) Color(0x668B5CF6) else Color(0x2B818CF8)
                                                )
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(brush = activePillBrush)
                                                .border(
                                                    width = 1.5.dp,
                                                    brush = activeBorderBrush,
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                        } else {
                                            Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                        }
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSeek(line.timestampMs)
                                    }
                                    .padding(
                                        vertical = if (isActive) 12.dp else 8.dp,
                                        horizontal = if (isActive) 14.dp else 8.dp
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Animated Equalizer Bars popping up exclusively on the currently singing line
                                    AnimatedVisibility(
                                        visible = isActive,
                                        enter = fadeIn(tween(250)),
                                        exit = fadeOut(tween(200))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            MiniEqualizerBars(
                                                isPlaying = true,
                                                color = if (glassConfig.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                    }

                                    Text(
                                        text = line.text,
                                        color = textColor.copy(alpha = opacity),
                                        fontSize = if (isActive) 20.sp else 16.sp,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                        lineHeight = if (isActive) 28.sp else 22.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
