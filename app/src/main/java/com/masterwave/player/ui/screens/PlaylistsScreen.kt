package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.ThemeState

/**
 * Apple Music-style Library tab.
 *
 * Deliberately limited to things that actually work offline in MasterWave:
 * smart collections, the user's playlists, search, sorting and creation.
 */
@Composable
fun PlaylistsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onPlaylistSelected: (Playlist) -> Unit,
    onNavigateToLikedSongs: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToGenerator: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {},
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val playlists by repository.playlists.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val localTracks by repository.localTracks.collectAsState()

    val context = LocalContext.current
    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, repository.okHttpClient, repository)
    }
    val downloadedTracks by downloadManager.downloadedTracks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var activeSortOption by remember { mutableStateOf("Date added") }
    val sortOptions = listOf("Date added", "A-Z", "Track count", "Most played")

    val accentColor = MaterialTheme.colorScheme.primary
    val isAmoled = ThemeState.isAmoledTheme()
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val fieldBg = MaterialTheme.colorScheme.surface
    val cardBg = MaterialTheme.colorScheme.surface
    val borderColor = if (isAmoled) Color(0x18FFFFFF) else Color(0xFF0A0A0A)

    val playCounts by repository.playCounts.collectAsState()
    val userPlaylists = playlists.filter { it.id != "liked_songs" && it.id != "local_songs" }
    val sortedPlaylists = remember(userPlaylists, activeSortOption, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            userPlaylists
        } else {
            userPlaylists.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        when (activeSortOption) {
            "A-Z" -> filtered.sortedBy { it.title.lowercase() }
            "Track count" -> filtered.sortedByDescending { it.trackCount }
            "Most played" -> filtered.sortedByDescending { it.tracks.sumOf { t -> playCounts[t.id] ?: 0 } }
            else -> filtered
        }
    }

    val totalPlaylistsCount = userPlaylists.size
    val totalTracksCount = userPlaylists.sumOf { it.trackCount }

    val listState = rememberLazyListState()
    LaunchedEffect(listState, onScrollStateChanged) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 25
        }
            .distinctUntilChanged()
            .collect { onScrollStateChanged(it) }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header (Reference Page 6) ---
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
                        text = "Playlist",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            letterSpacing = (-0.6).sp
                        ),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalPlaylistsCount Playlists • $totalTracksCount Tracks",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(BorderStroke(1.dp, borderColor), CircleShape)
                            .bouncyClickable { showCreateDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New playlist",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = cardBg,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.bouncyClickable { showSortMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort playlists",
                                    tint = textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sort",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = textPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            fontWeight = if (option == activeSortOption) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    onClick = {
                                        activeSortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Clean Full-Width Search Bar (Pixel-iOS Fusion Style) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isAmoled) 0.18f else 0.40f),
                                    Color.White.copy(alpha = if (isAmoled) 0.05f else 0.10f)
                                )
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search playlists & mixes...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = textSecondary
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }


        // --- Playlists header ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textPrimary
                )
                Text(
                    text = "${sortedPlaylists.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )
            }
        }

        if (sortedPlaylists.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No playlists yet" else "No matches",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Tap + to create your first playlist"
                        } else {
                            "Try a different name"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }
        } else {
            items(sortedPlaylists, key = { it.id }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accentColor = accentColor,
                    onClick = { onPlaylistSelected(playlist) }
                )
            }
        }

        // --- New playlist row ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .bouncyClickable { showCreateDialog = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(fieldBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "New Playlist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = accentColor
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    label = { Text("Name") },
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
                            Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show()
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

@Composable
private fun SmartCollectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .bouncyClickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.10f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = textSecondary
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val artUrl = playlist.artworkUrl ?: playlist.tracks.firstOrNull()?.artworkUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = playlist.title,
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
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Playlist • ${playlist.trackCount} songs",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = textSecondary,
                maxLines = 1
            )
        }

        if (playlist.isPinned) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Pinned",
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = textSecondary.copy(alpha = 0.55f),
            modifier = Modifier.size(12.dp)
        )
    }
}
