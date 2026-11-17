package com.masterwave.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material Lightning & Electric Fluid Waveform Seekbar:
 * - Dynamic oscillating electric sine waves with harmonic micro-arcs.
 * - Glowing thumb knob with radiant corona.
 * - Interactive dragging & tap-to-seek.
 * - Apple Music style timestamps.
 */
@Composable
fun LightningWaveform(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    waveColor: Color,
    codecLabel: String = "Opus • 160 kbps",
    onSeek: (Long) -> Unit,
    onCodecClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val displayProgress = if (isDragging) dragProgress else currentProgress

    // Electric Lightning Oscillation animation
    val infiniteTransition = rememberInfiniteTransition(label = "lightning_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val sparkPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spark"
    )

    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    val elapsedText = formatTime(if (isDragging) (dragProgress * durationMs).toLong() else positionMs)
    val remainingMs = (durationMs - (if (isDragging) (dragProgress * durationMs).toLong() else positionMs)).coerceAtLeast(0L)
    val remainingText = "-${formatTime(remainingMs)}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Lightning Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((newProgress * durationMs).toLong())
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgress * durationMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragProgress = (dragProgress + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val activeX = (width * displayProgress).coerceIn(0f, width)

                // 1. Ahead / Unplayed Track: Subtle undulating harmonic wave
                val unplayedPath = Path()
                unplayedPath.moveTo(activeX, midY)
                val unplayedStep = 4f
                var ux = activeX
                while (ux <= width) {
                    val progressNorm = ux / width
                    val unplayedAmp = if (isPlaying) {
                        (1.8f * sin((progressNorm * 24f + phase * 0.6f).toDouble())).toFloat()
                    } else {
                        (1.2f * sin((progressNorm * 18f).toDouble())).toFloat()
                    }
                    unplayedPath.lineTo(ux, midY + unplayedAmp)
                    ux += unplayedStep
                }
                unplayedPath.lineTo(width, midY)

                // Draw unplayed subtle wave
                drawPath(
                    path = unplayedPath,
                    color = Color(0x35FFFFFF),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 2. Active / Played Electric Lightning Wave (Dynamic undulating fluid spark wave)
                if (activeX > 2f) {
                    val activePath = Path()
                    val activeStep = 3f

                    activePath.moveTo(0f, midY)

                    var ax = 0f
                    while (ax <= activeX) {
                        val progressNorm = ax / width
                        // Multi-harmonic electric lightning undulation
                        val amp = if (isPlaying) {
                            ((3.8f * sin((progressNorm * 26f + phase).toDouble()) +
                              2.2f * sin((progressNorm * 54f - phase * 1.5f).toDouble()) +
                              1.2f * cos((progressNorm * 82f + phase * 2.2f).toDouble())) * sparkPulse).toFloat()
                        } else {
                            (1.8f * sin((progressNorm * 16f).toDouble())).toFloat()
                        }
                        val y = midY + amp
                        activePath.lineTo(ax, y)
                        ax += activeStep
                    }
                    activePath.lineTo(activeX, midY)

                    // Outer electric aura glow
                    drawPath(
                        path = activePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                waveColor.copy(alpha = 0.40f),
                                Color(0xFFFF8A80).copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.85f),
                                waveColor
                            )
                        ),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Secondary intense neon lightning stroke
                    drawPath(
                        path = activePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                waveColor.copy(alpha = 0.90f),
                                Color(0xFFFFD180),
                                Color.White,
                                waveColor
                            )
                        ),
                        style = Stroke(
                            width = 3.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Inner brilliant white core filament
                    drawPath(
                        path = activePath,
                        color = Color.White.copy(alpha = 0.95f),
                        style = Stroke(
                            width = 1.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 3. Electric Spark Knob at active playback position
                val knobX = activeX.coerceIn(0f, width)
                val knobCenter = Offset(knobX, midY)

                // Large radiating spark aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            waveColor.copy(alpha = 0.60f),
                            Color.Transparent
                        ),
                        center = knobCenter,
                        radius = 16.dp.toPx() * sparkPulse
                    ),
                    radius = 16.dp.toPx() * sparkPulse,
                    center = knobCenter
                )

                // Outer tactile ring
                drawCircle(
                    color = waveColor,
                    radius = if (isDragging) 8.dp.toPx() else 6.5.dp.toPx(),
                    center = knobCenter
                )

                // Inner bright white spark core
                drawCircle(
                    color = Color.White,
                    radius = if (isDragging) 4.5.dp.toPx() else 3.5.dp.toPx(),
                    center = knobCenter
                )
            }
        }

        // Timestamps & Codec Pill Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                color = Color(0xCCFFFFFF)
            )

            // Centered Codec Pill (Frame 0.0s, 2.5s, 7.5s, 20.0s)
            Surface(
                shape = CircleShape,
                color = Color(0x22FFFFFF),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onCodecClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = codecLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xDDFFFFFF)
                    )
                }
            }

            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                color = Color(0xCCFFFFFF)
            )
        }
    }
}
