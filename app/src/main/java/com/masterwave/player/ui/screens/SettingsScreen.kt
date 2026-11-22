package com.masterwave.player.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.rememberAppThemeColors
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentIce
import com.masterwave.player.ui.theme.WebAccentOchre
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentPink
import com.masterwave.player.ui.theme.WebAccentRose
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.WebAccentViolet
import kotlinx.coroutines.launch

data class SettingsItemData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val onClick: () -> Unit
)

/**
 * Supercool MasterWave Settings Screen:
 * Completely rebuilt in the MasterWave Website Warm Light Aesthetic (#fffaf0 canvas,
 * soft cream cards, bold ink outlines, drop-shadows & pastel badges).
 *
 * All unwanted/deprecated clutter permanently removed:
 * - Removed: AI Hub, Listening Summary, Echo Wrapped, Music Flow.
 * - Added: New Listen Together, Support MasterWave (persuasive donation), App Updates.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAudioPlayback: () -> Unit,
    onNavigateToAppUpdate: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToYouTubeSync: () -> Unit = {},
    onNavigateToListenTogether: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    player: MasterWavePlayer? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = rememberAppThemeColors()
    var searchQuery by remember { mutableStateOf("") }

    // --- Clean, Curated Settings Groups ---
    // Group 1: Sound & Experience
    val groupSound = listOf(
        SettingsItemData(
            id = "player_audio",
            icon = Icons.Default.PlayArrow,
            title = "Player & Audio",
            subtitle = "48 kHz studio stereo, equalizer, gapless audio",
            accentColor = WebAccentViolet,
            onClick = onNavigateToAudioPlayback
        ),
        SettingsItemData(
            id = "appearance",
            icon = Icons.Default.Palette,
            title = "Appearance & Visuals",
            subtitle = "Website light theme, accent colors, liquid glass",
            accentColor = WebAccentPink,
            onClick = onNavigateToAppearance
        ),
        SettingsItemData(
            id = "listen_together",
            icon = Icons.Default.Group,
            title = "Listen Together",
            subtitle = "Host or join real-time music sync with friends",
            accentColor = WebAccentSky,
            onClick = onNavigateToListenTogether
        )
    )

    // Group 2: Library, Storage & Sync
    val groupData = listOf(
        SettingsItemData(
            id = "storage",
            icon = Icons.Default.Storage,
            title = "Storage & Cache",
            subtitle = "Clear cached tracks and manage offline storage",
            accentColor = WebAccentPeach,
            onClick = {
                scope.launch {
                    try {
                        val cacheDir = context.cacheDir
                        val size = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                        cacheDir.deleteRecursively()
                        cacheDir.mkdirs()
                        val freedMb = (size / (1024 * 1024)).coerceAtLeast(1)
                        Toast.makeText(context, "Cleared $freedMb MB cached audio data", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Storage cache cleaned", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ),
        SettingsItemData(
            id = "backup_restore",
            icon = Icons.Default.CloudDownload,
            title = "Backup & Restore",
            subtitle = "Export or import your playlists and favorites",
            accentColor = WebAccentOchre,
            onClick = onNavigateToBackupRestore
        ),
        SettingsItemData(
            id = "account_sync",
            icon = Icons.Default.AccountCircle,
            title = "Cloud & Integrations",
            subtitle = "Import playlists from YouTube Music",
            accentColor = WebAccentEmerald,
            onClick = onNavigateToYouTubeSync
        )
    )

    // Group 3: Community, Support & Updates
    val groupCommunity = listOf(
        SettingsItemData(
            id = "support_donate",
            icon = Icons.Default.VolunteerActivism,
            title = "Support MasterWave",
            subtitle = "Keep MasterWave ad-free forever • Voluntary UPI support",
            accentColor = WebAccentRose,
            onClick = onNavigateToSupport
        ),
        SettingsItemData(
            id = "system_update",
            icon = Icons.Default.SystemUpdate,
            title = "App Updates & Changelog",
            subtitle = "Version v2.0.0 • Download latest APK and mirrors",
            accentColor = WebAccentIce,
            onClick = onNavigateToAppUpdate
        ),
        SettingsItemData(
            id = "privacy",
            icon = Icons.Default.Security,
            title = "Privacy & Zero Telemetry",
            subtitle = "100% private, zero profiling, on-device library only",
            accentColor = WebAccentGreen,
            onClick = {
                Toast.makeText(context, "Privacy: MasterWave has zero trackers and zero telemetry.", Toast.LENGTH_LONG).show()
            }
        )
    )

    val allGroups = listOf(
        Pair("SOUND & EXPERIENCE", groupSound),
        Pair("LIBRARY & STORAGE", groupData),
        Pair("COMMUNITY & UPDATES", groupCommunity)
    )

    val filteredGroups = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allGroups
        } else {
            allGroups.mapNotNull { (groupTitle, items) ->
                val filtered = items.filter { item ->
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true)
                }
                if (filtered.isNotEmpty()) Pair(groupTitle, filtered) else null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Top Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, colors.border, CircleShape)
                        .background(colors.surfaceSoft, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Settings",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colors.ink
                    )
                    Text(
                        text = "Customize sound, appearance, and sync",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                }
            }
        }

        // --- Stadium Search Pill Bar ---
        item {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = DisplayFontFamily,
                            color = colors.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.ink),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search settings, audio, and storage...",
                                    fontFamily = DisplayFontFamily,
                                    color = colors.muted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Grouped Bento Clusters ---
        filteredGroups.forEach { (groupTitle, items) ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = groupTitle,
                        fontFamily = DisplayFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = colors.muted,
                        modifier = Modifier.padding(start = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            items.forEachIndexed { index, itemData ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { itemData.onClick() }
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tactile Pastel Squircle Badge
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(itemData.accentColor)
                                            .border(1.5.dp, colors.border, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = itemData.icon,
                                            contentDescription = null,
                                            tint = Color(0xFF101828),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemData.title,
                                            fontFamily = DisplayFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.ink
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = itemData.subtitle,
                                            fontFamily = DisplayFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.muted,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = colors.muted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                if (index < items.size - 1) {
                                    HorizontalDivider(
                                        color = colors.hairline,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MasterWave App Brand Stamp Footer ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MasterWave v2.0.0 (Build 2026)",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "48 kHz Studio Architecture • 100% Free Forever",
                    fontFamily = DisplayFontFamily,
                    fontSize = 12.sp,
                    color = colors.muted
                )
            }
        }
    }
}
