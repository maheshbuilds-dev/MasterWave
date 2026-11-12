package com.masterwave.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.DarkBorder
import com.masterwave.player.ui.theme.DarkPrimaryCrimson
import com.masterwave.player.ui.theme.DarkTextSecondary
import com.masterwave.player.ui.theme.DarkTextTertiary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated Waveform Seekbar matching Screenshot 5.
 * Features:
 * - Fluid sine wave harmonic synthesis when playing.
 * - Interactive touch drag and tap seeking.
 * - Glowing thumb with pink/salmon accent.
 * - Opus audio codec quality pill in the center.
 * - Elapsed and remaining (-mm:ss) timestamp readouts.
 */
@Composable
fun AnimatedWaveform(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    codecLabel: String = "Opus • OPUS 160 kbps",
    onCodecClick: () -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val naturalProgress = (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    val displayProgress = if (isDragging) dragProgress else naturalProgress

    // Continuous wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Equalizer bars mini-animation for codec badge
    val eqBar1 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )
    val eqBar2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )
    val eqBar3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // Canvas waveform area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((newProgress * safeDuration).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
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
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val midY = canvasHeight / 2f
                val thumbX = canvasWidth * displayProgress

                // Inactive track (right of thumb)
                drawLine(
                    color = Color(0x33E53935),
                    start = Offset(thumbX.coerceAtLeast(0f), midY),
                    end = Offset(canvasWidth, midY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Active waveform (left of thumb)
                if (thumbX > 4f) {
                    val wavePath = Path()
                    val waveGlowPath = Path()
                    val numSteps = (thumbX / 3f).toInt().coerceAtLeast(10)
                    val dx = thumbX / numSteps

                    // Primary harmonic wave
                    wavePath.moveTo(0f, midY)
                    waveGlowPath.moveTo(0f, midY)

                    for (i in 0..numSteps) {
                        val x = i * dx
                        val progressNorm = (x / thumbX).coerceIn(0f, 1f)
                        // Envelope envelope window: 0 at edges, max in middle
                        val envelope = sin(progressNorm * PI.toFloat())
                        val motionFactor = if (isPlaying) 1f else 0.3f

                        val yOffset = if (thumbX > 20f) {
                            val f1 = sin((progressNorm * 5f * PI.toFloat()) - phase) * 7.dp.toPx()
                            val f2 = sin((progressNorm * 9f * PI.toFloat()) + (phase * 1.3f)) * 4.dp.toPx()
                            (f1 + f2) * envelope * motionFactor
                        } else {
                            0f
                        }

                        val y = midY + yOffset
                        if (i == 0) {
                            wavePath.moveTo(x, y)
                            waveGlowPath.moveTo(x, y)
                        } else {
                            wavePath.lineTo(x, y)
                            waveGlowPath.lineTo(x, y)
                        }
                    }

                    // Draw soft glow
                    drawPath(
                        path = waveGlowPath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0x55E53935),
                                Color(0x88FF7088),
                                Color(0xAAFFA4A4)
                            ),
                            startX = 0f,
                            endX = thumbX
                        ),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw crisp wave
                    drawPath(
                        path = wavePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFE53935),
                                Color(0xFFFF5252),
                                Color(0xFFFFA4A4)
                            ),
                            startX = 0f,
                            endX = thumbX
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Waveform Scrub Thumb (Glowing salmon pill circle)
                val thumbRadius = if (isDragging) 9.dp.toPx() else 7.dp.toPx()
                // Outer glow
                drawCircle(
                    color = Color(0x66FF8A80),
                    radius = thumbRadius + 5.dp.toPx(),
                    center = Offset(thumbX, midY)
                )
                // Inner solid thumb
                drawCircle(
                    color = Color(0xFFFFA4A4),
                    radius = thumbRadius,
                    center = Offset(thumbX, midY)
                )
                // Center accent
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = thumbRadius * 0.4f,
                    center = Offset(thumbX, midY)
                )
            }
        }

        // Time labels and Codec info badge row (Screenshot 5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elapsed time
            val elapsedMs = if (isDragging) (dragProgress * safeDuration).toLong() else positionMs
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = DarkTextSecondary
            )

            // Center: Audio Codec Badge with pulsing equalizer bars (Screenshot 5)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.clickable { onCodecClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    // Pulsing animated equalizer bars
                    Canvas(modifier = Modifier.size(width = 12.dp, height = 14.dp)) {
                        val barColor = Color(0xFFFFA4A4)
                        val h = size.height
                        val w = 2.dp.toPx()
                        // Bar 1
                        val b1 = (if (isPlaying) eqBar1 else 6f).dp.toPx().coerceIn(2f, h)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(0f, h - b1),
                            size = androidx.compose.ui.geometry.Size(w, b1)
                        )
                        // Bar 2
                        val b2 = (if (isPlaying) eqBar2 else 10f).dp.toPx().coerceIn(2f, h)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(4.dp.toPx(), h - b2),
                            size = androidx.compose.ui.geometry.Size(w, b2)
                        )
                        // Bar 3
                        val b3 = (if (isPlaying) eqBar3 else 4f).dp.toPx().coerceIn(2f, h)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(8.dp.toPx(), h - b3),
                            size = androidx.compose.ui.geometry.Size(w, b3)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = codecLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Audio settings",
                        tint = DarkTextTertiary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Remaining time (-mm:ss)
            val remainingMs = (durationMs - elapsedMs).coerceAtLeast(0L)
            Text(
                text = "-${formatDuration(remainingMs)}",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = DarkTextSecondary
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
