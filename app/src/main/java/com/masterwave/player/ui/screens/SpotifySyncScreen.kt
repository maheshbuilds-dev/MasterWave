package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.WebAuthDialog
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

@Composable
fun SpotifySyncScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val cloudManager = remember { CloudAccountManager.getInstance(context, repository) }
    val userProfile by cloudManager.userProfile.collectAsState()
    val playlists by repository.playlists.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncCanvas by remember { mutableStateOf(true) }
    var syncLikedSongs by remember { mutableStateOf(true) }
    var playlistUrlInput by remember { mutableStateOf("") }
    var isImportingUrl by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val spotifyGreen = Color(0xFF1DB954)
    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFAF0)
    val cardBg = if (isAmoled) Color(0xFF101016) else Color(0xFFF5F0E0)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)

    val spotifyPlaylists = remember(playlists) {
        playlists.filter { it.source == "Spotify" || it.id.startsWith("sp_") || it.id.startsWith("spotify_") }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spotify Connection & Sync",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textPrimary
                )
            }
        }

        // Spotify Account Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(spotifyGreen.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.photoUrl != null && userProfile.isSignedIn && userProfile.provider == "Spotify") {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = userProfile.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Spotify",
                                    tint = spotifyGreen,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userProfile.isSignedIn && userProfile.provider == "Spotify") userProfile.displayName else if (cloudManager.isSpotifyAuthenticated) "Spotify Connected" else "Connect Spotify",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Text(
                                text = if (cloudManager.isSpotifyAuthenticated) "Real Library & Liked Songs Active" else "Sign in to sync your Spotify library",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!cloudManager.isSpotifyAuthenticated) {
                                    showAuthDialog = true
                                } else {
                                    cloudManager.signOutSpotify()
                                    Toast.makeText(context, "Disconnected from Spotify", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cloudManager.isSpotifyAuthenticated) Color(0x33888898) else spotifyGreen
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (cloudManager.isSpotifyAuthenticated) "Disconnect" else "Connect Spotify",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (!isSyncing) {
                                    isSyncing = true
                                    scope.launch {
                                        val syncedList = cloudManager.syncRealSpotifyLibrary()
                                        isSyncing = false
                                        if (syncedList.isNotEmpty()) {
                                            Toast.makeText(context, "Synced ${syncedList.size} playlists & liked songs from Spotify!", Toast.LENGTH_SHORT).show()
                                        } else if (!cloudManager.isSpotifyAuthenticated) {
                                            Toast.makeText(context, "Connect Spotify first, or paste a playlist link below", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Spotify library is up to date", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isSyncing) "Syncing..." else "Sync Library",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section: Instant Spotify Playlist Link / ID Importer
        item {
            Text(
                text = "Import Any Spotify Playlist by Link / ID",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accentColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste any Spotify playlist URL or ID (e.g. https://open.spotify.com/playlist/...)",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    OutlinedTextField(
                        value = playlistUrlInput,
                        onValueChange = { playlistUrlInput = it },
                        placeholder = { Text("https://open.spotify.com/playlist/...", color = textSecondary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0x33888898),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val input = playlistUrlInput.trim()
                            if (input.isNotBlank()) {
                                isImportingUrl = true
                                scope.launch {
                                    val result = cloudManager.importSpotifyPlaylistUrl(input)
                                    isImportingUrl = false
                                    result.onSuccess { pl ->
                                        playlistUrlInput = ""
                                        Toast.makeText(context, "Imported \"${pl.title}\" (${pl.trackCount} tracks)!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Import failed: ${err.message ?: "Invalid link"}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please paste a playlist URL or ID", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImportingUrl
                    ) {
                        if (isImportingUrl) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isImportingUrl) "Importing Songs..." else "Import Playlist",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Sync Options
        item {
            Text(
                text = "Spotify Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accentColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Sync Liked Songs & Tracks", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Include heart-liked Spotify tracks in Library", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = syncLikedSongs,
                            onCheckedChange = { syncLikedSongs = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = spotifyGreen)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Spotify Canvas Looping", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Stream animated artwork loops when available", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = syncCanvas,
                            onCheckedChange = { syncCanvas = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = spotifyGreen)
                        )
                    }
                }
            }
        }

        // Section: Synced Spotify Playlists
        if (spotifyPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = "Synced Spotify Playlists (${spotifyPlaylists.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = accentColor
                    )
                )
            }

            items(spotifyPlaylists, key = { it.id }) { pl ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E28)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!pl.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = pl.artworkUrl,
                                    contentDescription = pl.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = spotifyGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pl.trackCount} tracks • Spotify",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }

                        IconButton(onClick = {
                            if (pl.tracks.isNotEmpty()) {
                                player.playTrack(pl.tracks.first(), pl.tracks)
                                Toast.makeText(context, "Playing ${pl.title}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Playlist has no tracks loaded", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play",
                                tint = spotifyGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    WebAuthDialog(
        isOpen = showAuthDialog,
        providerTitle = "Spotify Account Login",
        initialUrl = "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com",
        successDomain = "open.spotify.com",
        onDismiss = { showAuthDialog = false },
        onAuthCaptured = { cookies ->
            cloudManager.saveSpotifyAuth(cookies)
            showAuthDialog = false
            Toast.makeText(context, "Spotify Account Connected! Syncing library...", Toast.LENGTH_SHORT).show()
            scope.launch {
                cloudManager.syncRealSpotifyLibrary()
            }
        }
    )
}
