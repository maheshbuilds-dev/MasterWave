package com.masterwave.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.theme.AccentCrimson
import com.masterwave.player.ui.theme.CanvasObsidian
import com.masterwave.player.ui.theme.SurfaceElevated
import com.masterwave.player.ui.theme.TextPrimary
import com.masterwave.player.ui.theme.TextSecondary
import com.masterwave.player.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local, 1: Favorites, 2: History
    var isLoadingLocal by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val localTracks by repository.localTracks.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    fun scanLocal() {
        isLoadingLocal = true
        coroutineScope.launch {
            repository.loadLocalTracks()
            isLoadingLocal = false
        }
    }

    LaunchedEffect(Unit) {
        if (localTracks.isEmpty()) {
            scanLocal()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasObsidian)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )

            if (selectedTab == 0) {
                IconButton(
                    onClick = { scanLocal() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton("Local Audio", selectedTab == 0) { selectedTab = 0 }
            TabButton("Favorites", selectedTab == 1) { selectedTab = 1 }
            TabButton("Recents", selectedTab == 2) { selectedTab = 2 }
        }

        // Active List
        when (selectedTab) {
            0 -> {
                if (isLoadingLocal) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentCrimson)
                    }
                } else if (localTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No audio files found on device storage",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                    ) {
                        items(localTracks) { track ->
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = favorites.contains(track.id),
                                onClick = { player.playTrack(track, localTracks) },
                                onFavoriteToggle = { repository.toggleFavorite(track.id) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
            1 -> {
                // Favorites
                val favoriteTracks = (localTracks + recentTracks).distinctBy { it.id }.filter { favorites.contains(it.id) }
                if (favoriteTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No favorites yet. Tap ♡ on any track to add.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                    ) {
                        items(favoriteTracks) { track ->
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = true,
                                onClick = { player.playTrack(track, favoriteTracks) },
                                onFavoriteToggle = { repository.toggleFavorite(track.id) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
            2 -> {
                // Recents
                if (recentTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recently played tracks",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                    ) {
                        items(recentTracks) { track ->
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = favorites.contains(track.id),
                                onClick = { player.playTrack(track, recentTracks) },
                                onFavoriteToggle = { repository.toggleFavorite(track.id) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentCrimson else SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}
