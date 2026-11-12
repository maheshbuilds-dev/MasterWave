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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.AccentBurgundy
import com.masterwave.player.ui.theme.AccentCrimson
import com.masterwave.player.ui.theme.AccentEmerald
import com.masterwave.player.ui.theme.AccentIndigo
import com.masterwave.player.ui.theme.AccentMaroon
import com.masterwave.player.ui.theme.AccentRose
import com.masterwave.player.ui.theme.LightBackground
import com.masterwave.player.ui.theme.LightCardPink
import com.masterwave.player.ui.theme.LightPrimaryMaroon
import com.masterwave.player.ui.theme.LightSurface
import com.masterwave.player.ui.theme.LightSurfaceVariant
import com.masterwave.player.ui.theme.LightTextPrimary
import com.masterwave.player.ui.theme.LightTextSecondary

/**
 * Fullscreen Appearance & Visuals screen.
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeMode by remember { mutableStateOf("Light (LastWave Expressive)") }
    var selectedAccent by remember { mutableStateOf(AccentMaroon) }
    var dynamicColorsEnabled by remember { mutableStateOf(true) }
    var animatedWaveformEnabled by remember { mutableStateOf(true) }
    var lyricsBlurEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val accents = listOf(
        AccentMaroon to "Maroon",
        AccentCrimson to "Crimson",
        AccentRose to "Rose",
        AccentEmerald to "Emerald",
        AccentIndigo to "Indigo"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
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
                        tint = LightTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Appearance & Visuals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = LightTextPrimary
                )
            }
        }

        // Section: Theme Mode
        item {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LightPrimaryMaroon
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LightSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Light (LastWave Expressive)", "Dark (Pure Obsidian)", "System Default").forEach { mode ->
                        val isSelected = mode == themeMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) LightCardPink else Color.Transparent)
                                .clickable { themeMode = mode }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) LightPrimaryMaroon else LightTextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LightPrimaryMaroon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Accent Colors
        item {
            Text(
                text = "Accent Colors",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LightPrimaryMaroon
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LightSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accents.forEach { (color, name) ->
                        val isSelected = color == selectedAccent
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    selectedAccent = color
                                    Toast.makeText(context, "$name accent applied", Toast.LENGTH_SHORT).show()
                                }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Animations & Visuals
        item {
            Text(
                text = "Animations & Effects",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LightPrimaryMaroon
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LightSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Animated Waveform
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fluid Animated Waveform",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = LightTextPrimary
                            )
                            Text(
                                text = "Oscillating harmonic wave curves during playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightTextSecondary
                            )
                        }
                        Switch(
                            checked = animatedWaveformEnabled,
                            onCheckedChange = { animatedWaveformEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LightPrimaryMaroon)
                        )
                    }

                    // Dynamic Artwork Blur
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Artwork Background Blur",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = LightTextPrimary
                            )
                            Text(
                                text = "Immersive full-screen album ambient lighting",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightTextSecondary
                            )
                        }
                        Switch(
                            checked = lyricsBlurEnabled,
                            onCheckedChange = { lyricsBlurEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LightPrimaryMaroon)
                        )
                    }
                }
            }
        }
    }
}
