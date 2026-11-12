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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.masterwave.player.ui.theme.AccentCrimson
import com.masterwave.player.ui.theme.CanvasObsidian
import com.masterwave.player.ui.theme.SurfaceCard
import com.masterwave.player.ui.theme.SurfaceElevated
import com.masterwave.player.ui.theme.TextPrimary
import com.masterwave.player.ui.theme.TextSecondary
import com.masterwave.player.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasObsidian),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        // Section: Audio Engine
        item {
            SettingsCategoryHeader("AUDIO PLAYBACK")
            SettingsItemCard(
                icon = Icons.Default.Audiotrack,
                title = "Decoder & Codec Engine",
                subtitle = "ExoPlayer 1.2.1 + FFmpeg Software Decoder\nPrefers Opus ~160 kbps @ 48,000 Hz Stereo"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsItemCard(
                icon = Icons.Default.Audiotrack,
                title = "Audio Output",
                subtitle = "Deterministic AudioTrack with system Audio Focus"
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Privacy & Source
        item {
            SettingsCategoryHeader("PRIVACY & SOURCES")
            SettingsItemCard(
                icon = Icons.Default.Security,
                title = "Anonymous Access",
                subtitle = "Direct streaming without mandatory user accounts or tracking"
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsItemCard(
                icon = Icons.Default.Security,
                title = "Zero Ads & Zero Paid APIs",
                subtitle = "Self-contained playback engine without paid cloud backends"
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Storage
        item {
            SettingsCategoryHeader("STORAGE & CACHE")
            SettingsItemCard(
                icon = Icons.Default.Delete,
                title = "Clear Image Cache",
                subtitle = "Cleans artwork disk cache on local storage",
                onClick = {
                    Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: About
        item {
            SettingsCategoryHeader("ABOUT")
            SettingsItemCard(
                icon = Icons.Default.Info,
                title = "MasterWave v1.0.0",
                subtitle = "Licensed under GNU General Public License v3.0\nDesigned for Moto G45 5G & modern Android"
            )
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = AccentCrimson,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentCrimson,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
