package com.masterwave.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Track
import com.masterwave.player.ui.theme.ThemeState

@Composable
fun TrackItem(
    track: Track,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)
    val thumbBg = if (isAmoled) Color(0xFF181822) else Color(0xFFEFECEF)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) accentColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(thumbBg),
            contentAlignment = Alignment.Center
        ) {
            if (!track.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(22.dp)
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
                        modifier = Modifier.height(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(bar1.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(accentColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(bar2.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(accentColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(bar3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(accentColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (isPlaying) accentColor else if (!track.isPlayable) textSecondary.copy(alpha = 0.6f) else textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.providerId != "youtube" && track.providerId.isNotBlank()) {
                    Text(
                        text = when (track.providerId) {
                            "audius" -> "AUDIUS"
                            "internet_archive" -> "ARCHIVE"
                            "jamendo" -> "JAMENDO"
                            "local" -> "LOCAL"
                            else -> track.providerId.uppercase()
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = accentColor,
                        modifier = Modifier.padding(end = 5.dp)
                    )
                }
                Text(
                    text = track.artist + if (track.album.isNotBlank()) " • ${track.album}" else "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Optional Favorite Button
        if (onFavoriteToggle != null) {
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) accentColor else textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
