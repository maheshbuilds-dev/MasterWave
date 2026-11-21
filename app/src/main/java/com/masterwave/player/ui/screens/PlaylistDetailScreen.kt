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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.EditorialHeadlineMedium
import com.masterwave.player.ui.theme.ThemeState

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current

    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, MasterWaveApp.instance.okHttpClient, repository)
    }
    val downloadedIds by downloadManager.downloadedIds.collectAsState()

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFAF0)
    val cardBg = if (isAmoled) Color(0xFF12121A) else Color(0xFFF5F0E0)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)
    val pillBg = if (isAmoled) Color(0xFF181824) else Color(0xFFFAF5E8)

    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showGoogleAuth by remember { mutableStateOf(false) }

    val isCustomPlaylist = playlist.source == "Custom" || playlist.source == "User" || playlist.id.startsWith("custom_") || playlist.id.startsWith("user_")
    var tracksList by remember(playlist.id, playlist.tracks) { mutableStateOf(playlist.tracks) }
    var isLoadingTracks by remember(playlist.id) {
        mutableStateOf(playlist.tracks.isEmpty() && !isCustomPlaylist && (playlist.description.isNotBlank() || playlist.title.isNotBlank()))
    }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    val allPlaylists by repository.playlists.collectAsState()
    val updatedPlaylist = allPlaylists.find { it.id == playlist.id }
    LaunchedEffect(updatedPlaylist) {
        if (updatedPlaylist != null) {
            tracksList = updatedPlaylist.tracks
        }
    }

    LaunchedEffect(playlist.id) {
        if (tracksList.isEmpty() && !isCustomPlaylist && (playlist.description.isNotBlank() || playlist.title.isNotBlank())) {
            isLoadingTracks = true
            try {
                if (playlist.id == "yt_liked_songs") {
                    val cloudManager = com.masterwave.player.data.cloud.CloudAccountManager.getInstance(context, repository)
                    val synced = cloudManager.syncRealYouTubeMusicLibrary()
                    val ytLiked = synced.find { it.id == "yt_liked_songs" } ?: repository.playlists.value.find { it.id == "yt_liked_songs" }
                    if (ytLiked != null && ytLiked.tracks.isNotEmpty()) {
                        tracksList = ytLiked.tracks
                    }
                } else if (playlist.id.startsWith("yt_")) {
                    val cloudManager = com.masterwave.player.data.cloud.CloudAccountManager.getInstance(context, repository)
                    val cookies = cloudManager.ytCookies.value
                    if (!cookies.isNullOrBlank()) {
                        val ytFetcher = com.masterwave.player.data.cloud.YouTubeLibraryFetcher(com.masterwave.player.MasterWaveApp.instance.okHttpClient, repository)
                        val realTracks = ytFetcher.fetchPlaylistTracks(playlist.id.removePrefix("yt_"), cookies)
                        if (realTracks.isNotEmpty()) {
                            tracksList = realTracks
                            repository.addCustomPlaylist(playlist.copy(tracks = realTracks, trackCount = realTracks.size))
                        }
                    }
                } else if (playlist.source != "YouTube Music") {
                    // Only for pre-seeded mood/genre explore cards, NEVER for YouTube playlists
                    val q = playlist.description.ifBlank { playlist.title }
                    val results = repository.search(q).songs
                    if (results.isNotEmpty()) {
                        tracksList = results
                    }
                }
            } catch (_: Exception) {}
            isLoadingTracks = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .bouncyClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )

            Box {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .bouncyClickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Download All (${playlist.tracks.size} tracks)") },
                        onClick = {
                            showMenu = false
                            playlist.tracks.forEach { tr ->
                                downloadManager.downloadTrack(tr)
                            }
                            Toast.makeText(context, "Queued all tracks for download", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // --- Playlist Header Hero ---
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Rounded Artwork Box
                    val displayArt = playlist.artworkUrl ?: playlist.tracks.firstOrNull()?.artworkUrl
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(pillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!displayArt.isNullOrBlank()) {
                            AsyncImage(
                                model = displayArt,
                                contentDescription = playlist.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (playlist.id == "liked_songs") Icons.Default.Favorite else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (playlist.id == "liked_songs") accentColor else textSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = playlist.title,
                        style = EditorialHeadlineMedium.copy(fontSize = 26.sp),
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${tracksList.size.coerceAtLeast(playlist.trackCount)} tracks • ${playlist.source} • ${playlist.createdAt}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Play All, Shuffle & Add Songs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tracksList.isNotEmpty()) {
                            Button(
                                onClick = {
                                    player.playTrack(tracksList.first(), tracksList)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bouncyClickable(scaleDown = 0.94f, onClick = {
                                        player.playTrack(tracksList.first(), tracksList)
                                    })
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play All",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val shuffled = tracksList.shuffled()
                                    player.playTrack(shuffled.first(), shuffled)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = pillBg),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bouncyClickable(scaleDown = 0.94f, onClick = {
                                        val shuffled = tracksList.shuffled()
                                        player.playTrack(shuffled.first(), shuffled)
                                    })
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                        }

                        if (isCustomPlaylist) {
                            Button(
                                onClick = { showAddSongsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (tracksList.isEmpty()) accentColor else pillBg),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(if (tracksList.isEmpty()) 2f else 1f)
                                    .height(46.dp)
                                    .bouncyClickable(scaleDown = 0.94f, onClick = { showAddSongsDialog = true })
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Songs",
                                    tint = if (tracksList.isEmpty()) Color.White else textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Songs", fontWeight = FontWeight.Bold, color = if (tracksList.isEmpty()) Color.White else textPrimary)
                            }
                        }
                    }
                }
            }

            // --- Track List ---
            if (isLoadingTracks) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp))
                    }
                }
            } else if (tracksList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playlist.id == "yt_liked_songs") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No YouTube Liked Songs Synced",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Connect your Google account or tap below to sync your real liked tracks from YouTube Music.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoadingTracks = true
                                            val cloudManager = com.masterwave.player.data.cloud.CloudAccountManager.getInstance(context, repository)
                                            val synced = cloudManager.syncRealYouTubeMusicLibrary()
                                            val ytLiked = synced.find { it.id == "yt_liked_songs" } ?: repository.playlists.value.find { it.id == "yt_liked_songs" }
                                            if (ytLiked != null && ytLiked.tracks.isNotEmpty()) {
                                                tracksList = ytLiked.tracks
                                            } else {
                                                showGoogleAuth = true
                                            }
                                            isLoadingTracks = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync / Connect Google Account", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No tracks in this playlist yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showAddSongsDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Songs", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(tracksList, key = { index, tr -> "${tr.id}_$index" }) { index, track ->
                    val isCurrent = currentTrack?.id == track.id
                    val isDownloaded = downloadedIds.contains(track.id)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) accentColor.copy(alpha = if (isAmoled) 0.25f else 0.12f) else cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClickable {
                                player.playTrack(track, tracksList)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pillBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
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

                            // Track Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.5.sp
                                    ),
                                    color = if (isCurrent) accentColor else textPrimary,
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

                            if (isCustomPlaylist) {
                                IconButton(onClick = {
                                    repository.removeTrackFromPlaylist(playlist.id, track.id)
                                    tracksList = tracksList.filter { it.id != track.id }
                                    Toast.makeText(context, "Removed from ${playlist.title}", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Download Icon
                            IconButton(onClick = {
                                downloadManager.downloadTrack(track)
                                Toast.makeText(context, "Downloading \"${track.title}\"...", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) accentColor else textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsDialog) {
        AddSongsDialog(
            playlist = playlist,
            repository = repository,
            accentColor = accentColor,
            cardBg = cardBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onDismiss = { showAddSongsDialog = false },
            onTrackAdded = { newTrack ->
                if (tracksList.none { it.id == newTrack.id }) {
                    tracksList = tracksList + newTrack
                }
            }
        )
    }

    if (showGoogleAuth) {
        com.masterwave.player.ui.components.WebAuthDialog(
            isOpen = showGoogleAuth,
            providerTitle = "Google / YouTube Music",
            initialUrl = "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F",
            successDomain = "music.youtube.com",
            onDismiss = { showGoogleAuth = false },
            onAuthCaptured = { cookies ->
                val cloudManager = com.masterwave.player.data.cloud.CloudAccountManager.getInstance(context, repository)
                cloudManager.saveYouTubeAuth(cookies)
                showGoogleAuth = false
                scope.launch {
                    isLoadingTracks = true
                    val synced = cloudManager.syncRealYouTubeMusicLibrary()
                    val ytLiked = synced.find { it.id == "yt_liked_songs" } ?: repository.playlists.value.find { it.id == "yt_liked_songs" }
                    if (ytLiked != null && ytLiked.tracks.isNotEmpty()) {
                        tracksList = ytLiked.tracks
                    }
                    isLoadingTracks = false
                }
                android.widget.Toast.makeText(context, "Google Account Authorized!", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AddSongsDialog(
    playlist: Playlist,
    repository: MusicRepository,
    accentColor: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    onDismiss: () -> Unit,
    onTrackAdded: (Track) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val favorites by repository.favorites.collectAsState()
    val localTracks by repository.localTracks.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Search, 1 = Liked Songs, 2 = Local Audio

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to ${playlist.title}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.isNotBlank()) {
                            isSearching = true
                            scope.launch {
                                try {
                                    val res = repository.search(query).songs
                                    searchResults = res
                                } catch (_: Exception) {}
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    placeholder = { Text("Search songs on YouTube...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick tabs: Search, Liked Songs, Local
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Search", "Liked (${favorites.size})", "Local (${localTracks.size})").forEachIndexed { index, label ->
                        val isSelected = activeTab == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .weight(1f)
                                .bouncyClickable { activeTab = index }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) Color.White else textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayedTracks = when (activeTab) {
                    0 -> searchResults
                    1 -> favorites
                    else -> localTracks
                }

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
                    }
                } else if (displayedTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (activeTab == 0 && searchQuery.isBlank()) "Type in the search bar above to find songs" else "No songs found",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedTracks, key = { it.id }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
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
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        repository.addTrackToPlaylist(playlist.id, track)
                                        onTrackAdded(track)
                                        Toast.makeText(context, "Added \"${track.title}\"", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
