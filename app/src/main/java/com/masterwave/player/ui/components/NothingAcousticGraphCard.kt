package com.masterwave.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.NothingDotBadge
import com.masterwave.player.ui.theme.NothingHeadlineSmall
import com.masterwave.player.ui.theme.NothingTechMono
import com.masterwave.player.ui.theme.ThemeState
import kotlin.math.sin

enum class AcousticPreset(val label: String, val gains: FloatArray) {
    FLAT("FLAT", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("BASS+", floatArrayOf(0.75f, 0.50f, 0.10f, 0.20f, 0.35f)),
    VOCAL("VOCAL", floatArrayOf(-0.15f, 0.20f, 0.70f, 0.45f, 0.10f)),
    DYNAMIC("DYNAMIC", floatArrayOf(0.60f, 0.25f, 0.40f, 0.55f, 0.65f)),
    ACOUSTIC("STAGE", floatArrayOf(0.30f, 0.40f, 0.50f, 0.60f, 0.75f))
}

/**
 * Nothing OS Acoustic Spectrum & Interactive Frequency Graph:
 * - Dotted gridlines with +12dB to -12dB scale.
 * - Live animated 16-band spectrum visualizer bouncing to music playback.
 * - Smooth Bézier frequency response curve with animated nodes.
 * - Practical 5-preset sound curve switchers with instant tactile feedback.
 * - Technical telemetry: OPUS 251, dynamic range, and active frequency band.
 */
@Composable
fun NothingAcousticGraphCard(
    player: MasterWavePlayer,
    modifier: Modifier = Modifier
) {
    val isPlaying by player.isPlaying.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val accentColor by ThemeState.accentColor.collectAsState()

    var activePreset by remember { mutableStateOf(AcousticPreset.DYNAMIC) }

    // Animate the 5 frequency band control nodes with spring physics
    val node0 by animateFloatAsState(targetValue = activePreset.gains[0], animationSpec = spring(), label = "node0")
    val node1 by animateFloatAsState(targetValue = activePreset.gains[1], animationSpec = spring(), label = "node1")
    val node2 by animateFloatAsState(targetValue = activePreset.gains[2], animationSpec = spring(), label = "node2")
    val node3 by animateFloatAsState(targetValue = activePreset.gains[3], animationSpec = spring(), label = "node3")
    val node4 by animateFloatAsState(targetValue = activePreset.gains[4], animationSpec = spring(), label = "node4")

    // Infinite transition for live audio wave oscillation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "audio_graph_osc")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val textPrimary = if (isAmoled) Color.White else Color(0xFF0F0F14)
    val textSecondary = if (isAmoled) Color(0xFF888898) else Color(0xFF6B6B7B)
    val gridLineColor = if (isAmoled) Color(0x14FFFFFF) else Color(0x0E000000)
    val nothingRed = Color(0xFFD71921)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .glassySurface(
                shape = RoundedCornerShape(22.dp),
                style = GlassyStyle.CARD,
                isDark = isAmoled,
                borderWidth = 1.dp,
                elevation = 4.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // --- Header: Title + Nothing Red Recording Status ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Nothing Red dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) nothingRed else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "ACOUSTIC SPECTRUM // 10-BAND",
                        style = NothingTechMono,
                        color = textPrimary,
                        letterSpacing = 1.3.sp
                    )
                }

