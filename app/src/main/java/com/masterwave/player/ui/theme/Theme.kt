package com.masterwave.player.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Pure AMOLED Dark Color Scheme (True Pitch Black #000000 for OLED efficiency)
private fun createAmoledDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.35f),
    onPrimaryContainer = Color.White,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    tertiary = AccentRose,
    background = Color(0xFF000000), // Pure AMOLED Pitch Black
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF08080A), // Deepest obsidian surface
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF121216),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF24242E)
)

// Light Color Scheme
private fun createLightColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = accent,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    tertiary = AccentRose,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun MasterWaveTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeState.themeMode.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.AMOLED_DARK -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = if (isDark) {
        createAmoledDarkColorScheme(accentColor)
    } else {
        createLightColorScheme(accentColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = (if (isDark) Color(0xFF000000) else LightBackground).toArgb()
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
