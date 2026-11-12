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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.ThemeState

/**
 * Settings Screen with AMOLED Dark & Light mode adaptivity
 * and dynamic reactive accent coloring.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAudioPlayback: () -> Unit,
    onNavigateToAppUpdate: () -> Unit,
    onNavigateToSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF7F5F8)
    val cardBg = if (isAmoled) Color(0xFF101016) else Color(0xFFEFECEF).copy(alpha = 0.55f)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top Header Row ---
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
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    ),
                    color = textPrimary
                )
            }
        }

        // --- 2. Collaboration & Social ---
        item {
            SettingsSectionHeader("Collaboration & Social", accentColor)
            SettingsCard(
                icon = Icons.Default.Group,
                iconBg = accentColor.copy(alpha = 0.2f),
                iconTint = accentColor,
                title = "MasterWave Jam",
                subtitle = "Listen together in real-time • Host or join a room",
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = {
                    Toast.makeText(context, "MasterWave Jam: Peer listening ready", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // --- 3. Services & Add-ons ---
        item {
            SettingsSectionHeader("Services & Add-ons", accentColor)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsCard(
                    icon = Icons.Default.Extension,
                    iconBg = accentColor.copy(alpha = 0.15f),
                    iconTint = accentColor,
                    title = "Provider Modules",
                    subtitle = "Extra sources queried alongside YouTube Music",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        Toast.makeText(context, "Provider Modules: Active (NewPipe + InnerTube)", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsCard(
                    icon = Icons.Default.Album,
                    iconBg = accentColor.copy(alpha = 0.2f),
                    iconTint = accentColor,
                    title = "Last.fm",
                    subtitle = "Connect account, scrobbles & statistics sync",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        Toast.makeText(context, "Last.fm Scrobbler integration", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsCard(
                    icon = Icons.Default.Sync,
                    iconBg = if (isAmoled) Color(0xFF181822) else Color(0xFFEFECEF),
                    iconTint = textPrimary,
                    title = "YouTube & Sync",
                    subtitle = "1-Tap Cloud playlist synchronization",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        Toast.makeText(context, "YouTube Music 1-Tap Cloud Sync active", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsCard(
                    icon = Icons.Default.PlayCircle,
                    iconBg = Color(0xFF1DB954).copy(alpha = 0.2f),
                    iconTint = Color(0xFF1DB954),
                    title = "Spotify",
                    subtitle = "1-Tap Spotify Cloud discovery & playlists",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        Toast.makeText(context, "Spotify 1-Tap Cloud Sync ready", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- 4. Playback & UI ---
        item {
            SettingsSectionHeader("Playback & UI", accentColor)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsCard(
                    icon = Icons.Default.Palette,
                    iconBg = accentColor.copy(alpha = 0.18f),
                    iconTint = accentColor,
                    title = "Appearance & Visuals",
                    subtitle = "AMOLED Dark, Custom Palette, Color Picker",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToAppearance
                )
                SettingsCard(
                    icon = Icons.Default.Equalizer,
                    iconBg = accentColor.copy(alpha = 0.2f),
                    iconTint = accentColor,
                    title = "Audio & Playback",
                    subtitle = "Streaming quality, Audio engine, Equalizer...",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToAudioPlayback
                )
                SettingsCard(
                    icon = Icons.Default.QueueMusic,
                    iconBg = if (isAmoled) Color(0xFF181822) else Color(0xFFEFECEF),
                    iconTint = textPrimary,
                    title = "Library & Content",
                    subtitle = "Home layout, Playlist imports, Downloads,...",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        Toast.makeText(context, "Library & Content management", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- 5. System & About ---
        item {
            SettingsSectionHeader("System & About", accentColor)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsCard(
                    icon = Icons.Default.CloudDownload,
                    iconBg = if (isAmoled) Color(0xFF181822) else Color(0xFFEFECEF),
                    iconTint = textPrimary,
                    title = "Data & Storage",
                    subtitle = "Backup & Restore, Cache clearing, CSV history",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        Toast.makeText(context, "Cache and offline media storage", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsCard(
                    icon = Icons.Default.Info,
                    iconBg = accentColor.copy(alpha = 0.2f),
                    iconTint = accentColor,
                    title = "About & System",
                    subtitle = "Version 1.0.0 (Snapdragon 6s Gen 3)",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToAppUpdate
                )
                SettingsCard(
                    icon = Icons.Default.HelpOutline,
                    iconBg = if (isAmoled) Color(0xFF181822) else Color(0xFFEFECEF),
                    iconTint = textPrimary,
                    title = "Support & Community",
                    subtitle = "Open source code, FAQ, License",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToSupport
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, accentColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        ),
        color = accentColor,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
