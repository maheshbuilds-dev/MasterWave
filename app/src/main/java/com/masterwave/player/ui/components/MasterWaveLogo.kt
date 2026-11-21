package com.masterwave.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.masterwave.player.R

/**
 * MasterWave Brand Emblem:
 * Displays the iconic crimson red wave "M" logo with subtle alive breathing shimmer.
 */
@Composable
fun MasterWaveLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animated: Boolean = true
) {
    val scale = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        animatedScale
    } else {
        1.0f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_masterwave_logo),
            contentDescription = "MasterWave Logo",
            modifier = Modifier
                .size(size)
                .scale(scale)
        )
    }
}
