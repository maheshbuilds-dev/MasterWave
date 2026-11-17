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

object ThemeState {
    private const val PREFS_NAME = "masterwave_theme_prefs_v2"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
    private const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
    private const val KEY_WAVY_SEEKBAR = "wavy_seekbar_enabled"

    private val prefs: SharedPreferences by lazy {
        MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Default accent: MasterWave Turquoise (#00D4B2)
    val Presets = listOf(
        Color(0xFF00D4B2) to "MasterWave Turquoise",
        Color(0xFF00B4D8) to "Electric Cyan",
        Color(0xFF10B981) to "Emerald Pulse",
        Color(0xFF6366F1) to "Deep Indigo",
        Color(0xFF8B5CF6) to "Electric Violet",
        Color(0xFFF43F5E) to "Crimson Flame",
        Color(0xFFF59E0B) to "Amber Gold"
    )

    private val _themeMode = MutableStateFlow(
        try {
            val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.AMOLED.name)
            when (saved) {
                "AMOLED", "AMOLED_DARK" -> ThemeMode.AMOLED
                "DARK" -> ThemeMode.DARK
                "LIGHT" -> ThemeMode.AMOLED // Auto-upgrade legacy light mode to AMOLED Dark for MasterWave 2.0
                "SYSTEM" -> ThemeMode.AMOLED
                else -> ThemeMode.AMOLED
            }
        } catch (_: Exception) {
            ThemeMode.AMOLED
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(
        Color(prefs.getInt(KEY_ACCENT_COLOR, Color(0xFF00D4B2).toArgb()))
    )
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    // Isolated artwork color for Now Playing ambient backdrop (does NOT mutate global app headers)
    private val _artworkColor = MutableStateFlow<Color?>(null)
    val artworkColor: StateFlow<Color?> = _artworkColor.asStateFlow()

    fun setArtworkColor(color: Color?) {
        _artworkColor.value = color
    }

    private val _dynamicColorEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _liquidGlassEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_LIQUID_GLASS, true)
    )
    val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    private val _wavySeekbarEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_WAVY_SEEKBAR, true)
    )
    val wavySeekbarEnabled: StateFlow<Boolean> = _wavySeekbarEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        _liquidGlassEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply()
    }

    fun setWavySeekbarEnabled(enabled: Boolean) {
        _wavySeekbarEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WAVY_SEEKBAR, enabled).apply()
    }

    fun setAccentColor(color: Color) {
        _accentColor.value = color
        prefs.edit().putInt(KEY_ACCENT_COLOR, color.toArgb()).apply()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun isAmoledDark(): Boolean {
        return _themeMode.value == ThemeMode.AMOLED
    }

    @Composable
    fun isDarkTheme(): Boolean {
        val mode by themeMode.collectAsState()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.SYSTEM -> true
        }
    }

    @Composable
    fun isAmoledTheme(): Boolean {
        val mode by themeMode.collectAsState()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.SYSTEM -> true
        }
    }
}
