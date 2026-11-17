package com.masterwave.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * APPLE MUSIC-INSPIRED FLOATING LIQUID GLASS MINI-PLAYER
 * Belongs to the same optical material family as the Dock:
 * - Floating compact capsule resting above the navigation dock.
 * - Layered translucency with curved specular reflex sheen.
 * - Embedded optical laser groove progress line.
 * - Micro-beveled album artwork squircle.
 * - Live 3-bar animated audio visualizer.
 * - Prominent circular Liquid Glass play/pause jewel button.
 * - Tactile next-track glass control.
 * - Smooth swipe-to-skip gesture with expansion on tap.
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
    val accentColor by ThemeState.effectiveAccentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mini_accent"
    )

    val textPrimary = if (isAmoled) Color.White else Color(0xFF141722)
    val textSecondary = if (isAmoled) Color(0xFFA2A7B8) else Color(0xFF5A6072)

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
        // Floating Optical Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassySurface(
                    shape = RoundedCornerShape(26.dp),
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.2.dp,
                    elevation = 12.dp,
                    accentGlow = animatedAccent.copy(alpha = 0.16f),
                    hasSheen = true,
                    onClick = onExpand
                )
        ) {
            Column {
                // Top rim optical laser groove progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(if (isAmoled) Color(0x18FFFFFF) else Color(0x12000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        animatedAccent.copy(alpha = 0.55f),
                                        animatedAccent,
                                        Color.White.copy(alpha = 0.95f)
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
                    // Artwork Thumbnail with directional micro-bevel rim
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
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.60f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ),
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
                                            .background(animatedAccent)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar2Height.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(animatedAccent)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar3Height.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(animatedAccent)
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

                    // Play / Pause Circular Liquid Glass Jewel Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                spotColor = animatedAccent.copy(alpha = 0.45f),
                                ambientColor = Color.Black.copy(alpha = 0.20f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        animatedAccent.copy(alpha = 0.92f),
                                        animatedAccent.copy(alpha = 0.72f)
                                    )
                                )
                            )
                            .drawWithContent {
                                drawContent()
                                // Top specular sheen
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = size.height * 0.45f
                                    ),
                                    size = Size(size.width, size.height * 0.45f)
                                )
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.80f),
                                        Color.White.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .bouncyClickable(scaleDown = 0.92f, onClick = onPlayPause),
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

                    // Next Track Clear Glass Slab Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
