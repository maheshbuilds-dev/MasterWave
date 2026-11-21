package com.masterwave.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.masterwave.player.ui.components.bouncyClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.theme.ThemeState
import androidx.compose.material3.HorizontalDivider
import com.masterwave.player.ui.theme.SwissHairline
import com.masterwave.player.ui.theme.SwissMono
import com.masterwave.player.ui.theme.SwissDisplayLarge
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local, 1: Favorites, 2: History, 3: Downloaded
    var isLoadingLocal by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val localTracks by repository.localTracks.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current

    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, MasterWaveApp.instance.okHttpClient, repository)
    }
    val downloadedIds by downloadManager.downloadedIds.collectAsState()
    val downloadedTracks = localTracks.filter { downloadedIds.contains(it.id) }

    val bgColor = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

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
            .background(bgColor)
            .statusBarsPadding()
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library",
                    style = SwissDisplayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        letterSpacing = (-0.7).sp
                    ),
                    color = textPrimary
                )
            }

            if (selectedTab == 0) {
                IconButton(onClick = { scanLocal() }) {
                    if (isLoadingLocal) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Media",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = SwissHairline,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Tab Selector Row (Pixel-iOS Segmented Pill Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple("LOCAL", Icons.Default.Folder, localTracks.size),
                Triple("FAVES", Icons.Default.Favorite, favorites.size),
                Triple("HISTORY", Icons.Default.History, recentTracks.size),
                Triple("SAVED", Icons.Default.Download, downloadedTracks.size)
            )

            tabs.forEachIndexed { index, (title, icon, count) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                            } else {
                                Modifier.background(
                                    if (isAmoled) Color(0x18FFFFFF) else Color(0x0C000000)
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.10f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        if (isAmoled) Color(0x22FFFFFF) else Color(0x12000000),
                                        if (isAmoled) Color(0x08FFFFFF) else Color(0x04000000)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(19.dp)
                        )
                        .bouncyClickable(scaleDown = 0.92f) { selectedTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isSelected) Color.White else textSecondary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$title ($count)",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
                            color = textSecondary
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
                            color = textSecondary
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
                            color = textSecondary
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
            3 -> {
                // Downloaded
                if (downloadedTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No offline tracks downloaded yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        items(downloadedTracks) { track ->
                            val isFav = favorites.any { it.id == track.id }
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isFavorite = isFav,
                                onClick = { player.playTrack(track, downloadedTracks) },
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
