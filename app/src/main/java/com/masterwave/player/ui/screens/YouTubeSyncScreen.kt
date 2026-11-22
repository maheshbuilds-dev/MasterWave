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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.WebAuthDialog
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun YouTubeSyncScreen(
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

    var isSyncing by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var autoSyncLiked by remember { mutableStateOf(true) }
    var highResArtworkSync by remember { mutableStateOf(true) }
    var playlistUrlInput by remember { mutableStateOf("") }
    var isImportingUrl by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFAF0)
    val cardBg = if (isAmoled) Color(0xFF101016) else Color(0xFFF5F0E0)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)
    val ytRed = Color(0xFFFF0000)

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
                    text = "YouTube Music & Sync",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textPrimary
                )
            }
        }

        // Account Status Card
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
                                .background(ytRed.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.photoUrl != null && userProfile.isSignedIn) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = userProfile.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "YouTube Music",
                                    tint = ytRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userProfile.isSignedIn) userProfile.displayName else "YouTube Music Cloud",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Text(
                                text = if (userProfile.isSignedIn) userProfile.email else "Cloud Sync Ready • No password needed",
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
                                if (!cloudManager.isYtAuthenticated) {
                                    showAuthDialog = true
                                } else {
                                    cloudManager.signOutYouTube()
                                    Toast.makeText(context, "Signed out of YouTube Music", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cloudManager.isYtAuthenticated) Color(0x33888898) else ytRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (cloudManager.isYtAuthenticated) "Disconnect" else "Sign In with Google",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (!isSyncing) {
                                    isSyncing = true
                                    scope.launch {
                                        val syncedList = cloudManager.syncRealYouTubeMusicLibrary()
                                        isSyncing = false
                                        if (syncedList.isNotEmpty()) {
                                            Toast.makeText(context, "Synced ${syncedList.size} real playlists from your YouTube Music!", Toast.LENGTH_SHORT).show()
                                        } else if (!cloudManager.isYtAuthenticated) {
                                            Toast.makeText(context, "Sign in with Google first, or paste a playlist link below", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Library up to date", Toast.LENGTH_SHORT).show()
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

        // Section: Instant YouTube Playlist Link / ID Importer
        item {
            Text(
                text = "Import Any Playlist by Link / ID",
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
                        text = "Paste any YouTube / YouTube Music playlist URL or ID (e.g. https://music.youtube.com/playlist?list=...)",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    OutlinedTextField(
                        value = playlistUrlInput,
                        onValueChange = { playlistUrlInput = it },
                        placeholder = { Text("https://music.youtube.com/playlist?list=...", color = textSecondary.copy(alpha = 0.6f)) },
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
                                    val result = cloudManager.importYouTubePlaylistUrl(input)
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
                        colors = ButtonDefaults.buttonColors(containerColor = ytRed),
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

        // Section: Sync Settings
        item {
            Text(
                text = "Cloud Sync Options",
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
                            Text(text = "Auto-sync Liked Music", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Automatically mirror YouTube thumbs-up tracks", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = autoSyncLiked,
                            onCheckedChange = { autoSyncLiked = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ytRed)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "High-Res Artwork Upscaling", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Fetch 1080x1080 studio album covers", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = highResArtworkSync,
                            onCheckedChange = { highResArtworkSync = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ytRed)
                        )
                    }
                }
            }
        }

        // Section: Synced Playlists
        item {
            Text(
                text = "Your Synced Playlists (${playlists.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accentColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(playlists) { playlist ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ytRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = ytRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = playlist.title, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(text = "${playlist.trackCount} tracks • Synced from Cloud", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                    Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    WebAuthDialog(
        isOpen = showAuthDialog,
        providerTitle = "YouTube Music Sign In",
        initialUrl = "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F&hl=en",
        successDomain = "music.youtube.com",
        onDismiss = { showAuthDialog = false },
        onAuthCaptured = { cookies ->
            cloudManager.saveYouTubeAuth(cookies)
            showAuthDialog = false
            Toast.makeText(context, "YouTube Music Connected! Fetching your playlists...", Toast.LENGTH_SHORT).show()
            scope.launch {
                isSyncing = true
                val synced = cloudManager.syncRealYouTubeMusicLibrary()
                isSyncing = false
                if (synced.isNotEmpty()) {
                    Toast.makeText(context, "Successfully synced ${synced.size} YouTube playlists!", Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}
