package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import io.github.fletchmckee.liquid.LiquidState
import java.io.File

/**
 * APPLE MUSIC-INSPIRED FLOATING LIQUID GLASS MINI-PLAYER
 * Phase 6 Refinements:
 * - Separate, distinct glass object that complements the dock with an 8dp air gap.
 * - Calibrated density preventing underlying artwork and text from bleeding through.
 * - Embedded 2dp optical laser groove progress line.
 * - Micro-beveled album artwork squircle.
 * - Live 3-bar animated audio visualizer.
 * - Circular Liquid Glass play/pause jewel button with dynamic accent lens.
 * - Tactile next-track glass control.
 * - Horizontal swipe-to-skip gesture and full tap expansion into Now Playing.
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
    modifier: Modifier = Modifier,
    liquidState: LiquidState? = null,
    isGlassEnabled: Boolean = true,
    isCompact: Boolean = false
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "mini_progress")
    val accentColor by ThemeState.effectiveAccentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val compactSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )
    val outerPadding by animateDpAsState(
        targetValue = if (isCompact) 26.dp else 14.dp,
        animationSpec = compactSpring,
        label = "mini_outer_padding"
    )
    val artworkSize by animateDpAsState(
        targetValue = if (isCompact) 36.dp else 44.dp,
        animationSpec = compactSpring,
        label = "mini_artwork"
    )
    val playSize by animateDpAsState(
        targetValue = if (isCompact) 36.dp else 42.dp,
        animationSpec = compactSpring,
        label = "mini_play"
    )
    val nextSize by animateDpAsState(
        targetValue = if (isCompact) 32.dp else 38.dp,
        animationSpec = compactSpring,
        label = "mini_next"
    )
    val rowVertical by animateDpAsState(
        targetValue = if (isCompact) 4.5.dp else 7.dp,
        animationSpec = compactSpring,
        label = "mini_row_vertical"
    )

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
        initialValue = 4f, targetValue = 13f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 11f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    var totalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerPadding),
        contentAlignment = Alignment.Center
    ) {
        // Floating Optical Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .glassySurface(
                    shape = RoundedCornerShape(24.dp),
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.3.dp,
                    elevation = 12.dp,
                    accentGlow = animatedAccent.copy(alpha = 0.22f),
                    hasSheen = true,
                    tintColor = animatedAccent,
                    liquidState = liquidState,
                    isGlassEnabled = isGlassEnabled,
                    onClick = onExpand
                )
        ) {
            Column {
                // Top rim optical laser groove progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isAmoled) Color(0x18FFFFFF) else Color(0x10000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        animatedAccent.copy(alpha = 0.50f),
                                        animatedAccent,
                                        Color.White.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 10.dp, top = rowVertical, bottom = rowVertical),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork Thumbnail with directional micro-bevel rim & soft elevation
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
                            .size(artworkSize)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color.Black.copy(alpha = if (isAmoled) 0.45f else 0.12f),
                                ambientColor = Color.Black.copy(alpha = if (isAmoled) 0.30f else 0.04f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color.Black.copy(alpha = 0.08f)
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
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title, Artist, & Live Animated Visualizer
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onExpand)
                    ) {
                        Text(
                            text = track.title,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isCompact) 14.sp else 15.sp,
                            letterSpacing = (-0.3).sp,
                            color = if (isAmoled) Color.White else Color(0xFF0F1419),
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
                                        .height(13.dp)
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
                                fontSize = if (isCompact) 12.sp else 13.sp,
                                color = if (isAmoled) Color(0xFFA2A7B8) else Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play / Pause Circular Liquid Glass Jewel Button (Apple Music Signature)
                    val playColor = if (accentColor != Color(0xFFE53935) && accentColor != Color.Transparent) {
                        animatedAccent
                    } else {
                        if (isAmoled) Color(0xFF2B557C) else Color(0xFF244866)
                    }
                    val playJewelGradient = Brush.verticalGradient(
                        listOf(
                            playColor,
                            playColor.copy(alpha = 0.90f),
                            playColor.copy(alpha = 0.78f)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(playSize)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                spotColor = animatedAccent.copy(alpha = 0.35f),
                                ambientColor = Color.Black.copy(alpha = 0.20f)
                            )
                            .clip(CircleShape)
                            .background(playJewelGradient)
                            .drawWithContent {
                                drawContent()
                                // Optical gloss reflex across top 45%
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = size.height * 0.45f
                                    ),
                                    size = Size(size.width, size.height * 0.45f)
                                )
                            }
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .bouncyClickable(scaleDown = 0.94f, onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = Pair(isLoading, isPlaying),
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(130)))
                                    .togetherWith(scaleOut(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(90)))
                            },
                            label = "mini_play_pause_state"
                        ) { (loading, playing) ->
                            if (loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(if (isCompact) 18.dp else 20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isCompact) 21.dp else 24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Next Track Circular Frosted Lens Button
                    Box(
                        modifier = Modifier
                            .size(nextSize)
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = if (isAmoled) 0.35f else 0.08f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isAmoled) Color(0x28FFFFFF) else Color(0xFFEFF0F4)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        if (isAmoled) Color(0x40FFFFFF) else Color.White,
                                        if (isAmoled) Color(0x10FFFFFF) else Color(0x12000000)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .bouncyClickable(scaleDown = 0.93f, onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = if (isAmoled) Color.White else Color(0xFF1D1F28),
                            modifier = Modifier.size(if (isCompact) 17.dp else 20.dp)
                        )
                    }
                }
            }
        }
    }
}
