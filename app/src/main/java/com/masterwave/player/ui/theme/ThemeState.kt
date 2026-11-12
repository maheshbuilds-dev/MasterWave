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
    AMOLED_DARK,
    SYSTEM
}

object ThemeState {
    private const val PREFS_NAME = "masterwave_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"

    private val prefs: SharedPreferences by lazy {
        MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Default accent: Royal Maroon / Crimson / Rose
    val Presets = listOf(
        Color(0xFFE53935) to "Crimson Flame",
        Color(0xFF880E4F) to "Royal Maroon",
        Color(0xFFF06292) to "Cyberpunk Rose",
        Color(0xFF7C4DFF) to "Electric Violet",
        Color(0xFF2979FF) to "Sapphire Blue",
        Color(0xFF00E676) to "Neon Emerald",
        Color(0xFFFF9100) to "Amber Sunset",
        Color(0xFFFFD700) to "Obsidian Gold"
    )

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.AMOLED_DARK.name) ?: ThemeMode.AMOLED_DARK.name)
        } catch (_: Exception) {
            ThemeMode.AMOLED_DARK
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(
        Color(prefs.getInt(KEY_ACCENT_COLOR, Color(0xFF880E4F).toArgb()))
    )
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setAccentColor(color: Color) {
        _accentColor.value = color
        prefs.edit().putInt(KEY_ACCENT_COLOR, color.toArgb()).apply()
    }

    fun isAmoledDark(): Boolean {
        return _themeMode.value == ThemeMode.AMOLED_DARK
    }

    @Composable
    fun isAmoledTheme(): Boolean {
        val mode by themeMode.collectAsState()
        val systemDark = isSystemInDarkTheme()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.AMOLED_DARK -> true
            ThemeMode.SYSTEM -> systemDark
        }
    }
}
