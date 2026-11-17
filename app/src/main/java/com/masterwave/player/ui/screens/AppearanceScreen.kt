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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.components.ColorPickerModal
import com.masterwave.player.ui.theme.ThemeMode
import com.masterwave.player.ui.theme.ThemeState

/**
 * Fullscreen Appearance & Visuals screen matching LastWave reference:
 * - Theme Mode: AMOLED Dark (#000000 pitch black), Light, System Default.
 * - 8 Accent Color choices (7 ultra-vibrant presets + 8th is Custom Color Picker).
 * - Liquid Glass Ergonomics toggle.
 * - Dynamic Colors toggle (Artwork color extraction).
 * - Glowing Lyrics toggle.
 * - Animated Album Canvas toggle.
 * - Full Screen Artwork toggle.
 * - Gradient Blur on Canvas toggle.
 * - Button Now Playing toggle.
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by ThemeState.themeMode.collectAsState()
    val activeAccent by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val dynamicColorsEnabled by ThemeState.dynamicColorEnabled.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    var liquidGlassEnabled by remember { mutableStateOf(true) }
    var glowingLyricsEnabled by remember { mutableStateOf(true) }
    var animatedCanvasEnabled by remember { mutableStateOf(true) }
    var fullScreenArtworkEnabled by remember { mutableStateOf(true) }
    var gradientBlurEnabled by remember { mutableStateOf(true) }
    var buttonNowPlayingEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF6F5F3)
    val cardBg = if (isAmoled) Color(0xFF101016) else Color(0xFFEFECEF).copy(alpha = 0.55f)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)

    // The 7 presets + 8th is Custom Picker
    val top7Presets = ThemeState.Presets.take(7)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
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
                        tint = textPrimary,
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
                    color = textPrimary
                )
            }
        }

        // Section: Theme Mode (AMOLED Dark / Light)
        item {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = activeAccent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        ThemeMode.LIGHT to "Light Mode (Apple Music / LastWave #F6F5F3)",
                        ThemeMode.DARK to "Dark Mode (Layered Charcoal #0D0F0E)",
                        ThemeMode.AMOLED to "AMOLED Dark Mode (True Pitch Black #000000)",
                        ThemeMode.SYSTEM to "System Default"
                    ).forEach { (mode, label) ->
                        val isSelected = mode == themeMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) activeAccent.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable {
                                    ThemeState.setThemeMode(mode)
                                    Toast.makeText(context, "$label active", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) activeAccent else textPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: 8 Accent Colors (8th is Custom Color Picker!)
        item {
            Text(
                text = "Accent Colors & Palette",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = activeAccent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "8 immersive accents — 8th opens the free-form custom color picker:",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    // 8-item grid (4 x 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        top7Presets.take(4).forEach { (color, name) ->
                            val isSelected = color == activeAccent
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        ThemeState.setAccentColor(color)
                                        Toast.makeText(context, "$name applied", Toast.LENGTH_SHORT).show()
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        top7Presets.drop(4).forEach { (color, name) ->
                            val isSelected = color == activeAccent
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        ThemeState.setAccentColor(color)
                                        Toast.makeText(context, "$name applied", Toast.LENGTH_SHORT).show()
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        // 8th Item: Custom Color Picker Swatch!
                        val isCustomSelected = top7Presets.none { it.first == activeAccent }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                                .clickable { showColorPicker = true }
                                .then(
                                    if (isCustomSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCustomSelected) activeAccent else Color(0xCC000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Custom Picker",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Visual Effects & Liquid Glass
        item {
            Text(
                text = "Liquid Glass & Visual Effects",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = activeAccent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Liquid Glass
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Liquid Glass Dock", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Translucent frosted refraction with specular rims", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = liquidGlassEnabled,
                            onCheckedChange = { liquidGlassEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeAccent)
                        )
                    }

                    // Dynamic Colors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Dynamic Colors", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Adapt UI accents dynamically to album cover tones", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = dynamicColorsEnabled,
                            onCheckedChange = { ThemeState.setDynamicColorEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeAccent)
                        )
                    }

                    // Glowing Lyrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Glowing Lyrics", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Radiant neon illumination on active sung lyric line", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = glowingLyricsEnabled,
                            onCheckedChange = { glowingLyricsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeAccent)
                        )
                    }
                }
            }
        }

        // Section: Animated Album Canvas
        item {
            Text(
                text = "Animated Album Canvas",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = activeAccent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Animated Album Canvas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Animated Album Canvas", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Stream motion artwork loops from cloud sources", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = animatedCanvasEnabled,
                            onCheckedChange = { animatedCanvasEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeAccent)
                        )
                    }

                    // Full Screen Artwork
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Full Screen Artwork", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Full screen edge-to-edge artwork for Now Playing", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = fullScreenArtworkEnabled,
                            onCheckedChange = { fullScreenArtworkEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeAccent)
                        )
                    }

                    // Gradient Blur on Canvas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Gradient Blur on Canvas", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Blend canvas bottom into ambient pitch black background", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = gradientBlurEnabled,
                            onCheckedChange = { gradientBlurEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeAccent)
                        )
                    }
                }
            }
        }
    }

    // Modal: Free-form HSV Custom Color Picker
    if (showColorPicker) {
        ColorPickerModal(
            initialColor = activeAccent,
            onDismiss = { showColorPicker = false },
            onColorSelected = { selected ->
                ThemeState.setAccentColor(selected)
                showColorPicker = false
                Toast.makeText(context, "Custom accent applied", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
