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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.DarkPrimaryCrimson
import com.masterwave.player.ui.theme.LightBackground
import com.masterwave.player.ui.theme.LightCardPink
import com.masterwave.player.ui.theme.LightPrimaryMaroon
import com.masterwave.player.ui.theme.LightSurface
import com.masterwave.player.ui.theme.LightSurfaceVariant
import com.masterwave.player.ui.theme.LightTextPrimary
import com.masterwave.player.ui.theme.LightTextSecondary
import com.masterwave.player.ui.theme.LightTextTertiary
import kotlinx.coroutines.launch

/**
 * Playlists Screen matching Screenshot 1.
 * Features:
 * - "Playlist" header with count badge ("X Playlists • Y Tracks").
 * - Circular "+" button for Import / Create + Pill "Sort" button.
 * - Search bar with adjacent pink "✨ Generator" button.
 * - Pinned playlists: "Liked Songs 📌", "Local songs 📌".
 * - YouTube and Spotify playlist importers with zero API key dependencies!
 */
@Composable
fun PlaylistsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onPlaylistSelected: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by repository.playlists.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val localTracks by repository.localTracks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportYouTubeDialog by remember { mutableStateOf(false) }
    var showImportSpotifyDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val totalTracks = playlists.sumOf { it.trackCount }
    val filteredPlaylists = if (searchQuery.isBlank()) {
        playlists
    } else {
        playlists.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Top Header Row ---
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
                            fontSize = 32.sp
                        ),
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlists.size} Playlists • $totalTracks Tracks",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = LightTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circular "+" button (Screenshot 1)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LightSurfaceVariant)
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add or Import Playlist",
                            tint = LightTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Sort pill button
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = LightSurfaceVariant,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Sort: Default", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = LightTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sort",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = LightTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Search & Generator Row ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search field pill
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search playlists...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = LightTextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = LightTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LightSurfaceVariant,
                        unfocusedContainerColor = LightSurfaceVariant,
                        disabledContainerColor = LightSurfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )

                // Generator pink pill button (Screenshot 1: [ ✨ Generator ])
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LightCardPink,
                    modifier = Modifier
                        .height(50.dp)
                        .clickable {
                            Toast.makeText(context, "Smart Playlist Generator ready", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LightPrimaryMaroon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generator",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = LightPrimaryMaroon
                        )
                    }
                }
            }
        }

        // --- 3. Playlist Cards List ---
        items(filteredPlaylists, key = { it.id }) { playlist ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LightSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistSelected(playlist) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Box
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = LightTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Title & Metadata
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = LightTextPrimary
                            )
                            if (playlist.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = DarkPrimaryCrimson,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${playlist.trackCount} tracks • ${playlist.createdAt}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = LightTextSecondary
                        )
                    }

                    // 3-dots Menu
                    IconButton(onClick = {
                        Toast.makeText(context, "${playlist.title} options", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = LightTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // --- Action Dialogs for Adding & Importing Playlists ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add / Import Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showAddDialog = false
                            showImportYouTubeDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightPrimaryMaroon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import from YouTube (URL / ID)")
                    }
                    Button(
                        onClick = {
                            showAddDialog = false
                            showImportSpotifyDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import from Spotify (Link)")
                    }
                    Button(
                        onClick = {
                            showAddDialog = false
                            showCreateDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Custom Playlist", color = LightTextPrimary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // YouTube Import Dialog
    if (showImportYouTubeDialog) {
        var ytInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportYouTubeDialog = false },
            title = { Text("Import YouTube Playlist") },
            text = {
                Column {
                    Text("Paste YouTube playlist URL or ID (zero API keys):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ytInput,
                        onValueChange = { ytInput = it },
                        placeholder = { Text("https://www.youtube.com/playlist?list=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ytInput.isNotBlank()) {
                            isImporting = true
                            scope.launch {
                                val res = repository.importYouTubePlaylist(ytInput.trim())
                                isImporting = false
                                showImportYouTubeDialog = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Imported: ${res.getOrNull()?.title}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Import failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimaryMaroon)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportYouTubeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Spotify Import Dialog
    if (showImportSpotifyDialog) {
        var spInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportSpotifyDialog = false },
            title = { Text("Import Spotify Playlist") },
            text = {
                Column {
                    Text("Paste Spotify playlist link (zero credentials needed):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spInput,
                        onValueChange = { spInput = it },
                        placeholder = { Text("https://open.spotify.com/playlist/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (spInput.isNotBlank()) {
                            isImporting = true
                            scope.launch {
                                val res = repository.importSpotifyPlaylist(spInput.trim())
                                isImporting = false
                                showImportSpotifyDialog = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Imported: ${res.getOrNull()?.title}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Import failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportSpotifyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Custom Playlist Dialog
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            repository.createCustomPlaylist(newTitle.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimaryMaroon)
                ) {
                    Text("Create")
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
