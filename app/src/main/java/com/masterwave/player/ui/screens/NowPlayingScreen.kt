package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.AnimatedWaveform
import com.masterwave.player.ui.theme.DarkBackground
import com.masterwave.player.ui.theme.DarkPrimaryCrimson
import com.masterwave.player.ui.theme.DarkSurfaceElevated
import com.masterwave.player.ui.theme.DarkTextPrimary
import com.masterwave.player.ui.theme.DarkTextSecondary
import com.masterwave.player.ui.theme.DarkTextTertiary

/**
 * Now Playing Screen redesigned to match Screenshot 5 precisely:
 * - Blurred artwork backdrop with rich crimson ambient lighting.
 * - Pill round back button and 3-dots pill.
 * - High-res cover hero with soft corner radii.
 * - Song Title, Artist and dual pill actions: Heart & Quotes (lyrics).
 * - Animated Waveform Seekbar with harmonic oscillations & Opus badge.
 * - Previous/Next circular pills + Wide Stadium Pause/Play pill button.
 * - Bottom row: Shuffle stadium, Speaker/Output stadium, Repeat stadium.
 */
@Composable
fun NowPlayingScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onCollapse: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isLoading by player.isLoading.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val favorites by repository.favorites.collectAsState()

    val currentTrack = track ?: return
    val isFavorite = favorites.any { it.id == currentTrack.id }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Blurred Artwork Background Layer
        if (!currentTrack.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentTrack.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
            )
        }

        // Dark Vignette & Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC08080A),
                            Color(0x9908080A),
                            Color(0xDD08080A),
                            Color(0xFF08080A)
                        )
                    )
                )
        )

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. Top Header Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Collapse pill button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x3322222E))
                        .clickable(onClick = onCollapse),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center header title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "MasterWave",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = DarkTextTertiary
                    )
                }

                // 3-dots options pill button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x3322222E))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Queue") },
                            onClick = {
                                showMenu = false
                                onOpenQueue()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View Lyrics") },
                            onClick = {
                                showMenu = false
                                onOpenLyrics()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Audio Quality (Opus 160 kbps)") },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Streaming at high-fidelity Opus 160 kbps", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 2. Hero Artwork Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color(0x88E53935))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF16161E)),
                contentAlignment = Alignment.Center
            ) {
                if (!currentTrack.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentTrack.artworkUrl,
                        contentDescription = currentTrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. Track Metadata & Actions Row (Heart & Lyrics) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = DarkTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTrack.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = DarkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Actions: Heart & Lyrics pill buttons (Screenshot 5)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Heart Button with scale animation
                    val heartScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "heart_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x33282836))
                            .clickable {
                                repository.toggleFavorite(currentTrack)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) DarkPrimaryCrimson else DarkTextPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(heartScale)
                        )
                    }

                    // Lyrics Quotes Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x33282836))
                            .clickable(onClick = onOpenLyrics),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4. Animated Waveform Seekbar ---
            AnimatedWaveform(
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                codecLabel = "Opus • OPUS 160 kbps",
                onSeek = { player.seekTo(it) },
                onCodecClick = {
                    Toast.makeText(context, "Audio Engine: Hardware Opus Decoder (160 kbps, 48 kHz Stereo)", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. Main Playback Controls Row (Screenshot 5) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Track circular button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x33282836))
                        .clickable { player.skipToPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Pause / Play Large Stadium Pill Button (Screenshot 5)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0x33282836),
                    modifier = Modifier
                        .height(58.dp)
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .clickable(onClick = { player.togglePlayPause() })
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = DarkTextPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isPlaying) "Pause" else "Play",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = DarkTextPrimary
                            )
                        }
                    }
                }

                // Next Track circular button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x33282836))
                        .clickable { player.skipToNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 6. Bottom Utility Controls Row (Shuffle, Speaker, Repeat) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Stadium Pill
                val shuffleBg by animateColorAsState(
                    targetValue = if (isShuffle) DarkPrimaryCrimson.copy(alpha = 0.35f) else Color(0x2222222E),
                    label = "shuffle_bg"
                )
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(shuffleBg)
                        .clickable { player.toggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) DarkPrimaryCrimson else DarkTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center Speaker / Output Stadium Pill (Screenshot 5: [ 🔊 | ▶ ])
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1.3f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x2222222E))
                        .clickable {
                            Toast.makeText(context, "Audio Output: Device Speaker / Bluetooth", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speaker",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0x44FFFFFF))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Output",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Repeat Stadium Pill
                val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                val repeatBg by animateColorAsState(
                    targetValue = if (isRepeatActive) DarkPrimaryCrimson.copy(alpha = 0.35f) else Color(0x2222222E),
                    label = "repeat_bg"
                )
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(repeatBg)
                        .clickable { player.cycleRepeatMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) DarkPrimaryCrimson else DarkTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
