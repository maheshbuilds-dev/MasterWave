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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.ThemeState

/**
 * Statistics Screen with:
 * - Free Google Sign-In with user avatar & cloud sync.
 * - Dynamic reactive accent colors.
 * - AMOLED Dark & Light mode adaptivity.
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
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current

    val cloudManager = remember { CloudAccountManager.getInstance(context, repository) }
    val userProfile by cloudManager.userProfile.collectAsState()

    var selectedFilter by remember { mutableStateOf("Most Played") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val uniqueTracks = recentTracks.distinctBy { it.id }.size
    val uniqueArtists = recentTracks.map { it.artist }.distinct().size
    val uniqueAlbums = recentTracks.mapNotNull { it.album.ifBlank { null } }.distinct().size

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFAF0)
    val cardContainerBg = if (isAmoled) Color(0xFF101016) else Color(0xFFF5F0E0)
    val cardItemBg = if (isAmoled) Color(0xFF161620) else Color(0xFFFAF5E8)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)
    val pillBg = if (isAmoled) Color(0xFF181824) else Color(0xFFF5F0E0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top App Bar (Reference Page 5) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.6).sp
                    ),
                    color = textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Downloads manager", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloads",
                            tint = textPrimary,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textPrimary,
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
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.isSignedIn && !userProfile.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = userProfile.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. User Guest / Last.fm Banner Card (Reference Page 5) ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardItemBg,
                border = BorderStroke(1.dp, Color(0xFFE2E8E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(onClick = onNavigateToSettings)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.isSignedIn && !userProfile.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile.photoUrl,
                                contentDescription = userProfile.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userProfile.isSignedIn) userProfile.displayName else "Guest",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Connect Last.fm in Settings to sync global scrobbles",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = textSecondary
                        )
                    }
                }
            }
        }

        // --- 3. 2x2 Metric Grid (Reference Page 5) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Plays (Sage Green Card) & Tracks (White Card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Plays Card (Sage Green Featured Card)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = accentColor,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
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
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1B5434)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (totalPlays > 0) "$totalPlays" else "—",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Plays",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // Tracks Card (White Card)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardItemBg,
                        border = BorderStroke(1.dp, Color(0xFFE2E8E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (uniqueTracks > 0) "$uniqueTracks" else "—",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Tracks",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // Row 2: Artists & Albums (White Cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Artists Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardItemBg,
                        border = BorderStroke(1.dp, Color(0xFFE2E8E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (uniqueArtists > 0) "$uniqueArtists" else "—",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Artists",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    // Albums Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardItemBg,
                        border = BorderStroke(1.dp, Color(0xFFE2E8E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (uniqueAlbums > 0) "$uniqueAlbums" else "—",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. "List" Section with "Most Played" Dropdown (Reference Page 5) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        color = textPrimary
                    )

                    // Dropdown filter pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = cardItemBg,
                            border = BorderStroke(1.dp, Color(0xFFE2E8E1)),
                            modifier = Modifier.bouncyClickable { showFilterMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = selectedFilter,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = textSecondary,
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

                // White Card for Track List or Clean Empty State
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardItemBg,
                    border = BorderStroke(1.dp, Color(0xFFE2E8E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (recentTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tracks yet",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = textSecondary
                                )
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            recentTracks.take(10).forEach { track ->
                                TrackItem(
                                    track = track,
                                    isPlaying = player.currentTrack.collectAsState().value?.id == track.id && player.isPlaying.collectAsState().value,
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
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                ),
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                ),
                color = textSecondary
            )
        }
    }
}
