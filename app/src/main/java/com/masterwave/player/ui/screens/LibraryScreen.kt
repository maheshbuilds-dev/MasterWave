package com.masterwave.player.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.theme.LightBackground
import com.masterwave.player.ui.theme.LightCardPink
import com.masterwave.player.ui.theme.LightPrimaryMaroon
import com.masterwave.player.ui.theme.LightSurfaceVariant
import com.masterwave.player.ui.theme.LightTextPrimary
import com.masterwave.player.ui.theme.LightTextSecondary
import com.masterwave.player.ui.theme.LightTextTertiary
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
            .background(LightBackground)
            .statusBarsPadding()
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                ),
                color = LightTextPrimary
            )

            if (selectedTab == 0) {
                IconButton(onClick = { scanLocal() }) {
                    if (isLoadingLocal) {
                        CircularProgressIndicator(
                            color = LightPrimaryMaroon,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Media",
                            tint = LightPrimaryMaroon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LightBackground,
            contentColor = LightPrimaryMaroon,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = LightPrimaryMaroon,
                    height = 3.dp
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            LibraryTabItem(
                title = "Local (${localTracks.size})",
                icon = Icons.Default.Folder,
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            LibraryTabItem(
                title = "Favorites (${favorites.size})",
                icon = Icons.Default.Favorite,
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
            LibraryTabItem(
                title = "History (${recentTracks.size})",
                icon = Icons.Default.History,
                isSelected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Local Tracks
                if (localTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isLoadingLocal) "Scanning on-device music..." else "No local tracks found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LightTextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        items(localTracks) { track ->
                            val isFav = favorites.any { it.id == track.id }
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = isFav,
                                onClick = { player.playTrack(track, localTracks) },
                                onFavoriteToggle = { repository.toggleFavorite(track) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            1 -> {
                // Favorites
                if (favorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No favorites yet. Tap ♡ on any track to add.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LightTextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        items(favorites) { track ->
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = true,
                                onClick = { player.playTrack(track, favorites) },
                                onFavoriteToggle = { repository.toggleFavorite(track) },
                                modifier = Modifier.padding(vertical = 4.dp)
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
                            color = LightTextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        items(recentTracks) { track ->
                            val isFav = favorites.any { it.id == track.id }
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = isFav,
                                onClick = { player.playTrack(track, recentTracks) },
                                onFavoriteToggle = { repository.toggleFavorite(track) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = isSelected,
        onClick = onClick,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) LightPrimaryMaroon else LightTextTertiary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    ),
                    color = if (isSelected) LightPrimaryMaroon else LightTextTertiary
                )
            }
        }
    )
}
