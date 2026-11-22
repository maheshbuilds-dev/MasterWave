package com.masterwave.player.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.masterwave.player.MasterWaveApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED,
    SYSTEM
}

internal fun themeModeFromPreference(saved: String?): ThemeMode = when (saved) {
    "AMOLED", "AMOLED_DARK" -> ThemeMode.AMOLED
    "DARK" -> ThemeMode.DARK
    "LIGHT" -> ThemeMode.LIGHT
    "SYSTEM" -> ThemeMode.SYSTEM
    else -> ThemeMode.LIGHT
}

internal fun isDarkThemeMode(mode: ThemeMode, systemInDark: Boolean): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.AMOLED -> true
    ThemeMode.SYSTEM -> systemInDark
}

enum class ThemeAccentPreset(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    APPLE_MUSIC(
        title = "Apple Music",
        subtitle = "Signature rose red & warm coral",
        primaryColor = Color(0xFFFA2D48),
        secondaryColor = Color(0xFFFF6482)
    ),
    AUTOMATIC(
        title = "Automatic",
        subtitle = "Adapts live to currently playing album artwork",
        primaryColor = Color(0xFFFA2D48),
        secondaryColor = Color(0xFFFF6482)
    ),
    OCEAN(
        title = "Ocean",
        subtitle = "Pacific deep blue & marine teal",
        primaryColor = Color(0xFF0EA5E9),
        secondaryColor = Color(0xFF14B8A6)
    ),
    BERRY(
        title = "Berry",
        subtitle = "Muted velvet rose & deep burgundy",
        primaryColor = Color(0xFFE11D48),
        secondaryColor = Color(0xFFBE123C)
    ),
    FOREST(
        title = "Sage Forest",
        subtitle = "Natural sage emerald & calm woodland green",
        primaryColor = Color(0xFF236B43),
        secondaryColor = Color(0xFF388E5E)
    ),
    SUNSET(
        title = "Sunset",
        subtitle = "Warm amber glow & dusk coral",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFF97316)
    ),
    MONOCHROME(
        title = "Monochrome",
        subtitle = "Pure minimalist white, silver & slate",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFF94A3B8)
    )
}

object ThemeState {
    private const val PREFS_NAME = "masterwave_theme_prefs_v2"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_PRESET = "theme_accent_preset"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
    private const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
    private const val KEY_WAVY_SEEKBAR = "wavy_seekbar_enabled"
    private const val KEY_HIGH_REFRESH_RATE = "high_refresh_rate_enabled"
    private const val KEY_APP_FONT = "app_font"
    private const val KEY_ANIMATED_ALBUM_ART = "animated_album_art_enabled"

