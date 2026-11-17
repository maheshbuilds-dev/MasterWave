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
    private const val PREFS_NAME = "masterwave_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
    private const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
    private const val KEY_WAVY_SEEKBAR = "wavy_seekbar_enabled"

    private val prefs: SharedPreferences by lazy {
        MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Default accent: Primary Green (#165B33 / #34C759) or Crimson/Maroon
    val Presets = listOf(
        Color(0xFF165B33) to "Emerald Forest",
        Color(0xFF34C759) to "Neon Lime",
        Color(0xFFE53935) to "Crimson Flame",
        Color(0xFF880E4F) to "Royal Maroon",
        Color(0xFFF06292) to "Cyberpunk Rose",
        Color(0xFF7C4DFF) to "Electric Violet",
        Color(0xFF2979FF) to "Sapphire Blue",
        Color(0xFFFF9100) to "Amber Sunset"
    )

    private val _themeMode = MutableStateFlow(
        try {
            val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
            // Handle backwards compatibility with AMOLED_DARK
            if (saved == "AMOLED_DARK") ThemeMode.AMOLED else ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(
        Color(prefs.getInt(KEY_ACCENT_COLOR, Color(0xFF165B33).toArgb()))
    )
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

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
        val systemDark = isSystemInDarkTheme()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.AMOLED -> true
            ThemeMode.SYSTEM -> systemDark
        }
    }

    @Composable
    fun isAmoledTheme(): Boolean {
        val mode by themeMode.collectAsState()
        return mode == ThemeMode.AMOLED
    }
}
