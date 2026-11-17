package com.masterwave.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Pixel-perfect LastWave Fluid Wavy Seekbar matching Reference Frame 0.0s & 5.0s:
 * - Multi-layer fluid wavy progress slider with animated continuous sine wave oscillations.
 * - Active played portion rendered with radiant glowing accent tint.
 * - Inactive unplayed portion rendered with soft translucent wave.
 * - Circular thumb orb with radiant aura.
 * - Elapsed & Remaining timestamps in monospaced/clean typography.
 * - Centered Audio codec badge (e.g. "• Opus • OPUS 132 kbps >").
 */
@Composable
fun LastWaveWavySeekbar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    waveColor: Color,
    codecLabel: String = "• Opus • OPUS 132 kbps >",
    onSeek: (Long) -> Unit,
    onCodecClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val activeProgress = if (isDragging) dragProgress else (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    // Animated sine wave phase
    val infiniteTransition = rememberInfiniteTransition(label = "wave_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 3200 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Interactive Wavy Canvas ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((newProgress * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgress * safeDuration).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val thumbX = (w * activeProgress).coerceIn(0f, w)

                val wavelength = 42.dp.toPx()
                val waveAmplitude = if (isPlaying || isDragging) 4.5.dp.toPx() else 2.5.dp.toPx()

                // 1. Draw Played Section (Wavy Path with radiant accent)
                val playedPath = Path()
                var startedPlayed = false
                var x = 0f
                val step = 3f

                while (x <= thumbX) {
                    val y = midY + waveAmplitude * sin((2 * Math.PI * (x / wavelength) - phase)).toFloat()
                    if (!startedPlayed) {
                        playedPath.moveTo(x, y)
                        startedPlayed = true
                    } else {
                        playedPath.lineTo(x, y)
                    }
                    x += step
                }
                if (startedPlayed && x > thumbX) {
                    val finalY = midY + waveAmplitude * sin((2 * Math.PI * (thumbX / wavelength) - phase)).toFloat()
                    playedPath.lineTo(thumbX, finalY)
                }

                if (startedPlayed) {
                    // Soft glow underneath
                    drawPath(
                        path = playedPath,
                        color = waveColor.copy(alpha = 0.35f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Crisp core line
                    drawPath(
                        path = playedPath,
                        color = waveColor,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 2. Draw Unplayed Section (Subtle muted wave)
                val unplayedPath = Path()
                var startedUnplayed = false
                var ux = thumbX
                while (ux <= w) {
                    val y = midY + (waveAmplitude * 0.7f) * sin((2 * Math.PI * (ux / wavelength) - phase)).toFloat()
                    if (!startedUnplayed) {
                        unplayedPath.moveTo(ux, y)
                        startedUnplayed = true
                    } else {
                        unplayedPath.lineTo(ux, y)
                    }
                    ux += step
                }

                if (startedUnplayed) {
                    drawPath(
                        path = unplayedPath,
                        color = Color.White.copy(alpha = 0.22f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 3. Thumb Orb (Glowing circle exactly at thumbX)
                val thumbY = midY + waveAmplitude * sin((2 * Math.PI * (thumbX / wavelength) - phase)).toFloat()

                // Outer halo
                drawCircle(
                    color = waveColor.copy(alpha = 0.32f),
                    radius = if (isDragging) 12.dp.toPx() else 9.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
                // Mid ring
                drawCircle(
                    color = Color.White,
                    radius = if (isDragging) 6.dp.toPx() else 4.8.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
                // Inner core
                drawCircle(
                    color = waveColor,
                    radius = if (isDragging) 4.dp.toPx() else 3.2.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }
        }

        // --- 2. Timestamps and Audio Quality Chip (Frame 0.0s) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elapsed Time
            val currentMs = if (isDragging) (dragProgress * safeDuration).toLong() else positionMs
            Text(
                text = formatDuration(currentMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White.copy(alpha = 0.82f)
            )

            // Centered Codec & Quality Pill (LastWave Reference: "• Opus • OPUS 132 kbps >")
            Surface(
                shape = CircleShape,
                color = Color(0x35000000),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x30FFFFFF)),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onCodecClick)
            ) {
                Text(
                    text = codecLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                )
            }

            // Remaining Time (with minus sign)
            val remainingMs = (durationMs - currentMs).coerceAtLeast(0L)
            Text(
                text = "-${formatDuration(remainingMs)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
