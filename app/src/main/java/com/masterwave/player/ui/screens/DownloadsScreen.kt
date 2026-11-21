package com.masterwave.player.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeState
import java.io.File

@Composable
fun DownloadsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, repository.okHttpClient, repository)
    }
    val downloadedTracks by downloadManager.downloadedTracks.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }

    val deduplicatedTracks = remember(downloadedTracks) {
        downloadedTracks.distinctBy {
            "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}"
        }
    }

    val filteredTracks = if (searchQuery.isBlank()) {
        deduplicatedTracks
    } else {
        deduplicatedTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalSizeBytes = remember(deduplicatedTracks) {
        deduplicatedTracks.sumOf {
            try {
                File(it.streamUrl).length()
            } catch (e: Exception) {
                0L
            }
        }
    }
    val totalSizeMb = String.format("%.1f MB", totalSizeBytes / (1024f * 1024f))

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFAF0)
    val cardBg = if (isAmoled) Color(0xFF101018) else Color(0xFFF5F0E0)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
                        text = "Downloads",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "${downloadedTracks.size} tracks • $totalSizeMb • Offline storage",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search downloaded tracks...",
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
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    disabledContainerColor = cardBg,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .height(50.dp)
            )

            // Play & Shuffle Row
            if (filteredTracks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            player.playPlaylist(filteredTracks, 0)
                            Toast.makeText(context, "Playing downloads", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = CircleShape,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play All",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = cardBg,
                        modifier = Modifier
                            .height(44.dp)
                            .clickable {
                                player.playPlaylist(filteredTracks.shuffled(), 0)
                                Toast.makeText(context, "Shuffling downloads", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shuffle",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = textPrimary
                            )
                        }
                    }
                }
            }

            // Track List / Empty State
            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isBlank()) "No Downloaded Tracks" else "No matching downloads",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isBlank())
                                "Download tracks from Discover or Search to listen anywhere offline without internet."
                            else
                                "Try searching for a different track title or artist.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    itemsIndexed(filteredTracks) { index, track ->
                        val isCurrent = currentTrack?.id == track.id
                        val fileSize = remember(track.streamUrl) {
                            try {
                                val bytes = File(track.streamUrl).length()
                                String.format("%.1f MB", bytes / (1024f * 1024f))
                            } catch (e: Exception) {
                                "Offline"
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { player.playPlaylist(filteredTracks, index) }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            val artworkModel = remember(track.artworkUrl, track.streamUrl) {
                                val url = track.artworkUrl
                                when {
                                    url.isNullOrBlank() -> null
                                    url.startsWith("http://") || url.startsWith("https://") -> url
                                    url.startsWith("content://") -> Uri.parse(url)
                                    url.startsWith("file://") -> Uri.parse(url)
                                    else -> File(url)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                if (artworkModel != null) {
                                    AsyncImage(
                                        model = artworkModel,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )
                                }

                                // Offline badge indicator
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadDone,
                                        contentDescription = "Downloaded",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.5.sp
                                    ),
                                    color = if (isCurrent) accentColor else textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "•", color = textSecondary, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = fileSize,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = accentColor
                                    )
                                }
                            }

                            // Delete button
                            IconButton(onClick = { trackToDelete = track }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Download",
                                    tint = textSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        trackToDelete?.let { track ->
            AlertDialog(
                onDismissRequest = { trackToDelete = null },
                title = { Text("Delete Download?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete \"${track.title}\" from your offline storage?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            downloadManager.deleteDownload(track)
                            trackToDelete = null
                            Toast.makeText(context, "Deleted from offline storage", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
