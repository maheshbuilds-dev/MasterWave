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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.theme.LightBackground
import com.masterwave.player.ui.theme.LightCardPink
import com.masterwave.player.ui.theme.LightPrimaryMaroon
import com.masterwave.player.ui.theme.LightSurface
import com.masterwave.player.ui.theme.LightSurfaceVariant
import com.masterwave.player.ui.theme.LightTextPrimary
import com.masterwave.player.ui.theme.LightTextSecondary
import com.masterwave.player.ui.theme.LightTextTertiary

/**
 * Statistics Screen matching Screenshot 0.
 * Features:
 * - Top App bar with "Statistics", Download, Search, Profile icons.
 * - Guest chip and Headphone status pill.
 * - Last.fm scrobble sync banner.
 * - Hero Plays card with deep maroon circular arrow button.
 * - 3 Metric cards: Tracks, Artists, Albums.
 * - "List" with "Most Played" dropdown filter.
 */
@Composable
fun StatsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPlays by repository.totalPlays.collectAsState()
    val playCounts by repository.playCounts.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("Most Played") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val uniqueTracks = recentTracks.distinctBy { it.id }.size
    val uniqueArtists = recentTracks.map { it.artist }.distinct().size
    val uniqueAlbums = recentTracks.mapNotNull { it.album.ifBlank { null } }.distinct().size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top App Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = LightTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {
                        Toast.makeText(context, "Downloads manager", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloads",
                            tint = LightTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LightTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account & Settings",
                            tint = LightTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Guest and Headphone Status Pills ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Guest pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LightSurfaceVariant
                ) {
                    Text(
                        text = "Guest",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = LightTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Headphone status pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LightSurfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Headphones",
                            tint = LightTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "--",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LightTextSecondary
                        )
                    }
                }
            }
        }

        // --- 3. Last.fm Sync Banner Card ---
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LightCardPink,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings() }
            ) {
                Text(
                    text = "Connect Last.fm in Settings to sync global scrobbles",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = LightTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        // --- 4. Main Plays Card & 3 Metric Sub-Cards (Enclosed container) ---
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = LightSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Big Hero Plays Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = LightCardPink,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 22.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.size(46.dp)) // balance center

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (totalPlays > 0) "$totalPlays" else "—",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = LightTextPrimary
                                )
                                Text(
                                    text = "Plays",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = LightTextSecondary
                                )
                            }

                            // Circular maroon arrow button (Screenshot 0)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(LightPrimaryMaroon)
                                    .clickable {
                                        Toast.makeText(context, "Full listening breakdown", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Details",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 3 Metric Sub-Cards in a row: Tracks, Artists, Albums
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tracks
                        MetricCard(
                            label = "Tracks",
                            value = if (uniqueTracks > 0) "$uniqueTracks" else "—",
                            modifier = Modifier.weight(1f)
                        )
                        // Artists
                        MetricCard(
                            label = "Artists",
                            value = if (uniqueArtists > 0) "$uniqueArtists" else "—",
                            modifier = Modifier.weight(1f)
                        )
                        // Albums
                        MetricCard(
                            label = "Albums",
                            value = if (uniqueAlbums > 0) "$uniqueAlbums" else "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- 5. "List" Section with "Most Played" Dropdown ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LightSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "List",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = LightTextPrimary
                        )

                        // Dropdown filter pill: [ 📊 Most Played ▾ ]
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = LightSurfaceVariant,
                                modifier = Modifier.clickable { showFilterMenu = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = LightTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedFilter,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = LightTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = LightTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Most Played") },
                                    onClick = {
                                        selectedFilter = "Most Played"
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Recently Played") },
                                    onClick = {
                                        selectedFilter = "Recently Played"
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Top Artists") },
                                    onClick = {
                                        selectedFilter = "Top Artists"
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Content list or empty placeholder
                    if (recentTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tracks yet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = LightTextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentTracks.take(10).forEach { track ->
                                TrackItem(
                                    track = track,
                                    isPlaying = player.currentTrack.value?.id == track.id && player.isPlaying.value,
                                    onClick = { player.playTrack(track) }
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
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = LightTextSecondary
            )
        }
    }
}
