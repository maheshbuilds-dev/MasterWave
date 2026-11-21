package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.components.glassySlab
import com.masterwave.player.ui.theme.MasterWaveTurquoise
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

/**
 * MASTERWAVE 2.0 PLAYING QUEUE SCREEN
 * Features:
 * - Distinct Now Playing Hero Card with live animated audio equalizer bars.
 * - Up Next track list with track duration and quick swipe/tap removal.
 * - Dynamic "Recommended Next" shelf powered by the local Recommendation Engine.
 * - Tactile 1-tap "+ Add to Queue" actions.
 */
@Composable
fun QueueScreen(
    player: MasterWavePlayer,
    onBack: () -> Unit,
    repository: MusicRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val queue by player.queue.collectAsState()
    val currentIndex by player.currentIndex.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val bgColor = if (isAmoled) Color(0xFF090A0F) else Color(0xFFFFFAF0)
    val cardBg = if (isAmoled) Color(0xFF12141C) else Color(0xFFF5F0E0)
    val cardBorder = if (isAmoled) Color(0xFF1E2230) else Color(0xFF0A0A0A)
    val textPrimary = if (isAmoled) Color(0xFFF5F6FA) else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFF989EAF) else Color(0xFF6A6A6A)

    // Recommended candidates for the current song
    var recommendedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    LaunchedEffect(currentTrack?.id) {
        val cur = currentTrack
        if (cur != null && repository != null) {
            scope.launch {
                val recs = repository.recommendationManager.getQueueRecommendations(cur, limit = 8)
                recommendedTracks = recs.filterNot { rec -> queue.any { it.id == rec.id } }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .glassySlab(
                        shape = CircleShape,
                        isDark = isAmoled,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Playing Queue",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = textPrimary
                )
                Text(
                    text = "${queue.size} tracks queued",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary
                )
            }

            // Clear Queue Button
            if (queue.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .glassySlab(
                            shape = CircleShape,
                            isDark = isAmoled,
                            onClick = {
                                player.clearQueue()
                                Toast.makeText(context, "Queue cleared", Toast.LENGTH_SHORT).show()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Queue",
                        tint = MasterWaveTurquoise,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(42.dp))
            }
        }

        if (queue.isEmpty() && currentTrack == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .glassySlab(shape = CircleShape, isDark = isAmoled),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your queue is empty",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Play any song to start your queue",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- 2. Now Playing Hero Card ---
                if (currentTrack != null) {
                    item {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = MasterWaveTurquoise,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MasterWaveTurquoise.copy(alpha = 0.35f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Artwork
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF1B1D28)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!currentTrack!!.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentTrack!!.artworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack!!.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentTrack!!.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Play / Pause Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MasterWaveTurquoise)
                                        .clickable { player.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // --- 3. Up Next Header & Tracks ---
                val upNextTracks = queue.filterIndexed { idx, _ -> idx > currentIndex }
                if (upNextTracks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UP NEXT (${upNextTracks.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = textSecondary
                            )
                        }
                    }

                    itemsIndexed(upNextTracks) { offsetIdx, track ->
                        val absoluteIndex = queue.indexOfFirst { it.id == track.id }
                        QueueTrackRow(
                            track = track,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onPlay = {
                                player.playTrack(track, queue)
                            },
                            onRemove = {
                                player.removeFromQueue(track.id)
                                Toast.makeText(context, "Removed from queue", Toast.LENGTH_SHORT).show()
                            },
                            onMoveUp = if (absoluteIndex > currentIndex + 1) {
                                { player.moveQueueItem(absoluteIndex, absoluteIndex - 1) }
                            } else null,
                            onMoveDown = if (absoluteIndex < queue.lastIndex) {
                                { player.moveQueueItem(absoluteIndex, absoluteIndex + 1) }
                            } else null
                        )
                    }
                }

                // --- 4. Recommended Next Tracks Section ---
                if (recommendedTracks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MasterWaveTurquoise,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECOMMENDED NEXT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MasterWaveTurquoise
                            )
                        }
                    }

                    itemsIndexed(recommendedTracks) { _, track ->
                        RecommendedQueueTrackRow(
                            track = track,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onAdd = {
                                player.addToQueue(track)
                                repository?.recommendationManager?.eventTracker?.onManualQueueAdd(track)
                                recommendedTracks = recommendedTracks.filterNot { it.id == track.id }
                                Toast.makeText(context, "Added to queue: ${track.title}", Toast.LENGTH_SHORT).show()
                            },
                            onPlayNow = {
                                val newQueue = listOf(track) + recommendedTracks
                                player.playTrack(track, newQueue)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1D28)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration format
            val min = track.durationSeconds / 60
            val sec = track.durationSeconds % 60
            Text(
                text = String.format("%d:%02d", min, sec),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            if (onMoveUp != null) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move up",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (onMoveDown != null) {
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move down",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RecommendedQueueTrackRow(
    track: Track,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onPlayNow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayNow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1D28)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • Recommended",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // + Add to Queue button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MasterWaveTurquoise.copy(alpha = 0.18f))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Queue",
                        tint = MasterWaveTurquoise,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MasterWaveTurquoise
                    )
                }
            }
        }
    }
}
