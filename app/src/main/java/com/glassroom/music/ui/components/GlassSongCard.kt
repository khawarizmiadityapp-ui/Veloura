package com.glassroom.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassroom.music.domain.model.Track
import com.glassroom.music.ui.theme.*

@Composable
fun GlassSongCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    val glassConfig = LocalGlassConfig.current

    // Tactile spring press feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "song_card_press"
    )

    val cardBg = if (isPlaying) {
        if (glassConfig.isDark) {
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF263248),
                    Color(0xFF1B2332)
                )
            )
        } else {
            Brush.horizontalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFEFF6FF)
                )
            )
        }
    } else {
        glassConfig.cardBrush
    }

    val borderBrush = if (isPlaying) {
        Brush.verticalGradient(
            listOf(
                GlassAccentCobalt,
                GlassAccentCobalt.copy(alpha = 0.5f)
            )
        )
    } else {
        glassConfig.borderBrush
    }

    Box(
        modifier = modifier
            .scale(cardScale)
            .fillMaxWidth()
            .shadow(
                elevation = if (isPlaying) 6.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (glassConfig.isDark) Color(0x33000000) else Color(0x100F172A),
                spotColor = if (isPlaying) (if (glassConfig.isDark) Color(0x333B82F6) else Color(0x263B82F6)) else Color.Transparent
            )
            .clip(RoundedCornerShape(20.dp))
            .background(brush = cardBg)
            .border(
                width = if (isPlaying) 1.2.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Artwork Container with fallback placeholder
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            if (glassConfig.isDark) listOf(Color(0xFF222B3D), Color(0xFF171D29))
                            else listOf(Color(0xFFEDE9FE), Color(0xFFE2E8F0))
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        brush = if (glassConfig.isDark) Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF)))
                        else Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x20FFFFFF))),
                        shape = RoundedCornerShape(14.dp)
                    ),
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
                        tint = if (glassConfig.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.title,
                        style = GlassTypography.titleMedium,
                        color = if (isPlaying) GlassAccentCobalt else glassConfig.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Active Playing Equalizer Indicator
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = "Currently Playing",
                            tint = GlassAccentCobalt,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = GlassTypography.bodyMedium,
                    color = glassConfig.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            if (track.duration > 0) {
                Text(
                    text = track.durationFormatted,
                    style = GlassTypography.labelSmall,
                    color = glassConfig.textTertiary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            // Favorite Button with tactile bounce
            if (onFavoriteClick != null) {
                val favInteractionSource = remember { MutableInteractionSource() }
                val isFavPressed by favInteractionSource.collectIsPressedAsState()
                val favScale by animateFloatAsState(
                    targetValue = if (isFavPressed) 0.85f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "fav_scale"
                )

                val heartColor by animateColorAsState(
                    targetValue = if (track.isFavorite) GlassHeartActive else glassConfig.textTertiary,
                    animationSpec = tween(durationMillis = 200),
                    label = "heart_color"
                )

                Box(
                    modifier = Modifier
                        .scale(favScale)
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = favInteractionSource,
                            indication = null,
                            onClick = onFavoriteClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = heartColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // More Options Button
            if (onMoreClick != null) {
                val moreInteractionSource = remember { MutableInteractionSource() }
                val isMorePressed by moreInteractionSource.collectIsPressedAsState()
                val moreScale by animateFloatAsState(
                    targetValue = if (isMorePressed) 0.85f else 1f,
                    label = "more_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(moreScale)
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = moreInteractionSource,
                            indication = null,
                            onClick = onMoreClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = glassConfig.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
