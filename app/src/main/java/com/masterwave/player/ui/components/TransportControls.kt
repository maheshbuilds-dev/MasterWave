package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.masterwave.player.ui.animation.applePressEffect
import com.masterwave.player.ui.theme.AccentCrimson
import com.masterwave.player.ui.theme.TextPrimary
import com.masterwave.player.ui.theme.TextSecondary
import com.masterwave.player.ui.theme.TextTertiary

@Composable
fun TransportControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .applePressEffect(targetScale = 0.88f, onClick = onToggleShuffle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) AccentCrimson else TextTertiary,
                modifier = Modifier.size(26.dp)
            )
        }

        // Previous Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .applePressEffect(targetScale = 0.90f, onClick = onPrevious),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // Main Big Play/Pause Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = AccentCrimson.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(AccentCrimson)
                .applePressEffect(targetScale = 0.93f, onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = Pair(isLoading, isPlaying),
                transitionSpec = {
                    (scaleIn(initialScale = 0.86f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(140)))
                        .togetherWith(scaleOut(targetScale = 0.86f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(100)))
                },
                label = "transport_play_pause"
            ) { (loading, playing) ->
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // Next Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .applePressEffect(targetScale = 0.90f, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // Repeat Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .applePressEffect(targetScale = 0.88f, onClick = onToggleRepeat),
            contentAlignment = Alignment.Center
        ) {
            val repeatIcon = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat
            val repeatTint = if (repeatMode != 0) AccentCrimson else TextTertiary
            Icon(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = repeatTint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
