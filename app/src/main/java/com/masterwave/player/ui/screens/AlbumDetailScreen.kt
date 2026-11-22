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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Album
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

/**
 * Dedicated Album & Movie Soundtrack Detail Screen:
 * - Shows all songs of the movie/album when user searches movie soundtrack or selects an album.
 * - Header with cover art, movie title, composers, year, track count.
 * - Play All & Shuffle buttons.
 * - Full numbered tracklist with seamless playback.
 */
@Composable
fun AlbumDetailScreen(
    album: Album,
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val favorites by repository.favorites.collectAsState()

    var tracks by remember { mutableStateOf(album.tracks) }
    var isLoading by remember { mutableStateOf(album.tracks.isEmpty()) }

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBg = if (isAmoled) Color(0xFF101018) else Color(0xFFF1F3F5)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF101828)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)

    LaunchedEffect(album.id, album.title) {
        if (tracks.isEmpty()) {
            isLoading = true
            scope.launch {
                val query = "${album.title} ${album.artist} soundtrack songs"
                val res = repository.search(query)
                tracks = if (res.songs.isNotEmpty()) res.songs else {
                    repository.search("${album.title} songs").songs
                }
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // --- Header Banner & Album Artwork ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    // Ambient blur backdrop
                    if (!album.thumbnailUrl.isNullOrBlank() || tracks.firstOrNull()?.artworkUrl != null) {
                        val art = album.thumbnailUrl ?: tracks.firstOrNull()?.artworkUrl
                        AsyncImage(
                            model = art,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(accentColor.copy(alpha = 0.2f))
                        )
                    }

                    // Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x77000000),
                                        Color(0x99000000),
                                        bgColor
                                    )
                                )
                            )
                    )

                    // Top Bar Back Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Squircle Poster
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            shadowElevation = 18.dp,
                            modifier = Modifier
                                .size(180.dp)
                        ) {
                            val art = album.thumbnailUrl ?: tracks.firstOrNull()?.artworkUrl
                            if (!art.isNullOrBlank()) {
                                AsyncImage(
                                    model = art,
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(accentColor.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Album Info & Metadata ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = album.artist.ifBlank { "Original Motion Picture Soundtrack" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${tracks.size.coerceAtLeast(album.trackCount)} Tracks • ${album.year ?: "Soundtrack"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Play All & Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    player.playPlaylist(tracks, 0)
                                    Toast.makeText(context, "Playing ${album.title}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play All",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    player.playPlaylist(tracks.shuffled(), 0)
                                    Toast.makeText(context, "Shuffling ${album.title}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAmoled) Color(0xFF1E1E2A) else Color(0xFFE2E0E6)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shuffle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // --- Track List Section ---
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
            } else {
                itemsIndexed(tracks) { index, track ->
                    val isTrackPlaying = isPlaying && currentTrack?.id == track.id
                    val isFav = favorites.any { it.id == track.id }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { player.playPlaylist(tracks, index) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track Number
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = if (isTrackPlaying) accentColor else textSecondary,
                            modifier = Modifier.width(28.dp)
                        )

                        // Track Artwork
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                        ) {
                            if (!track.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isTrackPlaying) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 14.5.sp
                                ),
                                color = if (isTrackPlaying) accentColor else textPrimary,
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

                        // Favorite Icon
                        IconButton(onClick = { repository.toggleFavorite(track) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) accentColor else textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
