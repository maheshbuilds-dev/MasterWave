package com.masterwave.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Supercool MasterWave Iconic Logo:
 * Harmonic M+W soundwave bars with neon acoustic gradient (Turquoise -> Violet -> Crimson)
 * and ambient acoustic ripple rings.
 */
@Composable
fun MasterWaveLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    
    val phase by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "phase"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Neon Gradient: Electric Turquoise to Neon Violet to Vivid Crimson
            val waveBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00F5D4), // Electric Turquoise
                    Color(0xFF8B5CF6), // Neon Violet
                    Color(0xFFFF2A55)  // Cyber Crimson
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            // Outer subtle acoustic aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.22f),
                        Color(0xFF00F5D4).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.52f
                )
            )

            // Geometric Soundwave Bars forming the iconic "M" and "W" waveform
            // 7 symmetrical acoustic columns
            val barCount = 7
            val barWidth = w * 0.082f
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            val gap = (w - (barCount * barWidth)) / (barCount + 1)

            // Dynamic heights animated smoothly with phase
            val baseHeights = listOf(0.35f, 0.65f, 0.90f, 0.45f, 0.90f, 0.65f, 0.35f)
            val waveHeights = listOf(0.45f, 0.85f, 0.60f, 0.95f, 0.60f, 0.85f, 0.45f)

            for (i in 0 until barCount) {
                val factor = baseHeights[i] + (waveHeights[i] - baseHeights[i]) * phase
                val barHeight = h * factor * 0.76f
                val x = gap + i * (barWidth + gap)
                val y = (h - barHeight) / 2f

                drawRoundRect(
                    brush = waveBrush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }

            // Top specular sound crest arch
            val crestPath = Path().apply {
                moveTo(gap + barWidth / 2f, h * 0.5f)
                cubicTo(
                    w * 0.25f, h * (0.15f + 0.1f * phase),
                    w * 0.75f, h * (0.85f - 0.1f * phase),
                    w - gap - barWidth / 2f, h * 0.5f
                )
            }

            drawPath(
                path = crestPath,
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(
                    width = w * 0.025f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
