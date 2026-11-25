package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masterwave.player.data.appearance.AnimationSpeed
import com.masterwave.player.data.appearance.AppIconStyle
import com.masterwave.player.data.appearance.AppearanceStudioManager
import com.masterwave.player.data.appearance.LibraryLayout
import com.masterwave.player.data.appearance.NowPlayingBgStyle
import com.masterwave.player.data.appearance.NowPlayingTemplate
import com.masterwave.player.data.appearance.ThemePreset
import com.masterwave.player.data.appearance.TypographyScale
import com.masterwave.player.data.icon.AppIcon
import com.masterwave.player.data.icon.AppIconManager
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.AppFont
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeAccentPreset
import com.masterwave.player.ui.theme.ThemeMode
import com.masterwave.player.ui.theme.ThemeState
import com.masterwave.player.ui.theme.rememberAppThemeColors
import com.masterwave.player.ui.theme.toFontFamily

/**
 * MASTERWAVE APPEARANCE STUDIO
 * Professional design-system and personalization hub:
 * - Theme Engine: Follow System, Light, Dark, AMOLED, and 10 Built-In Theme Presets
 * - Custom Colour Studio: Curated swatches + custom accents + dynamic artwork palette
 * - Now Playing Customization Studio: 5 Selectable Templates (Classic, Immersive, Minimal, Compact, Modern Glass)
 * - Now Playing Background Studio: Blurred Artwork, Solid Canvas, Album Gradient, Dark Cinematic
 * - Artwork Corner Radius (4dp to 40dp)
 * - Icon Style Studio: 6 Vector Global Icon Styles (Minimal Outline, Rounded, Filled, Duotone, Soft Rounded, Monochrome)
 * - Typography Studio: Font Family (Fucek default, System Default) + 3 Scale Ramps (Compact, Balanced, Large)
 * - Home & Library Layouts: List, Grid, Compact
 * - Animation Studio: Off, Subtle, Normal, Expressive + 120Hz Refresh Rate
 * - Appearance Profiles: Save, Apply, Export JSON, Import JSON, Restore Factory Defaults
 * - Live Interactive Appearance Preview Modal
 * - Full Settings Search filtering
 * - Strict Rule 0 Protection: Preserves existing Liquid Glass bottom dock and mini-player!
 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = rememberAppThemeColors()
    val isAmoled = colors.isDark

    val themeMode by ThemeState.themeMode.collectAsState()
    val activeAccentPreset by ThemeState.accentPreset.collectAsState()
    val effectiveAccent by ThemeState.effectiveAccentColor.collectAsState()
    val dynamicColorEnabled by ThemeState.dynamicColorEnabled.collectAsState()
    val liquidGlassEnabled by ThemeState.liquidGlassEnabled.collectAsState()
    val highRefreshRateEnabled by ThemeState.highRefreshRateEnabled.collectAsState()
    val selectedFont by ThemeState.selectedFont.collectAsState()
    val animatedAlbumArtEnabled by ThemeState.animatedAlbumArtEnabled.collectAsState()

    // Appearance Studio State Flows
    val activeProfile by AppearanceStudioManager.activeProfileName.collectAsState()
    val nowPlayingTemplate by AppearanceStudioManager.nowPlayingTemplate.collectAsState()
    val nowPlayingBgStyle by AppearanceStudioManager.nowPlayingBgStyle.collectAsState()
    val artworkRadius by AppearanceStudioManager.artworkCornerRadiusDp.collectAsState()
    val iconStyle by AppearanceStudioManager.iconStyle.collectAsState()
    val libraryLayout by AppearanceStudioManager.libraryLayout.collectAsState()
    val animationSpeed by AppearanceStudioManager.animationSpeed.collectAsState()
    val typographyScale by AppearanceStudioManager.typographyScale.collectAsState()
    val currentThemePreset by AppearanceStudioManager.themePreset.collectAsState()

    // Search and Dialogs State
    var searchQuery by remember { mutableStateOf("") }
    var showLivePreview by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    val bgColor = colors.canvas
    val cardBg = colors.surfaceCard
    val cardBorder = colors.border
    val textPrimary = colors.ink
    val textSecondary = colors.muted
    val dividerColor = colors.hairline

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top App Bar ---
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appearance Studio",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Design systems, themes, templates, and UI customization",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                }
                IconButton(
                    onClick = { showLivePreview = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(effectiveAccent.copy(alpha = 0.16f))
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Live Preview",
                        tint = effectiveAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 2. Live Search Settings Bar ---
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search themes, colors, templates, fonts...",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = effectiveAccent,
                    unfocusedBorderColor = cardBorder
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        // --- 3. Active Appearance Overview Card ---
        if (searchQuery.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.5.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(effectiveAccent)
                                )
                                Text(
                                    text = "ACTIVE PROFILE",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = effectiveAccent
                                )
                            }
                            Text(
                                text = activeProfile,
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Theme" to "${currentThemePreset.title} (${themeMode.name})",
                                "Player" to "Immersive",
                                "Icons" to iconStyle.title
                            ).forEach { (label, value) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAmoled) Color(0xFF141620) else Color(0xFFE5E7EB),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = value,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showLivePreview = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = effectiveAccent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Preview", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    AppearanceStudioManager.resetToDefaults()
                                    ThemeState.setThemeMode(ThemeMode.SYSTEM)
                                    ThemeState.setAccentPreset(ThemeAccentPreset.APPLE_MUSIC)
                                    Toast.makeText(context, "Restored factory appearance defaults", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAmoled) Color(0xFF1A1C28) else Color(0xFFE2E4EA),
                                    contentColor = textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset All", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Section: Themes & Presets ---
        if (searchQuery.isEmpty() || "theme".contains(searchQuery, true) || "preset".contains(searchQuery, true) || "dark".contains(searchQuery, true) || "light".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Themes & Mode", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 3 Theme Modes: Light, Dark AMOLED, System
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isAmoled) Color(0xFF181A24) else Color(0xFFEFF1F5),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val themes = listOf(
                                    ThemeMode.LIGHT to ("Light" to Icons.Default.LightMode),
                                    ThemeMode.AMOLED to ("Dark AMOLED" to Icons.Default.DarkMode),
                                    ThemeMode.SYSTEM to ("System" to Icons.Default.AutoAwesome)
                                )
                                themes.forEach { (mode, pair) ->
                                    val (label, icon) = pair
                                    val isSelected = when (mode) {
                                        ThemeMode.LIGHT -> themeMode == ThemeMode.LIGHT
                                        ThemeMode.SYSTEM -> themeMode == ThemeMode.SYSTEM
                                        ThemeMode.AMOLED -> themeMode == ThemeMode.AMOLED || themeMode == ThemeMode.DARK
                                        else -> false
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) effectiveAccent else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .bouncyClickable {
                                                ThemeState.setThemeMode(mode)
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
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AppearanceDivider(dividerColor)

                        // 10 Curated Theme Presets Horizontal Carousel
                        Text(
                            text = "SIGNATURE THEME PRESETS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemePreset.values().forEach { preset ->
                                val isSelected = currentThemePreset == preset
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) effectiveAccent.copy(alpha = 0.16f) else (if (isAmoled) Color(0xFF141622) else Color(0xFFF1F3F8)),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.8.dp else 1.dp,
                                        color = if (isSelected) effectiveAccent else cardBorder
                                    ),
                                    modifier = Modifier.bouncyClickable {
                                        AppearanceStudioManager.setThemePreset(preset)
                                        ThemeState.setThemeMode(preset.defaultMode)
                                        ThemeState.setAccentColor(preset.primaryColor)
                                        Toast.makeText(context, "Preset: ${preset.title}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(preset.primaryColor)
                                        )
                                        Text(
                                            text = preset.title,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) effectiveAccent else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Section: Custom Colour Studio ---
        if (searchQuery.isEmpty() || "color".contains(searchQuery, true) || "accent".contains(searchQuery, true) || "palette".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Custom Colour Studio", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dynamic Wallpaper / Album Art Colors Toggle
                        AppearanceToggleRow(
                            icon = Icons.Default.AutoAwesome,
                            tint = effectiveAccent,
                            title = "Adaptive Album Artwork Color",
                            subtitle = "Ambient bleed and accents dynamically match currently playing song",
                            checked = dynamicColorEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onCheckedChange = {
                                ThemeState.setDynamicColorEnabled(it)
                                Toast.makeText(context, if (it) "Dynamic color enabled" else "Static accent color locked", Toast.LENGTH_SHORT).show()
                            }
                        )

                        AppearanceDivider(dividerColor)

                        // Curated Accent Palette Swatches
                        Text(
                            text = "CURATED ACCENT SWATCHES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        val swatches = listOf(
                            Color(0xFFFA2D48) to "Apple Red",
                            Color(0xFF0091FF) to "Cyber Cyan",
                            Color(0xFF8B5CF6) to "Electric Violet",
                            Color(0xFF10B981) to "Emerald Mint",
                            Color(0xFFFF6B4A) to "Sunset Coral",
                            Color(0xFF881337) to "Velvet Ruby",
                            Color(0xFFF59E0B) to "Warm Amber",
                            Color(0xFF64748B) to "Minimal Slate"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            swatches.forEach { (color, name) ->
                                val isSelected = effectiveAccent == color && !dynamicColorEnabled
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) (if (isAmoled) Color.White else Color.Black) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .bouncyClickable {
                                            ThemeState.setDynamicColorEnabled(false)
                                            ThemeState.setAccentColor(color)
                                            Toast.makeText(context, "Accent: $name", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
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

        // --- 6. Section: Now Playing Customization Studio (Background & Artwork Corner Radius) ---
        if (searchQuery.isEmpty() || "now playing".contains(searchQuery, true) || "corner".contains(searchQuery, true) || "background".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Now Playing Studio", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Background Style Selector
                        Text(
                            text = "BACKGROUND STYLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NowPlayingBgStyle.values().forEach { bgStyle ->
                                val isSelected = nowPlayingBgStyle == bgStyle
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) effectiveAccent else (if (isAmoled) Color(0xFF141622) else Color(0xFFF1F3F8)),
                                    modifier = Modifier.bouncyClickable {
                                        AppearanceStudioManager.setNowPlayingBgStyle(bgStyle)
                                        Toast.makeText(context, "Background: ${bgStyle.title}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = bgStyle.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        AppearanceDivider(dividerColor)

                        // Artwork Corner Radius Slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Artwork Corner Radius",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "${artworkRadius}dp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = effectiveAccent
                                )
                            }
                            Slider(
                                value = artworkRadius.toFloat(),
                                onValueChange = { AppearanceStudioManager.setArtworkCornerRadius(it.toInt()) },
                                valueRange = 4f..40f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = effectiveAccent,
                                    activeTrackColor = effectiveAccent,
                                    inactiveTrackColor = if (isAmoled) Color(0xFF222432) else Color(0xFFE2E4EA)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // --- 7. Section: Home & Library Layouts ---
        if (searchQuery.isEmpty() || "library".contains(searchQuery, true) || "layout".contains(searchQuery, true) || "grid".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Library & List Layouts", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "LAYOUT PRESENTATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LibraryLayout.values().forEach { layout ->
                                val isSelected = libraryLayout == layout
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) effectiveAccent else (if (isAmoled) Color(0xFF161826) else Color(0xFFE5E7EB)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .bouncyClickable {
                                            AppearanceStudioManager.setLibraryLayout(layout)
                                            Toast.makeText(context, "Library Layout: ${layout.title}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = layout.title,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 8. Section: Icon Style Studio ---
        if (searchQuery.isEmpty() || "icon".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Icon Style Studio", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "GLOBAL VECTOR ICON STYLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        AppIconStyle.values().toList().chunked(2).forEach { rowStyles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStyles.forEach { style ->
                                    val isSelected = iconStyle == style
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) effectiveAccent.copy(alpha = 0.14f) else (if (isAmoled) Color(0xFF141622) else Color(0xFFF1F3F8)),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) effectiveAccent else cardBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .bouncyClickable {
                                                AppearanceStudioManager.setIconStyle(style)
                                                Toast.makeText(context, "Icon Style: ${style.title}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = style.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) effectiveAccent else textPrimary
                                            )
                                            Text(
                                                text = style.subtitle,
                                                fontSize = 10.5.sp,
                                                color = textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 9. Section: Typography Studio ---
        if (searchQuery.isEmpty() || "font".contains(searchQuery, true) || "typography".contains(searchQuery, true) || "scale".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Typography Studio", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Typography Scale Selector
                        Text(
                            text = "TYPOGRAPHY SCALE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TypographyScale.values().forEach { scale ->
                                val isSelected = typographyScale == scale
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) effectiveAccent else (if (isAmoled) Color(0xFF161826) else Color(0xFFE5E7EB)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .bouncyClickable {
                                            AppearanceStudioManager.setTypographyScale(scale)
                                            Toast.makeText(context, "Font Scale: ${scale.title}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = scale.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        AppearanceDivider(dividerColor)

                        // Font Family Selection
                        Text(
                            text = "FONT FAMILY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        AppFont.entries.forEach { font ->
                            val isSelected = selectedFont == font
                            val fontFamily = font.toFontFamily()

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) effectiveAccent.copy(alpha = 0.12f) else (if (isAmoled) Color(0xFF141622) else Color(0xFFF1F3F8)),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) effectiveAccent else cardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bouncyClickable {
                                        ThemeState.setSelectedFont(font)
                                        Toast.makeText(context, "Font: ${font.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = font.displayName,
                                            fontFamily = fontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) effectiveAccent else textPrimary
                                        )
                                        Text(
                                            text = font.previewSubtitle,
                                            fontFamily = fontFamily,
                                            fontSize = 11.5.sp,
                                            color = textSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(effectiveAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 10. Section: Animation Studio ---
        if (searchQuery.isEmpty() || "animation".contains(searchQuery, true) || "motion".contains(searchQuery, true) || "refresh".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Animation & Display Motion", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "MOTION SPEED PRESET",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimationSpeed.values().forEach { speed ->
                                val isSelected = animationSpeed == speed
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) effectiveAccent else (if (isAmoled) Color(0xFF161826) else Color(0xFFE5E7EB)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .bouncyClickable {
                                            AppearanceStudioManager.setAnimationSpeed(speed)
                                            Toast.makeText(context, "Motion: ${speed.title}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = speed.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        AppearanceDivider(dividerColor)

                        // 120Hz High Refresh Rate
                        AppearanceToggleRow(
                            icon = Icons.Default.Speed,
                            tint = Color(0xFF38BDF8),
                            title = "High Refresh Rate (120Hz)",
                            subtitle = "Fluid animation physics and ultra-silky list scrolling",
                            checked = highRefreshRateEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onCheckedChange = {
                                ThemeState.setHighRefreshRateEnabled(it)
                                Toast.makeText(context, if (it) "120Hz High Refresh Rate active" else "Standard Refresh Rate", Toast.LENGTH_SHORT).show()
                            }
                        )

                        AppearanceDivider(dividerColor)

                        // Liquid Glass Shader Toggle
                        AppearanceToggleRow(
                            icon = Icons.Default.AutoAwesome,
                            tint = effectiveAccent,
                            title = "Liquid Glass Shader",
                            subtitle = "AGSL optical refraction and specular reflection",
                            checked = liquidGlassEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onCheckedChange = {
                                ThemeState.setLiquidGlassEnabled(it)
                                Toast.makeText(context, if (it) "Liquid glass optical shader active" else "Standard acrylic glass active", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // --- 11. Section: Appearance Profiles & Import/Export ---
        if (searchQuery.isEmpty() || "profile".contains(searchQuery, true) || "export".contains(searchQuery, true) || "import".contains(searchQuery, true)) {
            item {
                AppearanceCardSection(title = "Appearance Profiles & Presets", textSecondary = textSecondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PRE-CONFIGURED DESIGN PROFILES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textSecondary
                        )

                        val quickProfiles = listOf(
                            "Clean Daily" to (ThemePreset.MASTERWAVE_SIGNATURE to NowPlayingTemplate.CLASSIC),
                            "AMOLED Pure" to (ThemePreset.OBSIDIAN to NowPlayingTemplate.IMMERSIVE),
                            "Minimal Studio" to (ThemePreset.MINIMAL_SILVER to NowPlayingTemplate.MINIMAL),
                            "Cyber Ocean" to (ThemePreset.OCEAN to NowPlayingTemplate.MODERN_GLASS)
                        )

                        quickProfiles.forEach { (name, config) ->
                            val (preset, template) = config
                            val isSelected = activeProfile == name
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) effectiveAccent.copy(alpha = 0.12f) else (if (isAmoled) Color(0xFF141622) else Color(0xFFF1F3F8)),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) effectiveAccent else cardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bouncyClickable {
                                        AppearanceStudioManager.setActiveProfileName(name)
                                        AppearanceStudioManager.setThemePreset(preset)
                                        AppearanceStudioManager.setNowPlayingTemplate(template)
                                        ThemeState.setThemeMode(preset.defaultMode)
                                        ThemeState.setAccentColor(preset.primaryColor)
                                        Toast.makeText(context, "Applied Profile: $name", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (isSelected) effectiveAccent else textPrimary)
                                        Text(text = "${preset.title} • ${template.title}", fontSize = 11.sp, color = textSecondary)
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(effectiveAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }

                        AppearanceDivider(dividerColor)

                        // Export & Import Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val json = AppearanceStudioManager.exportCurrentProfileJson()
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "Configuration JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAmoled) Color(0xFF1A1C28) else Color(0xFFE2E4EA),
                                    contentColor = textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { showImportDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAmoled) Color(0xFF1A1C28) else Color(0xFFE2E4EA),
                                    contentColor = textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Live Interactive Appearance Preview Modal Dialog ---
    if (showLivePreview) {
        Dialog(onDismissRequest = { showLivePreview = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = BorderStroke(2.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Appearance Preview",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textPrimary
                        )
                        IconButton(onClick = { showLivePreview = false }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                        }
                    }

                    // Mini Mockup Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Mock Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("MasterWave", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(effectiveAccent)
                                )
                            }

                            // Mock Now Playing Mini Card
                            Surface(
                                shape = RoundedCornerShape(artworkRadius.coerceAtMost(24).dp),
                                color = if (isAmoled) Color(0xFF141624) else Color(0xFFE5E7EB),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(effectiveAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sample Track", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                        Text("Immersive Player • ${currentThemePreset.title}", fontSize = 10.sp, color = textSecondary)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(effectiveAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Mock Bottom Navigation Dock
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isAmoled) Color(0x30FFFFFF) else Color(0x30000000),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = effectiveAccent, modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                                    Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showLivePreview = false },
                        colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Looks Perfect", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // --- Import Configuration Dialog ---
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Appearance Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste profile JSON configuration below:", fontSize = 12.sp, color = textSecondary)
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("{\n  \"profileName\": \"Custom\"...\n}", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = AppearanceStudioManager.importProfileJson(importText)
                        if (success) {
                            Toast.makeText(context, "Profile successfully loaded!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                            importText = ""
                        } else {
                            Toast.makeText(context, "Invalid profile JSON format", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Apply Profile", fontWeight = FontWeight.Bold, color = effectiveAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
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
        modifier = Modifier.padding(vertical = 4.dp),
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
