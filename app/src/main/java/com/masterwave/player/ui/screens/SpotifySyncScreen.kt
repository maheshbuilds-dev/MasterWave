package com.masterwave.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.data.spotify.SpotifyAuthManager
import com.masterwave.player.data.spotify.SpotifyConfig
import com.masterwave.player.data.spotify.SpotifyErrorType
import com.masterwave.player.data.spotify.SpotifyFileImporter
import com.masterwave.player.data.spotify.SpotifyPlaylistSummary
import com.masterwave.player.data.spotify.SpotifyResult
import com.masterwave.player.data.spotify.SpotifyWebApiClient
import com.masterwave.player.playback.MasterWavePlayer
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
    val scope = rememberCoroutineScope()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val spotifyGreen = Color(0xFF1DB954)
    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFAF9F6)
    val cardBg = if (isAmoled) Color(0xFF121218) else Color(0xFFFFFFFF)
    val cardBorder = if (isAmoled) Color(0x22FFFFFF) else Color(0xFFE5E7EB)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF101828)
    val textSecondary = if (isAmoled) Color(0xFF9E9EA8) else Color(0xFF6B7280)

    val authManager = remember { SpotifyAuthManager.getInstance(context) }
    val webApiClient = remember { SpotifyWebApiClient(authManager) }

    val isConnected by authManager.isConnected.collectAsState()
    val userProfile by authManager.userProfile.collectAsState()
    val authError by authManager.authError.collectAsState()
    val isAuthenticating by authManager.isAuthenticating.collectAsState()

    val playlists by repository.playlists.collectAsState()
    val spotifyPlaylists = remember(playlists) {
        playlists.filter { it.source == "Spotify" || it.id == "spotify_liked_songs" || it.id.startsWith("sp_") }
    }

    // Remote fetched playlists state
    val remotePlaylists = remember { mutableStateListOf<SpotifyPlaylistSummary>() }
    val selectedPlaylists = remember { mutableStateMapOf<String, Boolean>() }
    var syncLikedSongs by remember { mutableStateOf(true) }

    var isLoadingPlaylists by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf("") }
    var importProgressFraction by remember { mutableFloatStateOf(0f) }

    var showSetupGuideDialog by remember { mutableStateOf(false) }
    var showClientIdDialog by remember { mutableStateOf(false) }
    var clientIdInput by remember { mutableStateOf(SpotifyConfig.getClientId()) }
    var errorMessageDialog by remember { mutableStateOf<String?>(null) }

    // File picker launcher for CSV / JSON
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                importStatusMessage = "Reading and parsing playlist file..."
                val result = SpotifyFileImporter.importFromFile(uri)
                isImporting = false
                result.onSuccess { playlist ->
                    repository.importSpotifyPlaylist(playlist)
                    Toast.makeText(context, "Imported \"${playlist.title}\" (${playlist.trackCount} tracks)!", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    errorMessageDialog = "File Import Error: ${err.message ?: "Failed parsing playlist file."}"
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top App Bar ---
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
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import from Spotify",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Official Web API (PKCE) • Read-only metadata",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }
                IconButton(onClick = { showSetupGuideDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Setup Guide",
                        tint = spotifyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = {
                    clientIdInput = SpotifyConfig.getClientId()
                    showClientIdDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // --- 2. Auth Status & Account Card ---
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(spotifyGreen.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isConnected && !userProfile?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userProfile?.avatarUrl,
                                    contentDescription = userProfile?.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Spotify",
                                    tint = spotifyGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isConnected) (userProfile?.displayName ?: "Spotify Connected") else "Connect Spotify Account",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isConnected) {
                                    val tier = userProfile?.product?.replaceFirstChar { it.uppercase() } ?: "Account"
                                    "$tier • Official Web API Active"
                                } else {
                                    "Authorization Code with PKCE • Free & Secure"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = authError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isConnected) {
                            Button(
                                onClick = {
                                    if (!SpotifyConfig.isConfigured()) {
                                        clientIdInput = SpotifyConfig.getClientId()
                                        showClientIdDialog = true
                                    } else {
                                        authManager.startAuthorizationFlow(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isAuthenticating) "Connecting..." else "Connect Spotify",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { showSetupGuideDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text(
                                    text = "Setup App",
                                    color = textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    authManager.disconnect()
                                    remotePlaylists.clear()
                                    Toast.makeText(context, "Disconnected from Spotify", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text(
                                    text = "Disconnect",
                                    color = textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoadingPlaylists = true
                                        val res = webApiClient.fetchUserPlaylists()
                                        isLoadingPlaylists = false
                                        when (res) {
                                            is SpotifyResult.Success -> {
                                                remotePlaylists.clear()
                                                remotePlaylists.addAll(res.data)
                                                selectedPlaylists.clear()
                                                res.data.forEach { selectedPlaylists[it.id] = true }
                                                Toast.makeText(context, "Found ${res.data.size} playlists!", Toast.LENGTH_SHORT).show()
                                            }
                                            is SpotifyResult.Error -> {
                                                errorMessageDialog = when (res.type) {
                                                    SpotifyErrorType.NO_INTERNET -> "No internet connection. Please check your network and try again."
                                                    SpotifyErrorType.DEV_MODE_RESTRICTION_403 -> "Spotify 403 (Development Mode restriction): In Development Mode, only playlists you own or collaborate on can be imported, and your account must be registered in the Spotify Developer Dashboard."
                                                    SpotifyErrorType.UNAUTHORIZED -> "Session expired. Please reconnect to Spotify."
                                                    else -> res.message
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.3f),
                                enabled = !isLoadingPlaylists && !isImporting
                            ) {
                                if (isLoadingPlaylists) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isLoadingPlaylists) "Fetching..." else "Fetch Playlists",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Import Progress Bar (Active during import) ---
        if (isImporting) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Importing Spotify Metadata...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            CircularProgressIndicator(
                                color = spotifyGreen,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = importStatusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { importProgressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = spotifyGreen,
                            trackColor = spotifyGreen.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // --- 4. Remote Playlists Selection & Import Section (When fetched) ---
        if (isConnected && remotePlaylists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Playlists to Import (${remotePlaylists.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = textPrimary
                        )
                        Text(
                            text = "Pick the playlists you want to save to MasterWave",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                    TextButton(onClick = {
                        val allSelected = selectedPlaylists.values.all { it }
                        remotePlaylists.forEach { selectedPlaylists[it.id] = !allSelected }
                    }) {
                        val allSelected = selectedPlaylists.values.all { it }
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All",
                            color = spotifyGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Liked Songs Selection Checkbox Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { syncLikedSongs = !syncLikedSongs }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEC4899).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include Spotify Liked Songs",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                                color = textPrimary
                            )
                            Text(
                                text = "Imports all saved tracks into Liked Songs & Spotify Liked collection",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = textSecondary
                            )
                        }
                        Checkbox(
                            checked = syncLikedSongs,
                            onCheckedChange = { syncLikedSongs = it },
                            colors = CheckboxDefaults.colors(checkedColor = spotifyGreen)
                        )
                    }
                }
            }

            // List of Remote Playlists
            items(remotePlaylists, key = { it.id }) { pl ->
                val isChecked = selectedPlaylists[pl.id] ?: false
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, if (isChecked) spotifyGreen.copy(alpha = 0.4f) else cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlaylists[pl.id] = !isChecked }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF202028)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!pl.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = pl.artworkUrl,
                                    contentDescription = pl.name,
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
                                text = pl.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pl.trackCount} tracks • By ${pl.ownerName ?: "Spotify"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { selectedPlaylists[pl.id] = it },
                            colors = CheckboxDefaults.colors(checkedColor = spotifyGreen)
                        )
                    }
                }
            }

            // Start Import Button
            item {
                val selectedCount = selectedPlaylists.values.count { it }
                Button(
                    onClick = {
                        val targets = remotePlaylists.filter { selectedPlaylists[it.id] == true }
                        if (targets.isEmpty() && !syncLikedSongs) {
                            Toast.makeText(context, "Select at least one playlist or Liked Songs to import", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isImporting = true
                            var importedPlaylistsCount = 0
                            val totalSteps = targets.size + (if (syncLikedSongs) 1 else 0)
                            var currentStep = 0

                            // 1. Import Playlists
                            for (pl in targets) {
                                currentStep++
                                importProgressFraction = currentStep.toFloat() / totalSteps.toFloat()
                                importStatusMessage = "Importing playlist $currentStep of $totalSteps: \"${pl.name}\"..."

                                val trackRes = webApiClient.fetchPlaylistTracks(pl.id)
                                when (trackRes) {
                                    is SpotifyResult.Success -> {
                                        val domainPlaylist = Playlist(
                                            id = "sp_pl_${pl.id}",
                                            title = pl.name,
                                            description = pl.description.ifBlank { "Imported from Spotify" },
                                            trackCount = trackRes.data.size,
                                            tracks = trackRes.data,
                                            artworkUrl = pl.artworkUrl ?: trackRes.data.firstOrNull()?.artworkUrl,
                                            source = "Spotify",
                                            isPinned = false
                                        )
                                        repository.importSpotifyPlaylist(domainPlaylist)
                                        importedPlaylistsCount++
                                    }
                                    is SpotifyResult.Error -> {
                                        if (trackRes.type == SpotifyErrorType.DEV_MODE_RESTRICTION_403) {
                                            errorMessageDialog = "Spotify Development Mode Notice: Playlist \"${pl.name}\" could not be read because in Development Mode Spotify only permits reading playlists you own or collaborate on."
                                        }
                                    }
                                }
                            }

                            // 2. Import Liked Songs
                            var likedSongsCount = 0
                            if (syncLikedSongs) {
                                currentStep++
                                importProgressFraction = 0.95f
                                importStatusMessage = "Importing Liked Songs from Spotify..."
                                val likedRes = webApiClient.fetchLikedSongs()
                                if (likedRes is SpotifyResult.Success) {
                                    likedSongsCount = repository.importSpotifyLikedSongs(likedRes.data)
                                }
                            }

                            isImporting = false
                            importProgressFraction = 1f
                            importStatusMessage = "Import completed successfully!"

                            val toastMsg = buildString {
                                append("Imported $importedPlaylistsCount playlists")
                                if (syncLikedSongs) append(" and $likedSongsCount Liked Songs")
                                append(" from Spotify!")
                            }
                            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isImporting && (selectedCount > 0 || syncLikedSongs)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (syncLikedSongs) "Import Selected ($selectedCount playlists + Liked)" else "Import Selected ($selectedCount playlists)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // --- 5. Fallback: Import from File (CSV / JSON) ---
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Offline Fallback: Import from File",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Don't want to connect API? Import exported playlist files directly (Exportify CSV or JSON).",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exportify CSV or JSON file",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Creates read-only metadata playlist in MasterWave",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isImporting
                        ) {
                            Text(
                                text = "Select Playlist File (.csv / .json)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 6. Currently Synced / Imported Spotify Playlists ---
        if (spotifyPlaylists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Imported Spotify Playlists (${spotifyPlaylists.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = textPrimary
                    )
                }
            }

            items(spotifyPlaylists, key = { it.id }) { pl ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(spotifyGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Spotify Track • Metadata Only",
                                        color = spotifyGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${pl.trackCount} tracks",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = textSecondary
                                )
                            }
                        }

                        IconButton(onClick = {
                            val matchedLocal = pl.tracks.firstOrNull { it.isLocal && !it.streamUrl.isNullOrBlank() }
                            if (matchedLocal != null) {
                                player.playTrack(matchedLocal, pl.tracks.filter { it.isLocal })
                                Toast.makeText(context, "Playing matched tracks from ${pl.title}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Metadata-only playlist. Tap individual songs to match with local audio files.", Toast.LENGTH_LONG).show()
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

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- Dialog 1: Setup Guide & Developer App Instructions ---
    if (showSetupGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSetupGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = spotifyGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Spotify Developer Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "To import directly from the official Spotify Web API using PKCE, follow these quick steps (100% Free):",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    Text(
                        text = "1. Open developer.spotify.com/dashboard and log in with your Spotify account.\n" +
                                "2. Click \"Create App\". Enter Name (e.g., MasterWave) and App description.\n" +
                                "3. In \"Redirect URIs\", add:\n" +
                                "   masterwave://spotify-callback\n" +
                                "4. Which API/SDKs are you planning to use? Select \"Web API\".\n" +
                                "5. Check terms and click Save.\n" +
                                "6. In App Settings, copy your Client ID and paste it into MasterWave Settings.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = textPrimary
                    )

                    HorizontalDivider(color = cardBorder)

                    Text(
                        text = "Official February 2026 Guidelines Notice:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary
                    )
                    Text(
                        text = "• App owner requires an active Spotify Premium subscription.\n" +
                                "• In Development Mode, up to 5 authorized users are supported.\n" +
                                "• Playlist items can be read for playlists you own or collaborate on.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = textSecondary
                    )

                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("Redirect URI", SpotifyConfig.REDIRECT_URI)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "Copied Redirect URI to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x221DB954)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = spotifyGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Redirect URI", color = spotifyGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSetupGuideDialog = false }) {
                    Text("Got It", color = spotifyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- Dialog 2: Spotify Client ID Configuration ---
    if (showClientIdDialog) {
        AlertDialog(
            onDismissRequest = { showClientIdDialog = false },
            title = {
                Text("Spotify Client ID", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your Spotify Developer Client ID. No client secret is ever required (Authorization Code with PKCE).",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it.trim() },
                        label = { Text("Client ID") },
                        placeholder = { Text("e.g. 4a8b7c9d...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spotifyGreen,
                            focusedLabelColor = spotifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18888888))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Redirect URI: ${SpotifyConfig.REDIRECT_URI}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Redirect URI", SpotifyConfig.REDIRECT_URI)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, "Copied Redirect URI!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = clientIdInput.trim()
                        if (trimmed.isNotBlank()) {
                            SpotifyConfig.setClientId(trimmed)
                            showClientIdDialog = false
                            Toast.makeText(context, "Saved Spotify Client ID", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a valid Client ID", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClientIdDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // --- Dialog 3: Error Message Dialog ---
    if (errorMessageDialog != null) {
        AlertDialog(
            onDismissRequest = { errorMessageDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Spotify Sync Notice", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    text = errorMessageDialog ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { errorMessageDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