                // Format telemetry
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAmoled) Color(0xFF161720) else Color(0xFFEBEBF0)
                ) {
                    Text(
                        text = if (isPlaying) "LIVE • OPUS 48kHz" else "IDLE • 24-BIT",
                        style = NothingDotBadge,
                        color = if (isPlaying) accentColor else textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- The Graph Area: Dotted dB Grids, Bars, & Bézier Frequency Curve ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAmoled) Color(0xFF07070A) else Color(0xFFF7F7FA))
                    .border(1.dp, if (isAmoled) Color(0x15FFFFFF) else Color(0x0C000000), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp)) {
                    val w = size.width
                    val h = size.height
                    val centerY = h * 0.5f

                    // 1. Draw Dotted dB Reference Grid Lines (+12dB, 0dB, -12dB)
                    val dotEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    val dbLevels = listOf(0.18f, 0.50f, 0.82f)
                    dbLevels.forEach { lvl ->
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, h * lvl),
                            end = Offset(w, h * lvl),
                            strokeWidth = 1f,
                            pathEffect = dotEffect
                        )
                    }

                    // 2. Draw 16 Live Spectrum Frequency Bars (Sub-bass to treble)
                    val barCount = 18
                    val barSpacing = w / barCount
                    for (i in 0 until barCount) {
                        val barX = i * barSpacing + barSpacing * 0.5f
                        val normalizedIdx = i.toFloat() / barCount
                        val osc = if (isPlaying) {
                            (sin(phase + i * 0.65f) * 0.35f + 0.45f).coerceIn(0.1f, 1f)
                        } else {
                            0.15f + (i % 3) * 0.05f
                        }

                        // Influence by active preset gains
                        val presetWeight = when {
                            i < 4 -> node0
                            i < 8 -> node1
                            i < 12 -> node2
                            i < 15 -> node3
                            else -> node4
                        }
                        val barHeight = (h * 0.70f * (osc * 0.7f + presetWeight * 0.3f).coerceIn(0.08f, 0.95f))
                        val barY = h - barHeight

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (isPlaying) 0.85f else 0.4f),
                                    accentColor.copy(alpha = 0.15f)
                                )
                            ),
                            topLeft = Offset(barX - 2.5f, barY),
                            size = androidx.compose.ui.geometry.Size(5f, barHeight)
                        )
                    }

                    // 3. Draw Smooth Bézier Frequency Response Curve
                    val nodes = floatArrayOf(node0, node1, node2, node3, node4)
                    val nodeCount = nodes.size
                    val points = ArrayList<Offset>()

                    for (i in 0 until nodeCount) {
                        val nx = (w / (nodeCount - 1)) * i
                        val ny = centerY - (nodes[i] * (h * 0.42f))
                        points.add(Offset(nx, ny))
                    }

                    // Construct smooth path with cubic Bézier interpolation
                    val curvePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        curvePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, h)
                        fillPath.lineTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlY2 = p1.y

                            curvePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }

                        fillPath.lineTo(points.last().x, h)
                        fillPath.close()

                        // Draw Shaded Frequency Area Fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (isAmoled) 0.22f else 0.15f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Draw Glowing Response Curve Line
                        drawPath(
                            path = curvePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(accentColor, nothingRed, accentColor)
                            ),
                            style = Stroke(width = 2.8f)
                        )

                        // Draw Node Circles with Specular Glow
                        points.forEach { pt ->
                            drawCircle(
                                color = accentColor.copy(alpha = 0.25f),
                                radius = 7f,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = pt
                            )
                        }
                    }
                }

                // dB scale markings on right rim
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("+12dB", style = NothingDotBadge.copy(fontSize = 7.5.sp), color = textSecondary.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(" 0dB", style = NothingDotBadge.copy(fontSize = 7.5.sp), color = textSecondary.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(30.dp))
                    Text("-12dB", style = NothingDotBadge.copy(fontSize = 7.5.sp), color = textSecondary.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Frequency Band Markers Along Bottom ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("32Hz", "125Hz", "500Hz", "2kHz", "8kHz", "16kHz").forEach { freq ->
                    Text(
                        text = freq,
                        style = NothingDotBadge.copy(fontSize = 8.5.sp),
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Interactive EQ Acoustic Preset Switcher ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AcousticPreset.values().forEach { preset ->
                    val isSelected = preset == activePreset
                    val pillText = if (isSelected) Color.White else textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glassySurface(
                                shape = RoundedCornerShape(14.dp),
                                style = if (isSelected) GlassyStyle.PROMINENT else GlassyStyle.SLAB,
                                isDark = isAmoled,
                                tintColor = if (isSelected) accentColor else null,
                                elevation = if (isSelected) 3.dp else 1.dp,
                                onClick = { activePreset = preset }
                            )
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.label,
                            style = NothingTechMono.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = pillText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
