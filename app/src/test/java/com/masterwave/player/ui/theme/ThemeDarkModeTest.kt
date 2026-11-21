package com.masterwave.player.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeDarkModeTest {
    @Test
    fun darkSurfaceFlagFollowsSelectedTheme() {
        assertFalse(isDarkThemeMode(ThemeMode.LIGHT, systemInDark = true))
        assertTrue(isDarkThemeMode(ThemeMode.DARK, systemInDark = false))
        assertTrue(isDarkThemeMode(ThemeMode.AMOLED, systemInDark = false))
        assertTrue(isDarkThemeMode(ThemeMode.SYSTEM, systemInDark = true))
        assertFalse(isDarkThemeMode(ThemeMode.SYSTEM, systemInDark = false))
    }
}
