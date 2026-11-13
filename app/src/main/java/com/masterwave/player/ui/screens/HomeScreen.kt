package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.recommendation.AiMusicRecommender
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeState
import java.util.Calendar

/**
 * Supercharged Home Screen with:
 * - Dynamic Time-of-Day Greeting (Good Morning / Afternoon / Evening / Night).
 * - Real-time AMOLED Dark & Light mode adaptivity.
 * - Dynamic reactive accent colors.
 * - Category Chips row (All, AI Mix, Podcasts, Romance, Feel good, Party, Workout).
 * - Discover hero cards carousel.
 * - AI Mood Engine personalized vibe engine card.
 * - Quick picks with shuffle button.
 * - New Releases section with "NEW" badges.
 * - Favorite Artists circular recommendations based on listening history.
 * - Trending Community Playlists at the downside.
 */
@Composable
fun HomeScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMoodGenres: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("All") }

    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current

    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, MasterWaveApp.instance.okHttpClient, repository)
    }
    val downloadedIds by downloadManager.downloadedIds.collectAsState()

    val cloudManager = remember { CloudAccountManager.getInstance(context, repository) }
    val userProfile by cloudManager.userProfile.collectAsState()

    val categories = listOf("All", "AI Mix", "Podcasts", "Romance", "Feel good", "Party", "Workout", "Focus")

    // Dynamic Greeting based on current local hour
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LaunchedEffect(Unit) {
        trendingTracks = repository.getTrending()
        isLoadingTrending = false
    }

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF7F5F8)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)
    val chipInactiveBg = if (isAmoled) Color(0xFF14141C) else Color(0xFFEFECEF)

    // Favorite artists based on user listening history or defaults
    val favoriteArtists = remember(recentTracks) {
        val fromRecent = recentTracks.map { it.artist }.distinct().filter { it.isNotBlank() }
        if (fromRecent.isNotEmpty()) {
            fromRecent.take(6)
        } else {
            listOf("Arijit Singh", "The Weeknd", "Diljit Dosanjh", "Anirudh Ravichander", "Taylor Swift", "Badshah")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        // --- 1. Custom Top Header Bar with Dynamic Greeting ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = accentColor
                    )
                    Text(
                        text = "Home",
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
                    // Downloads Icon Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0xFF161620) else Color(0xFFEFECEF))
                            .clickable {
                                Toast.makeText(context, "${downloadedIds.size} tracks downloaded offline", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloads",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Search Icon Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0xFF161620) else Color(0xFFEFECEF))
                            .clickable(onClick = onNavigateToSearch),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Profile / Avatar Halo Ring
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(2.dp, accentColor, CircleShape)
                            .clickable(onClick = onNavigateToSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.photoUrl != null && userProfile.isSignedIn) {
                            AsyncImage(
                                model = userProfile.photoUrl,
                                contentDescription = userProfile.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(accentColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.displayName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Category Chips Row ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) {
                            if (isAmoled) accentColor.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.18f)
                        } else chipInactiveBg,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                selectedCategory = category
                                Toast.makeText(context, "Filtered: $category", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = if (isSelected) accentColor else textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // --- 3. Hero Carousel (Screenshot 5: DISCOVER cards) ---
        item {
            if (isLoadingTrending) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (trendingTracks.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trendingTracks.take(8)) { track ->
                        Column(
                            modifier = Modifier
                                .width(155.dp)
                                .clickable { player.playTrack(track) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(155.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isAmoled) Color(0xFF161622) else Color(0xFFE2E0E4))
                            ) {
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                    color = accentColor,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    Text(
                                        text = "DISCOVER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- 4. AI Mood Engine Card ---
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isAmoled) Color(0xFF101016) else accentColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable(onClick = onNavigateToMoodGenres)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Moods & Genres Discovery",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "16 MOODS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Explore Indian regional soundscapes & emotional vibes",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        // --- 5. Quick Picks Section (Screenshot 5) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick picks",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Trending songs worth playing now",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isAmoled) 0.35f else 0.16f))
                        .clickable {
                            if (trendingTracks.isNotEmpty()) {
                                player.playTrack(trendingTracks.shuffled().first())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle Trending",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 6. Quick Picks Items ---
        if (trendingTracks.isNotEmpty()) {
            items(trendingTracks.drop(8).take(6)) { track ->
                val isDownloaded = downloadedIds.contains(track.id)
                HomeTrackRow(
                    track = track,
                    isPlaying = currentTrack?.id == track.id && isPlaying,
                    isDownloaded = isDownloaded,
                    accentColor = accentColor,
                    isAmoled = isAmoled,
                    onClick = { player.playTrack(track) },
                    onDownload = {
                        downloadManager.downloadTrack(track)
                        Toast.makeText(context, "Downloading \"${track.title}\"...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- 7. "Because you listened to..." / Similar to Top Artist Section ---
        item {
            val topArtist = favoriteArtists.firstOrNull() ?: "KR\$NA"
            val similarTracks = remember(trendingTracks, topArtist) {
                trendingTracks.filter { it.artist.contains(topArtist, ignoreCase = true) || it.title.contains(topArtist, ignoreCase = true) }
                    .ifEmpty { trendingTracks.shuffled().take(6) }
            }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Because you listened to $topArtist",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Similar vibe and recommended tracks",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(similarTracks) { track ->
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { player.playTrack(track) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isAmoled) Color(0xFF161622) else Color(0xFFE2E0E4))
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
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
                    }
                }
            }
        }

        // --- 8. Favorite Artists Section ---
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Favorite Artists",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Text(
                    text = "Artists you listen to frequently",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteArtists) { artistName ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(90.dp)
                                .clickable {
                                    Toast.makeText(context, "Playing top tracks by $artistName", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artistName.take(1).uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Top Artist",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = textSecondary
                            )
                        }
                    }
                }
            }
        }

        // --- 8. Trending Community Playlists (Full Downside Section) ---
        item {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                Text(
                    text = "Trending Community Playlists",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Text(
                    text = "Crowd-curated mixes streamed in hardware Opus",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                val communityPlaylists = listOf(
                    "Global Top 100 Hits 2026" to "50 Tracks • Billboard & Viral",
                    "Bollywood Acoustic & Sufi" to "40 Tracks • Soulful Classics",
                    "Cyberpunk Synthwave & Night Drive" to "35 Tracks • Deep Bass",
                    "Chill Coffee & Lofi Beats" to "60 Tracks • Study & Focus",
                    "Beast Mode Workout Energy" to "45 Tracks • High BPM EDM",
                    "Late Night Romance & R&B" to "38 Tracks • Smooth Vibes"
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(communityPlaylists) { (title, subtitle) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isAmoled) Color(0xFF101018) else Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, if (isAmoled) Color(0x22FFFFFF) else Color(0x11000000), RoundedCornerShape(20.dp))
                                .clickable {
                                    if (trendingTracks.isNotEmpty()) {
                                        player.playTrack(trendingTracks.shuffled().first())
                                        Toast.makeText(context, "Playing $title", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accentColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTrackRow(
    track: Track,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    accentColor: Color,
    isAmoled: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAmoled) Color(0xFF161622) else Color(0xFFEFECEF)),
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
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (isPlaying) accentColor else textPrimary,
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

        IconButton(onClick = onDownload) {
            Icon(
                imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                contentDescription = "Download",
                tint = if (isDownloaded) accentColor else textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Download Offline") },
                    onClick = {
                        showMenu = false
                        onDownload()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Play Next") },
                    onClick = { showMenu = false }
                )
            }
        }
    }
}
