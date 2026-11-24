package com.masterwave.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Track
import com.masterwave.player.ui.theme.SwissMono

/**
 * Swiss Editorial track row with mathematical hairline rule,
 * crisp architectural corner geometry, and high-contrast typography hierarchy.
 */
@Composable
fun TrackItem(
    track: Track,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val thumbBg = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isPlaying) accent.copy(alpha = 0.12f) else Color.Transparent)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                    }
                }
                .bouncyClickable(scaleDown = 0.98f, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(thumbBg)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val artworkModel = androidx.compose.runtime.remember(track.artworkUrl, track.streamUrl) {
                    val url = track.artworkUrl
                    when {
                        url.isNullOrBlank() -> null
                        url.startsWith("http://") || url.startsWith("https://") -> url
                        url.startsWith("content://") || url.startsWith("file://") -> android.net.Uri.parse(url)
                        else -> java.io.File(url)
                    }
                }

                if (artworkModel != null) {
                    AsyncImage(
                        model = artworkModel,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "track_equalizer")
                        val bar1 by infiniteTransition.animateFloat(
                            initialValue = 4f,
                            targetValue = 18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "b1"
                        )
                        val bar2 by infiniteTransition.animateFloat(
                            initialValue = 16f,
                            targetValue = 6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(550, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "b2"
                        )
                        val bar3 by infiniteTransition.animateFloat(
                            initialValue = 8f,
                            targetValue = 20f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(480, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "b3"
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(18.dp)
                        ) {
                            EqualizerBar(bar1, Color.White)
                            EqualizerBar(bar2, Color.White)
                            EqualizerBar(bar3, Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = when {
                        isPlaying -> accent
                        !track.isPlayable -> textSecondary.copy(alpha = 0.55f)
                        else -> textPrimary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.providerId == "local") {
                        Text(
                            text = "LOCAL",
                            style = SwissMono.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = accent,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = track.artist + if (track.album.isNotBlank()) " • ${track.album}" else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            letterSpacing = (-0.1).sp
                        ),
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onFavoriteToggle != null) {
                val animatedHeartColor by animateColorAsState(
                    targetValue = if (isFavorite) accent else textSecondary.copy(alpha = 0.55f),
                    label = "track_fav_color"
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .bouncyClickable(scaleDown = 0.88f, onClick = onFavoriteToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = animatedHeartColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 70.dp, end = 10.dp),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.06f)
        )
    }
}

@Composable
private fun EqualizerBar(heightDp: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(color)
    )
}
