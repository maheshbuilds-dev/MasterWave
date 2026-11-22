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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.AppFont
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeAccentPreset
import com.masterwave.player.ui.theme.ThemeMode
import com.masterwave.player.ui.theme.ThemeState
import com.masterwave.player.ui.theme.toFontFamily

/**
 * MasterWave Appearance Screen:
 * - Theme Style: Exclusively Dark (AMOLED pitch black) & Light mode.
 * - Accent Colours: Curated palette presets & dynamic art.
 * - High Refresh Rate: Toggle 120Hz display refresh mode.
 * - Liquid Glass: Toggle shader-powered frosted acrylic glass dock.
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by ThemeState.themeMode.collectAsState()
    val activeAccentPreset by ThemeState.accentPreset.collectAsState()
    val effectiveAccent by ThemeState.effectiveAccentColor.collectAsState()
    val dynamicColorEnabled by ThemeState.dynamicColorEnabled.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val liquidGlassEnabled by ThemeState.liquidGlassEnabled.collectAsState()
    val highRefreshRateEnabled by ThemeState.highRefreshRateEnabled.collectAsState()
    val selectedFont by ThemeState.selectedFont.collectAsState()
    val animatedAlbumArtEnabled by ThemeState.animatedAlbumArtEnabled.collectAsState()
    val context = LocalContext.current

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBg = if (isAmoled) Color(0xFF101118) else Color(0xFFF1F3F5)
    val cardBorder = if (isAmoled) Color(0x1FFFFFFF) else Color(0xFF101828)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF101828)
    val textSecondary = if (isAmoled) Color(0xFFA0A5B8) else Color(0xFF6A6A6A)
    val dividerColor = if (isAmoled) Color(0x14FFFFFF) else Color(0xFFE5E5E5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Top App Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isAmoled) Color(0xFF14151E) else Color(0xFFE5E7EB))
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Theme, colours, high refresh rate, and liquid glass",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }
        }

        // --- 1. Theme & Colours ---
        item {
            AppearanceCardSection(title = "Theme and Colours", textSecondary = textSecondary) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Theme Style Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(effectiveAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = effectiveAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Theme Style",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Text(
                                text = if (themeMode == ThemeMode.LIGHT) "Clean Studio Light" else "AMOLED Pure Dark",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }
                    }

                    // Strict Dark (AMOLED) & Light Capsule Only
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isAmoled) Color(0xFF181A24) else Color(0xFFEFF1F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val themes = listOf(
                                ThemeMode.DARK to ("Dark (AMOLED)" to Icons.Default.DarkMode),
                                ThemeMode.LIGHT to ("Light" to Icons.Default.LightMode)
                            )
                            themes.forEach { (mode, pair) ->
                                val (label, icon) = pair
                                val isSelected = when (mode) {
                                    ThemeMode.LIGHT -> themeMode == ThemeMode.LIGHT
                                    else -> themeMode != ThemeMode.LIGHT
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) effectiveAccent else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .bouncyClickable {
                                            ThemeState.setThemeMode(if (mode == ThemeMode.LIGHT) ThemeMode.LIGHT else ThemeMode.AMOLED)
                                            Toast.makeText(context, "$label Theme applied", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AppearanceDivider(dividerColor)

                    // Accent Colors Title
                    Text(
                        text = "ACCENT COLOURS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = textSecondary
                    )

                    // Interactive Color Swatches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf(
                            ThemeAccentPreset.APPLE_MUSIC to Color(0xFFFA2D48),
                            ThemeAccentPreset.OCEAN to Color(0xFF00D4B2),
                            ThemeAccentPreset.BERRY to Color(0xFF8B5CF6),
                            ThemeAccentPreset.FOREST to Color(0xFF10B981),
                            ThemeAccentPreset.SUNSET to Color(0xFFF59E0B),
                            ThemeAccentPreset.MONOCHROME to Color(0xFFE2E8F0)
                        )
                        presets.forEach { (preset, color) ->
                            val isSelected = activeAccentPreset == preset && !dynamicColorEnabled
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) (if (isAmoled) Color.White else Color.Black) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        ThemeState.setDynamicColorEnabled(false)
                                        ThemeState.setAccentPreset(preset)
                                        Toast.makeText(context, "Accent: ${preset.title}", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color == Color(0xFFE2E8F0)) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. High Refresh Rate ---
        item {
            AppearanceCardSection(title = "Display Refresh Rate", textSecondary = textSecondary) {
                AppearanceToggleRow(
                    icon = Icons.Default.Speed,
                    tint = Color(0xFF38BDF8),
                    title = "Enable High Refresh Rate",
                    subtitle = "Force 120Hz display refresh mode for fluid animations and silky scrolling",
                    checked = highRefreshRateEnabled,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onCheckedChange = {
                        ThemeState.setHighRefreshRateEnabled(it)
                        Toast.makeText(context, if (it) "120Hz High Refresh Rate enabled" else "Standard Refresh Rate enabled", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- 3. Now Playing & Visual Materials ---
        item {
            AppearanceCardSection(title = "Now Playing & Materials", textSecondary = textSecondary) {
                AppearanceToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    tint = effectiveAccent,
                    title = "Album Cover Art Animation",
                    subtitle = "Dynamic kinetic breathing and fluid ambient glow for album art in Now Playing",
                    checked = animatedAlbumArtEnabled,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onCheckedChange = {
                        ThemeState.setAnimatedAlbumArtEnabled(it)
                        Toast.makeText(context, if (it) "Album Cover Art animation enabled" else "Album Cover Art animation disabled", Toast.LENGTH_SHORT).show()
                    }
                )
                AppearanceDivider(dividerColor)
                AppearanceToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    tint = effectiveAccent,
                    title = "Liquid Glass",
                    subtitle = "Shader-powered frosted acrylic glass dock and controls",
                    checked = liquidGlassEnabled,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onCheckedChange = {
                        ThemeState.setLiquidGlassEnabled(it)
                        Toast.makeText(context, if (it) "Liquid Glass enabled" else "Liquid Glass disabled", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- 4. Fonts & Typography ---
        item {
            AppearanceCardSection(title = "Fonts & Typography", textSecondary = textSecondary) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    AppFont.entries.forEachIndexed { index, font ->
                        val isSelected = selectedFont == font
                        val fontFamily = font.toFontFamily()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable(scaleDown = 0.98f) {
                                    ThemeState.setSelectedFont(font)
                                    Toast.makeText(context, "Font: ${font.displayName}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = font.displayName,
                                        fontFamily = fontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) effectiveAccent else textPrimary
                                    )
                                    if (font == AppFont.FUCEK) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(effectiveAccent.copy(alpha = 0.16f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "DEFAULT",
                                                fontFamily = fontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                color = effectiveAccent
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = font.previewSubtitle,
                                    fontFamily = fontFamily,
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = font.sampleText,
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = textPrimary.copy(alpha = 0.90f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "1234567890!@#%&*()_+-=",
                                    fontFamily = fontFamily,
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(effectiveAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (index < AppFont.entries.size - 1) {
                            AppearanceDivider(dividerColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceCardSection(
    title: String,
    textSecondary: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 0.9.sp
            ),
            color = textSecondary,
            modifier = Modifier.padding(start = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun AppearanceDivider(color: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        thickness = 0.6.dp,
        color = color
    )
}

@Composable
private fun AppearanceToggleRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                ),
                color = textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = textSecondary,
                maxLines = 2
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tint
            )
        )
    }
}
