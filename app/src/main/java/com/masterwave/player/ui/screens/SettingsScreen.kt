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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBackupRestore
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.ThemeState

/**
 * Clean, Streamlined Settings Screen for MasterWave Music:
 * Preserves essential services (YouTube Music, Spotify), Audio Engine,
 * Visuals, and full launcher-standard Backup & Restore.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAudioPlayback: () -> Unit,
    onNavigateToAppUpdate: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToYouTubeSync: () -> Unit = {},
    onNavigateToSpotifySync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF6F5F3)
    val cardBg = if (isAmoled) Color(0xFF101016) else Color(0xFFFFFFFF)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)

    val headerBg = if (isAmoled) Color(0xFF14151E) else Color(0xFFFFFFFF)
    val headerBorder = if (isAmoled) Color(0x20FFFFFF) else Color(0x0D000000)
    val headerShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 96.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Connected Services ---
            item {
                SettingsSectionHeader("Connected Services", accentColor)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCard(
                        icon = Icons.Default.Sync,
                        iconBg = Color(0xFFFF0000).copy(alpha = 0.18f),
                        iconTint = Color(0xFFFF0000),
                        title = "YouTube & Sync",
                    subtitle = "Account connection, channels, history & cloud sync",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToYouTubeSync
                )
                SettingsCard(
                    icon = Icons.Default.PlayCircle,
                    iconBg = Color(0xFF1DB954).copy(alpha = 0.2f),
                    iconTint = Color(0xFF1DB954),
                    title = "Spotify",
                    subtitle = "Account connection, Canvas, playlist imports",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToSpotifySync
                )
            }
        }

            // --- 2. Playback & UI ---
            item {
                SettingsSectionHeader("Playback & UI", textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCard(
                        icon = Icons.Default.Palette,
                        iconBg = com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.16f),
                        iconTint = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        title = "Appearance & Visuals",
                        subtitle = "AMOLED Dark, Liquid Glass Dock, Accent Colors",
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = onNavigateToAppearance
                    )
                    SettingsCard(
                        icon = Icons.Default.Equalizer,
                        iconBg = com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.16f),
                        iconTint = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        title = "Audio & Playback",
                        subtitle = "Streaming quality, Audio engine, Equalizer...",
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = onNavigateToAudioPlayback
                    )
                }
            }

            // --- 3. System & Backup ---
            item {
                SettingsSectionHeader("System & Data", textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCard(
                        icon = Icons.Default.SettingsBackupRestore,
                        iconBg = com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.16f),
                        iconTint = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        title = "Backup & Restore",
                        subtitle = "Export or restore settings, themes, and playlists",
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = onNavigateToBackupRestore
                    )
                    SettingsCard(
                        icon = Icons.Default.Info,
                        iconBg = com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.16f),
                        iconTint = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        title = "About & System",
                        subtitle = "MasterWave 2.0 • Snapdragon 6s Gen 3",
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
                    subtitle = "Telegram updates, Discord, FAQ",
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = onNavigateToSupport
                )
            }
        }
    }

    // Pinned Floating Top Header matching Reference Screenshot
    Surface(
        shape = headerShape,
        color = headerBg,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .shadow(
                elevation = 8.dp,
                shape = headerShape,
                spotColor = Color.Black.copy(alpha = if (isAmoled) 0.5f else 0.08f),
                ambientColor = Color.Black.copy(alpha = if (isAmoled) 0.3f else 0.03f)
            )
            .border(width = 1.dp, color = headerBorder, shape = headerShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = textPrimary
                    )
                }

                IconButton(onClick = { /* Search in settings */ }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
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
            fontSize = 16.sp
        ),
        color = Color(0xFF880E4F), // Signature Burgundy / Crimson from reference screenshot
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
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
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
