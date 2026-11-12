package com.masterwave.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.masterwave.player.ui.theme.AccentBurgundy
import com.masterwave.player.ui.theme.AccentCrimsonGlow
import com.masterwave.player.ui.theme.SurfaceCard
import com.masterwave.player.ui.theme.TextTertiary

@Composable
fun ArtworkHero(
    artworkUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = tween(durationMillis = 400),
        label = "ArtworkScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow background
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentCrimsonGlow, AccentBurgundy.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        // Artwork image card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "Album Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}
