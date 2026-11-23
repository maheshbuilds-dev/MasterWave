package com.masterwave.player.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.data.repository.PlayHistoryItem
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentOchre
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentPink
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.WebAccentViolet
import com.masterwave.player.ui.theme.rememberAppThemeColors
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Statistics Screen:
 * Comprehensive listening metrics, interactive metric cards (Plays, Tracks, Artists, Albums),
 * ranked charts, and 3-day full playback history.
 */
@Composable
fun StatsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {},
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val totalPlays by repository.totalPlays.collectAsState()
    val playCounts by repository.playCounts.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()
    val playHistory by repository.playHistory.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    val colors = rememberAppThemeColors()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current

    // Active Metric Tab: 0 = Plays (3-Day History), 1 = Tracks, 2 = Artists, 3 = Albums
    var activeMetricTab by remember { mutableIntStateOf(0) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Filter play history to strictly last 3 days
    val threeDaysAgo = remember { System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L) }
    val threeDayHistory = remember(playHistory) {
        val cutoff = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        playHistory.filter { it.playedAt >= cutoff }
    }

    val uniqueTracks = recentTracks.distinctBy { it.id }.size
    val uniqueArtists = recentTracks.map { it.artist }.distinct().size
    val uniqueAlbums = recentTracks.mapNotNull { it.album.ifBlank { null } }.distinct().size

    // Ranked list computations
    val topTracksRanked = remember(recentTracks, playCounts) {
        recentTracks.sortedByDescending { playCounts[it.id] ?: 1 }
    }

    val topArtistsRanked = remember(recentTracks, playCounts) {
        recentTracks.groupBy { it.artist }.map { (artist, tracks) ->
            Triple(artist, tracks.sumOf { playCounts[it.id] ?: 1 }, tracks.firstOrNull()?.artworkUrl)
        }.sortedByDescending { it.second }
    }

    val topAlbumsRanked = remember(recentTracks, playCounts) {
        recentTracks.filter { it.album.isNotBlank() }.groupBy { it.album }.map { (album, tracks) ->
            Triple(album, tracks.sumOf { playCounts[it.id] ?: 1 }, tracks.firstOrNull())
        }.sortedByDescending { it.second }
    }

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

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        return when {
            diff < 60 * 1000 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            hours < 48 -> "Yesterday"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top App Bar ---
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
                        text = "Statistics",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.6).sp,
                        color = colors.ink
                    )
                    Text(
                        text = "Listening analytics & 3-day history",
                        fontFamily = DisplayFontFamily,
                        fontSize = 12.5.sp,
                        color = colors.muted
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.ink,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSoft)
                                .border(1.5.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = colors.ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Interactive 2x2 Metric Grid (Plays, Tracks, Artists, Albums) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Plays Card & Tracks Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Plays Card (3-Day Playback History)
                    InteractiveMetricCard(
                        title = "Plays",
                        subtitle = "3-Day History",
                        value = if (threeDayHistory.isNotEmpty()) "${threeDayHistory.size}" else "$totalPlays",
                        icon = Icons.Default.BarChart,
                        accentColor = Color(0xFF165B33),
                        isSelected = activeMetricTab == 0,
                        colors = colors,
                        onClick = { activeMetricTab = 0 },
                        modifier = Modifier.weight(1f)
                    )

                    // Tracks Card (Top Tracks Ranked)
                    InteractiveMetricCard(
                        title = "Tracks",
                        subtitle = "Top Songs",
                        value = if (uniqueTracks > 0) "$uniqueTracks" else "—",
                        icon = Icons.Default.Audiotrack,
                        accentColor = WebAccentSky,
                        isSelected = activeMetricTab == 1,
                        colors = colors,
                        onClick = { activeMetricTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Artists Card & Albums Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Artists Card (Top Artists Ranked)
                    InteractiveMetricCard(
                        title = "Artists",
                        subtitle = "Top Artists",
                        value = if (uniqueArtists > 0) "$uniqueArtists" else "—",
                        icon = Icons.Default.People,
                        accentColor = WebAccentPink,
                        isSelected = activeMetricTab == 2,
                        colors = colors,
                        onClick = { activeMetricTab = 2 },
                        modifier = Modifier.weight(1f)
                    )

                    // Albums Card (Top Albums Ranked)
                    InteractiveMetricCard(
                        title = "Albums",
                        subtitle = "Top Albums",
                        value = if (uniqueAlbums > 0) "$uniqueAlbums" else "—",
                        icon = Icons.Default.Album,
                        accentColor = WebAccentPeach,
                        isSelected = activeMetricTab == 3,
                        colors = colors,
                        onClick = { activeMetricTab = 3 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 3. Dynamic Section Header Matching Active Metric Tab ---
        item {
            val sectionTitle = when (activeMetricTab) {
                0 -> "Plays History (Last 3 Days)"
                1 -> "Top Tracks"
                2 -> "Top Artists"
                3 -> "Top Albums"
                else -> "List"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = sectionTitle,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = colors.ink
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(WebAccentGreen)
                            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (activeMetricTab) {
                                0 -> "${threeDayHistory.size} PLAYS"
                                1 -> "${topTracksRanked.size} SONGS"
                                2 -> "${topArtistsRanked.size} ARTISTS"
                                3 -> "${topAlbumsRanked.size} ALBUMS"
                                else -> ""
                            },
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = Color(0xFF101828)
                        )
                    }
                }

                // Filter Dropdown Pill
                Box {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = colors.surfaceSoft,
                        border = BorderStroke(1.5.dp, colors.border),
                        modifier = Modifier.bouncyClickable { showFilterMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (activeMetricTab) {
                                    0 -> "Plays"
                                    1 -> "Tracks"
                                    2 -> "Artists"
                                    3 -> "Albums"
                                    else -> "Filter"
                                },
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Plays (3-Day History)", fontFamily = DisplayFontFamily) },
                            onClick = { activeMetricTab = 0; showFilterMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Top Tracks", fontFamily = DisplayFontFamily) },
                            onClick = { activeMetricTab = 1; showFilterMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Top Artists", fontFamily = DisplayFontFamily) },
                            onClick = { activeMetricTab = 2; showFilterMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Top Albums", fontFamily = DisplayFontFamily) },
                            onClick = { activeMetricTab = 3; showFilterMenu = false }
                        )
                    }
                }
            }
        }

        // --- 4. Content List for Selected Metric Tab ---
        when (activeMetricTab) {
            0 -> {
                // TAB 0: 3-Day Playback History (Every played song within the last 3 days)
                if (threeDayHistory.isEmpty()) {
                    item {
                        EmptyStatsCard(
                            message = "No playback history in the last 3 days",
                            subMessage = "Play any track in MasterWave to begin tracking your listening history.",
                            colors = colors
                        )
                    }
                } else {
                    itemsIndexed(threeDayHistory) { index, item ->
                        val isThisPlaying = currentTrack?.id == item.track.id && isPlaying
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = BorderStroke(1.5.dp, colors.hairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable { player.playTrack(item.track, threeDayHistory.map { it.track }) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Artwork with live equalizer overlay
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceSoft)
                                        .border(1.dp, colors.hairline, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!item.track.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.track.artworkUrl,
                                            contentDescription = item.track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = colors.muted)
                                    }

                                    if (isThisPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.track.title,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = if (isThisPlaying) Color(0xFF00E676) else colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.track.artist} • ${formatRelativeTime(item.playedAt)}",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceSoft)
                                        .border(1.dp, colors.hairline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = colors.ink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // TAB 1: Top Tracks Ranked by Play Count
                if (topTracksRanked.isEmpty()) {
                    item {
                        EmptyStatsCard(
                            message = "No top tracks yet",
                            subMessage = "Listen to songs to see your most played music ranked.",
                            colors = colors
                        )
                    }
                } else {
                    itemsIndexed(topTracksRanked) { index, track ->
                        val count = playCounts[track.id] ?: 1
                        val isThisPlaying = currentTrack?.id == track.id && isPlaying

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = BorderStroke(1.5.dp, colors.hairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable { player.playTrack(track, topTracksRanked) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank Number
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) WebAccentGreen else if (index == 1) WebAccentSky else if (index == 2) WebAccentPeach else colors.surfaceSoft)
                                        .border(1.dp, colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF101828)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Artwork
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceSoft),
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
                                        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = colors.muted)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = if (isThisPlaying) Color(0xFF00E676) else colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.muted,
                                        maxLines = 1
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.surfaceSoft)
                                        .border(1.dp, colors.hairline, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$count ${if (count == 1) "play" else "plays"}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = colors.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // TAB 2: Top Artists Ranked
                if (topArtistsRanked.isEmpty()) {
                    item {
                        EmptyStatsCard(
                            message = "No artist analytics yet",
                            subMessage = "Your top artists will automatically appear here as you listen.",
                            colors = colors
                        )
                    }
                } else {
                    itemsIndexed(topArtistsRanked) { index, (artistName, playsCount, artUrl) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = BorderStroke(1.5.dp, colors.hairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable {
                                    Toast.makeText(context, "Artist: $artistName ($playsCount plays)", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank Badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) WebAccentGreen else if (index == 1) WebAccentSky else if (index == 2) WebAccentPeach else colors.surfaceSoft)
                                        .border(1.dp, colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF101828)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Artist Avatar Circle
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(WebAccentViolet)
                                        .border(1.5.dp, colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!artUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = artUrl,
                                            contentDescription = artistName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = artistName.take(1).uppercase(),
                                            fontFamily = DisplayFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF101828)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artistName,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Top Artist • $playsCount total plays",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.muted
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = colors.muted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                // TAB 3: Top Albums Ranked
                if (topAlbumsRanked.isEmpty()) {
                    item {
                        EmptyStatsCard(
                            message = "No album analytics yet",
                            subMessage = "Album data will populate as you play songs from various albums.",
                            colors = colors
                        )
                    }
                } else {
                    itemsIndexed(topAlbumsRanked) { index, (albumName, playsCount, sampleTrack) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = BorderStroke(1.5.dp, colors.hairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable {
                                    if (sampleTrack != null) player.playTrack(sampleTrack)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank Badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) WebAccentGreen else if (index == 1) WebAccentSky else if (index == 2) WebAccentPeach else colors.surfaceSoft)
                                        .border(1.dp, colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF101828)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Album Artwork
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceSoft)
                                        .border(1.dp, colors.hairline, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!sampleTrack?.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = sampleTrack!!.artworkUrl,
                                            contentDescription = albumName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = colors.muted)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = albumName,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sampleTrack?.artist ?: "Unknown Artist"} • $playsCount plays",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.muted
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = colors.ink,
                                    modifier = Modifier.size(18.dp)
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
private fun InteractiveMetricCard(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    colors: com.masterwave.player.ui.theme.AppThemeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) (if (colors.isDark) Color(0xFF162E20) else Color(0xFFE8F5E9)) else colors.surfaceCard,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.5.dp,
            color = if (isSelected) Color(0xFF165B33) else colors.hairline
        ),
        shadowElevation = if (isSelected && !colors.isDark) 4.dp else 0.dp,
        modifier = modifier
            .height(130.dp)
            .bouncyClickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(1.5.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF101828),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF165B33))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontFamily = DisplayFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Column {
                Text(
                    text = value,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = colors.ink
                )
                Text(
                    text = "$title • $subtitle",
                    fontFamily = DisplayFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted
                )
            }
        }
    }
}

@Composable
private fun EmptyStatsCard(
    message: String,
    subMessage: String,
    colors: com.masterwave.player.ui.theme.AppThemeColors
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.5.dp, colors.hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceSoft)
                    .border(1.5.dp, colors.hairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = message,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.ink
            )
            Text(
                text = subMessage,
                fontFamily = DisplayFontFamily,
                fontSize = 12.5.sp,
                color = colors.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
