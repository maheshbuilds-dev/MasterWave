package com.masterwave.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Track
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState
import java.io.File

/**
 * MASTERWAVE SONIC MINI-PLAYER DOCK
 * Engineered with:
 * - Fluid refractive acrylic glass dock with specular top rim
 * - Animated continuous playback progress line
 * - Live dynamic 3-bar audio visualizer when playing
 * - Swipe-left gesture to skip to next track
 * - Tactile 48dp spring button for playback toggle
 */
@Composable
fun MiniPlayerDock(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "mini_progress")
    val brandTurquoise = com.masterwave.player.ui.theme.MasterWaveTurquoise
    val isAmoled = ThemeState.isAmoledTheme()

    val textPrimary = if (isAmoled) Color.White else Color(0xFF14171E)
    val textSecondary = if (isAmoled) Color(0xFFA0A5B8) else Color(0xFF6B7285)

    // Live audio wave equalizer animation
    val infiniteTransition = rememberInfiniteTransition(label = "mini_eq")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    var totalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag < -60f) {
                            onNext()
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassySurface(
                    shape = RoundedCornerShape(24.dp),
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.dp,
                    elevation = 12.dp,
                    hasSheen = true,
                    onClick = onExpand
                )
        ) {
            Column {
                // Top rim glowing playback progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isAmoled) Color(0x15FFFFFF) else Color(0x10000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        brandTurquoise.copy(alpha = 0.6f),
                                        brandTurquoise,
                                        Color.White.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork Thumbnail with specular rim
                    val artworkModel = remember(track.artworkUrl) {
                        val u = track.artworkUrl
                        when {
                            u.isNullOrBlank() -> null
                            u.startsWith("http://") || u.startsWith("https://") -> u
                            u.startsWith("content://") -> android.net.Uri.parse(u)
                            u.startsWith("file://") -> android.net.Uri.parse(u)
                            else -> File(u)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 0.8.dp,
                                color = if (isAmoled) Color(0x30FFFFFF) else Color(0x15000000),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(if (isAmoled) Color(0xFF1E202B) else Color(0xFFE9ECF2))
                            .clickable(onClick = onExpand),
                        contentAlignment = Alignment.Center
                    ) {
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title, Artist, & Live Animated Visualizer
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onExpand)
                    ) {
                        Text(
                            text = track.title,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            letterSpacing = (-0.2).sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPlaying) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier
                                        .height(14.dp)
                                        .padding(end = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar1Height.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(brandTurquoise)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar2Height.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(brandTurquoise)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar3Height.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(brandTurquoise)
                                    )
                                }
                            }
                            Text(
                                text = track.artist,
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play / Pause Prominent Glassy Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .glassySurface(
                                shape = CircleShape,
                                style = GlassyStyle.PROMINENT,
                                tintColor = brandTurquoise,
                                elevation = 4.dp,
                                onClick = onPlayPause
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Next Track Frosted Glass Slab Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .glassySurface(
                                shape = CircleShape,
                                style = GlassyStyle.SLAB,
                                isDark = isAmoled,
                                elevation = 2.dp,
                                onClick = onNext
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
