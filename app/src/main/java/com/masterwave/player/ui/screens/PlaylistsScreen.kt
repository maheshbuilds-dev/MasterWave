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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

/**
 * Playlists Screen featuring Dedicated Liked Songs & Local / Downloaded Songs cards,
 * 1-Tap Cloud Sync for YouTube Music & Spotify, and custom playlist creation.
 */
@Composable
fun PlaylistsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onPlaylistSelected: (Playlist) -> Unit,
    onNavigateToLikedSongs: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToGenerator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playlists by repository.playlists.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val context = LocalContext.current
    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, repository.okHttpClient, repository)
    }
    val downloadedTracks by downloadManager.downloadedTracks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var isSyncingCloud by remember { mutableStateOf(false) }

    var activeLibraryFilter by remember { mutableStateOf("Playlists") }
    val libraryFilters = listOf("Playlists", "Songs", "Albums", "Artists", "Podcasts")

    var activeSortOption by remember { mutableStateOf("Date added") }
    val sortOptions = listOf("Date added", "A-Z", "Track count", "Most played")
    var showSortMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val cloudManager = remember { CloudAccountManager.getInstance(context, repository) }

    val localTracks by repository.localTracks.collectAsState()
    val playCounts by repository.playCounts.collectAsState()

    val userPlaylists = playlists.filter { it.id != "liked_songs" && it.id != "local_songs" }
    val sortedPlaylists = remember(userPlaylists, activeSortOption, searchQuery) {
        val filtered = if (searchQuery.isBlank()) userPlaylists else userPlaylists.filter { it.title.contains(searchQuery, ignoreCase = true) }
        when (activeSortOption) {
            "A-Z" -> filtered.sortedBy { it.title.lowercase() }
            "Track count" -> filtered.sortedByDescending { it.trackCount }
            "Most played" -> filtered.sortedByDescending { it.tracks.sumOf { t -> playCounts[t.id] ?: 0 } }
            else -> filtered // "Date added"
        }
    }
    val filteredPlaylists = sortedPlaylists

    val brandTurquoise = com.masterwave.player.ui.theme.MasterWaveTurquoise
    val brandCyan = com.masterwave.player.ui.theme.MasterWaveCyan

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF6F5F3)
    val cardBg = if (isAmoled) Color(0xFF101018) else Color(0xFFFFFFFF)
    val searchBg = if (isAmoled) Color(0xFF161622) else Color(0xFFEFECEF)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top Header Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIBRARY",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = brandTurquoise
                    )
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Generator pill button
                    Surface(
                        shape = CircleShape,
                        color = if (isAmoled) Color(0xFF161622) else Color(0xFFE2E0E4),
                        modifier = Modifier.clickable(onClick = onNavigateToGenerator)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Generator",
                                tint = brandTurquoise,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = textPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(searchBg)
                            .clickable { showCreateDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Playlist",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // --- 1.5 Category Filters & Sorting Row ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(libraryFilters) { filter ->
                        val isSelected = filter == activeLibraryFilter
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) {
                                brandTurquoise.copy(alpha = 0.22f)
                            } else searchBg,
                            modifier = Modifier.bouncyClickable {
                                activeLibraryFilter = filter
                                Toast.makeText(context, "Filtered: $filter", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) brandTurquoise else textSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Sorting Pill & Smart Collections Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort Pill
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = searchBg,
                            modifier = Modifier.bouncyClickable { showSortMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = brandTurquoise,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeSortOption,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = textPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            for (opt in sortOptions) {
                                DropdownMenuItem(
                                    text = { Text(opt, fontWeight = if (opt == activeSortOption) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        activeSortOption = opt
                                        showSortMenu = false
                                        Toast.makeText(context, "Sorted by $opt", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Smart Collections Shortcut Count
                    Text(
                        text = "${filteredPlaylists.size} playlists • ${favorites.size + downloadedTracks.size} items",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = textSecondary
                    )
                }
            }
        }

        // --- 2. Top Hero Cards: Liked Songs & Local Downloaded Songs ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Liked Songs
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .bouncyClickable(onClick = onNavigateToLikedSongs)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(brandTurquoise.copy(alpha = 0.14f), Color.Transparent)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(brandTurquoise.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = brandTurquoise,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Liked Songs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "${favorites.size} songs",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // Card 2: Local & Downloaded Songs
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .bouncyClickable(onClick = onNavigateToDownloads)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(brandCyan.copy(alpha = 0.14f), Color.Transparent)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(brandCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = brandCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Local Songs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "${downloadedTracks.size} offline",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2.5 Generator Action Banner ---
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(onClick = onNavigateToGenerator)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(brandTurquoise.copy(alpha = 0.12f), brandCyan.copy(alpha = 0.06f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(brandTurquoise.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = brandTurquoise,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✦ Generator",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Algorithmic playlists, song radio & acoustic DNA",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = brandTurquoise.copy(alpha = 0.18f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Open Generator",
                                    tint = brandTurquoise,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. 1-Tap Cloud Sync Section ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = brandTurquoise,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cloud Sync Playlists",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = textPrimary
                        )
                    }

                    if (isSyncingCloud) {
                        CircularProgressIndicator(
                            color = brandTurquoise,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            modifier = Modifier.clickable {
                                if (!isSyncingCloud) {
                                    isSyncingCloud = true
                                    scope.launch {
                                        val synced = cloudManager.syncRealYouTubeMusicLibrary()
                                        isSyncingCloud = false
                                        if (synced.isNotEmpty()) {
                                            Toast.makeText(context, "Synced ${synced.size} YouTube playlists", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Sign in with Google in Settings to sync playlists", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(brandTurquoise.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = brandTurquoise,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "YouTube Music",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Sync Google Account",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Open Settings > YouTube & Sync to import link", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(brandCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = brandCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Import Link",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "YouTube URL / ID",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Spotify account sync active in Settings", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(brandTurquoise.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = brandTurquoise,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Spotify Sync",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Web Authenticated",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Search Filter for Custom Playlists ---
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search playlists...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = searchBg,
                    unfocusedContainerColor = searchBg,
                    disabledContainerColor = searchBg,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }

        // --- 5. Custom Playlists List ---
        items(filteredPlaylists, key = { it.id }) { playlist ->
            val artUrl = playlist.artworkUrl ?: playlist.tracks.firstOrNull()?.artworkUrl
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { onPlaylistSelected(playlist) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(searchBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!artUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = playlist.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${playlist.trackCount} tracks • ${playlist.source}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }

                    if (playlist.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(130.dp))
        }
    }

    // Dialog for creating new playlist
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    label = { Text("Playlist Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistTitle.isNotBlank()) {
                            repository.createPlaylist(newPlaylistTitle.trim())
                            newPlaylistTitle = ""
                            showCreateDialog = false
                            Toast.makeText(context, "Playlist created!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
