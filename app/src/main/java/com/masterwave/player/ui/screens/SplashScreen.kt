package com.masterwave.player.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.components.MasterWaveLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.72f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(750, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(600)
        )
        delay(1400)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF131024),
                        Color(0xFF090712),
                        Color(0xFF040307)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Concentric acoustic wave ripples
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .border(
                    width = 1.8.dp,
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF00F5D4), Color(0xFF8B5CF6), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(310.dp)
                .scale(pulseScale * 1.15f)
                .alpha(pulseAlpha * 0.6f)
                .border(
                    width = 1.dp,
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF2A55), Color(0xFF8B5CF6), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // MasterWave Iconic Waveform Emblem
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF20163A),
                                Color(0xFF0F0B1E),
                                Color(0xFF07050F)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF00F5D4),
                                Color(0xFF8B5CF6),
                                Color(0xFFFF2A55)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                MasterWaveLogo(
                    size = 84.dp,
                    animated = true
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MasterWave Typography with Wide Letter-Spacing
            Text(
                text = "MASTERWAVE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    letterSpacing = 6.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sound Engine Tagline
            Text(
                text = "LOSSLESS 48kHz AUDIO ENGINE",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF00F5D4).copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Real-Time Audio Spectrum Bars Animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(7) { index ->
                    val barHeight by infiniteTransition.animateFloat(
                        initialValue = 6f + (index % 4) * 4f,
                        targetValue = 26f - (index % 3) * 5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(380 + index * 75, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "eq_bar_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(3.2.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF00F5D4),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFFF2A55)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Hardware Decoder Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF141322))
                    .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "OPUS • HIGH RES AUDIO • 120Hz",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}