    private val prefs: SharedPreferences by lazy {
        MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val Presets = listOf(
        Color(0xFFFA2D48) to "Apple Music Red",
        Color(0xFF00D4B2) to "MasterWave Turquoise",
        Color(0xFF0EA5E9) to "Ocean Blue",
        Color(0xFFE11D48) to "Velvet Berry",
        Color(0xFF10B981) to "Emerald Forest",
        Color(0xFFF59E0B) to "Sunset Amber",
        Color(0xFFFFFFFF) to "Monochrome White"
    )

    private val _themeMode = MutableStateFlow(
        try {
            themeModeFromPreference(prefs.getString(KEY_THEME_MODE, null))
        } catch (_: Exception) {
            ThemeMode.AMOLED
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentPreset = MutableStateFlow(
        try {
            val saved = prefs.getString(KEY_ACCENT_PRESET, ThemeAccentPreset.APPLE_MUSIC.name)
            ThemeAccentPreset.valueOf(saved ?: ThemeAccentPreset.APPLE_MUSIC.name)
        } catch (_: Exception) {
            ThemeAccentPreset.APPLE_MUSIC
        }
    )
    val accentPreset: StateFlow<ThemeAccentPreset> = _accentPreset.asStateFlow()

    // Isolated artwork color extracted from current album cover
    private val _artworkColor = MutableStateFlow<Color?>(null)
    val artworkColor: StateFlow<Color?> = _artworkColor.asStateFlow()

    // Full multi-tone dynamic palette extracted from current song thumbnail
    private val _currentArtworkPalette = MutableStateFlow<ArtworkColorPalette?>(null)
    val currentArtworkPalette: StateFlow<ArtworkColorPalette?> = _currentArtworkPalette.asStateFlow()

    // Dynamic artwork tinting is enabled by default so ambient bleed reflects song thumbnail colors.
    private val _dynamicColorEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    // Effective reactive accent color (combines preset and dynamic artwork)
    private val _effectiveAccentColor = MutableStateFlow(computeEffectiveAccent())
    val effectiveAccentColor: StateFlow<Color> = _effectiveAccentColor.asStateFlow()

    // Backwards-compatible alias for existing composables
    val accentColor: StateFlow<Color> = _effectiveAccentColor.asStateFlow()

    private val _liquidGlassEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_LIQUID_GLASS, true)
    )
    val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    private val _highRefreshRateEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_HIGH_REFRESH_RATE, true)
    )
    val highRefreshRateEnabled: StateFlow<Boolean> = _highRefreshRateEnabled.asStateFlow()

    private val _wavySeekbarEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_WAVY_SEEKBAR, true)
    )
    val wavySeekbarEnabled: StateFlow<Boolean> = _wavySeekbarEnabled.asStateFlow()

    private val _selectedFont = MutableStateFlow(
        try {
            AppFont.fromId(prefs.getString(KEY_APP_FONT, AppFont.DEFAULT.id))
        } catch (_: Exception) {
            AppFont.DEFAULT
        }
    )
    val selectedFont: StateFlow<AppFont> = _selectedFont.asStateFlow()

    private val _animatedAlbumArtEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ANIMATED_ALBUM_ART, true)
    )
    val animatedAlbumArtEnabled: StateFlow<Boolean> = _animatedAlbumArtEnabled.asStateFlow()

    init {
        // App is exclusively powered by Fucek typography
    }

    private fun computeEffectiveAccent(): Color {
        val preset = _accentPreset.value
        return if (preset == ThemeAccentPreset.AUTOMATIC && _dynamicColorEnabled.value) {
            _artworkColor.value ?: preset.primaryColor
        } else {
            preset.primaryColor
        }
    }

    fun setArtworkPalette(palette: ArtworkColorPalette?) {
        _currentArtworkPalette.value = palette
        _artworkColor.value = palette?.vibrantColor ?: palette?.dominantColor
        _effectiveAccentColor.value = computeEffectiveAccent()
    }

    fun setArtworkColor(color: Color?) {
        _artworkColor.value = color
        _effectiveAccentColor.value = computeEffectiveAccent()
    }

    fun setAccentPreset(preset: ThemeAccentPreset) {
        _accentPreset.value = preset
        _effectiveAccentColor.value = computeEffectiveAccent()
        prefs.edit().putString(KEY_ACCENT_PRESET, preset.name).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        _liquidGlassEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply()
    }

    fun setHighRefreshRateEnabled(enabled: Boolean) {
        _highRefreshRateEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HIGH_REFRESH_RATE, enabled).apply()
    }

    fun setWavySeekbarEnabled(enabled: Boolean) {
        _wavySeekbarEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WAVY_SEEKBAR, enabled).apply()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        _effectiveAccentColor.value = computeEffectiveAccent()
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setAccentColor(color: Color) {
        // Find nearest preset or fallback
        _effectiveAccentColor.value = color
        prefs.edit().putInt(KEY_ACCENT_COLOR, color.toArgb()).apply()
    }

    fun setSelectedFont(font: AppFont) {
        _selectedFont.value = AppFont.FUCEK
        prefs.edit().putString(KEY_APP_FONT, AppFont.FUCEK.id).apply()
    }

    fun setAnimatedAlbumArtEnabled(enabled: Boolean) {
        _animatedAlbumArtEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ANIMATED_ALBUM_ART, enabled).apply()
    }

    fun currentFontFamily(): androidx.compose.ui.text.font.FontFamily {
        return _selectedFont.value.toFontFamily()
    }

    fun isAmoledDark(): Boolean {
        return _themeMode.value == ThemeMode.AMOLED
    }

    @Composable
    fun isDarkTheme(): Boolean {
        val mode by themeMode.collectAsState()
        val systemInDark = isSystemInDarkTheme()
        return isDarkThemeMode(mode, systemInDark)
    }

    @Composable
    fun isAmoledTheme(): Boolean {
        val mode by themeMode.collectAsState()
        val systemInDark = isSystemInDarkTheme()
        return isDarkThemeMode(mode, systemInDark)
    }
}
