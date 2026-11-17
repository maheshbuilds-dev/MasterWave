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
    tertiary = AccentLime,
    background = AmoledBackground, // #000000 Pure Pitch Black
    onBackground = Color(0xFFFFFFFF),
    surface = AmoledSurface, // Deepest surface
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = AmoledSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = AmoledBorder
)

// Standard Dark Color Scheme (#0D0F0E Layered Dark Charcoal)
private fun createDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.28f),
    onPrimaryContainer = Color.White,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    tertiary = AccentLime,
    background = DarkBackground, // #0D0F0E
    onBackground = DarkTextPrimary, // #F2F4F3
    surface = DarkSurface, // #141716
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

// Light Color Scheme (#F6F5F3 Default Light Palette)
private fun createLightColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = LightSoftTint,
    onPrimaryContainer = LightPrimaryGreen,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    tertiary = LightPrimaryGreen,
    background = LightBackground, // #F6F5F3
    onBackground = LightTextPrimary, // #1C1B1F
    surface = LightSurface, // #FFFFFF
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant, // #F0EFEB
    onSurfaceVariant = LightTextSecondary, // #5A5A66
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
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> createLightColorScheme(accentColor)
        ThemeMode.DARK -> createDarkColorScheme(accentColor)
        ThemeMode.AMOLED -> createAmoledDarkColorScheme(accentColor)
        ThemeMode.SYSTEM -> if (systemInDark) createDarkColorScheme(accentColor) else createLightColorScheme(accentColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = (when (themeMode) {
                ThemeMode.AMOLED -> AmoledBackground
                ThemeMode.DARK -> DarkBackground
                ThemeMode.LIGHT -> LightBackground
                ThemeMode.SYSTEM -> if (systemInDark) DarkBackground else LightBackground
            }).toArgb()
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
