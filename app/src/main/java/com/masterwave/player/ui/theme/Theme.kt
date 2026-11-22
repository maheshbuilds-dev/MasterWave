package com.masterwave.player.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
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

// Pure AMOLED Color Scheme — true pitch black for OLED efficiency.
private fun createAmoledDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.24f),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = AppleMusicRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0A16),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = AppleMusicIndigo,
    onTertiary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141416),
    onSurfaceVariant = AMDarkTextSecondary,
    surfaceTint = accent,
    surfaceDim = Color.Black,
    surfaceBright = AMDarkSurfaceHighest,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF121214),
    surfaceContainerHigh = Color(0xFF1A1A1C),
    surfaceContainerHighest = Color(0xFF242426),
    outline = AMDarkOutline,
    outlineVariant = AMDarkSeparator,
    inverseSurface = Color.White,
    inverseOnSurface = AMLightTextPrimary,
    inversePrimary = AppleMusicRedDeep,
    error = Color(0xFFFF453A),
    onError = Color.White,
    scrim = Color.Black
)

// Standard dark scheme — layered charcoal, Apple Music dark surfaces.
private fun createDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.22f),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = AppleMusicRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0A16),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = AppleMusicIndigo,
    onTertiary = Color.White,
    background = AMDarkBackground,
    onBackground = AMDarkTextPrimary,
    surface = AMDarkBackground,
    onSurface = AMDarkTextPrimary,
    surfaceVariant = AMDarkSurface,
    onSurfaceVariant = AMDarkTextSecondary,
    surfaceTint = accent,
    surfaceDim = AMDarkBackground,
    surfaceBright = AMDarkSurfaceHighest,
    surfaceContainerLowest = Color(0xFF08080A),
    surfaceContainerLow = Color(0xFF121214),
    surfaceContainer = AMDarkSurface,
    surfaceContainerHigh = AMDarkSurfaceHigh,
    surfaceContainerHighest = AMDarkSurfaceHighest,
    outline = AMDarkOutline,
    outlineVariant = AMDarkSeparator,
    inverseSurface = Color.White,
    inverseOnSurface = AMLightTextPrimary,
    inversePrimary = AppleMusicRedDeep,
    error = Color(0xFFFF453A),
    onError = Color.White,
    scrim = Color.Black
)

// Light scheme — MasterWave Warm Light Website Canvas (#fffaf0) with tactile bento surfaces.
private fun createLightColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.14f),
    onPrimaryContainer = WebInk,
    secondary = WebAccentPink,
    onSecondary = Color.White,
    secondaryContainer = WebAccentPeach.copy(alpha = 0.3f),
    onSecondaryContainer = WebInk,
    tertiary = WebAccentViolet,
    onTertiary = Color.White,
    background = WebCanvas,
    onBackground = WebInk,
    surface = WebCanvas,
    onSurface = WebInk,
    surfaceVariant = WebSurfaceCard,
    onSurfaceVariant = WebBody,
    surfaceTint = accent,
    surfaceDim = WebSurfaceSoft,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WebSurfaceCard,
    surfaceContainer = WebSurfaceCard,
    surfaceContainerHigh = WebSurfaceSoft,
    surfaceContainerHighest = WebSurfaceSoft,
    outline = WebInk,
    outlineVariant = WebHairline,
    inverseSurface = WebInk,
    inverseOnSurface = Color.White,
    inversePrimary = WebAccentPink,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    scrim = Color.Black
)

private fun MaterialTypography.withForegroundColor(color: Color) = copy(
    displayLarge = displayLarge.copy(color = color),
    displayMedium = displayMedium.copy(color = color),
    displaySmall = displaySmall.copy(color = color),
    headlineLarge = headlineLarge.copy(color = color),
    headlineMedium = headlineMedium.copy(color = color),
    headlineSmall = headlineSmall.copy(color = color),
    titleLarge = titleLarge.copy(color = color),
    titleMedium = titleMedium.copy(color = color),
    titleSmall = titleSmall.copy(color = color),
    bodyLarge = bodyLarge.copy(color = color),
    bodyMedium = bodyMedium.copy(color = color),
    bodySmall = bodySmall.copy(color = color),
    labelLarge = labelLarge.copy(color = color),
    labelMedium = labelMedium.copy(color = color),
    labelSmall = labelSmall.copy(color = color)
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

    val colorScheme = if (isDark) {
        createAmoledDarkColorScheme(accentColor)
    } else {
        createLightColorScheme(accentColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography.withForegroundColor(colorScheme.onBackground),
        shapes = ExpressiveShapes,
        content = content
    )
}
