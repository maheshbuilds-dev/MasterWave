package com.masterwave.player.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.model.LyricLine
import com.masterwave.player.data.model.LyricsResult
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.AccentCrimson
import com.masterwave.player.ui.theme.CanvasObsidian
import com.masterwave.player.ui.theme.SurfaceElevated
import com.masterwave.player.ui.theme.TextPrimary
import com.masterwave.player.ui.theme.TextSecondary
import com.masterwave.player.ui.theme.TextTertiary

@Composable
fun LyricsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track by player.currentTrack.collectAsState()
    val positionMs by player.positionMs.collectAsState()

    var lyricsResult by remember { mutableStateOf<LyricsResult?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(track?.id) {
        val curTrack = track
        if (curTrack != null) {
            isLoadingLyrics = true
            lyricsResult = repository.getLyrics(curTrack)
            isLoadingLyrics = false
        }
    }

    val currentLineIndex = remember(positionMs, lyricsResult) {
        val lines = lyricsResult?.lines ?: emptyList()
        val idx = lines.indexOfLast { it.timestampMs <= positionMs }
        if (idx >= 0) idx else 0
    }

    LaunchedEffect(currentLineIndex) {
        if (lyricsResult?.isSynced == true && currentLineIndex >= 0) {
            listState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = track?.title ?: "Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isLoadingLyrics) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentCrimson)
            }
        } else if (lyricsResult == null || lyricsResult!!.lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No synchronized lyrics available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextTertiary
                )
            }
        } else {
            val lines = lyricsResult!!.lines
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp, start = 24.dp, end = 24.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    val isActive = index == currentLineIndex && lyricsResult!!.isSynced
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) TextPrimary else TextTertiary,
                        label = "LyricTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (lyricsResult!!.isSynced) {
                                    player.seekTo(line.timestampMs)
                                }
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = line.text.ifBlank { "♪" },
                            fontSize = if (isActive) 24.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = textColor,
                            lineHeight = if (isActive) 32.sp else 24.sp
                        )
                    }
                }
            }
        }
    }
}
