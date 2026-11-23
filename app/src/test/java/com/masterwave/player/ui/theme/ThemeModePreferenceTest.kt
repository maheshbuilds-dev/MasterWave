package com.masterwave.player.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModePreferenceTest {
    @Test
    fun restoresEverySavedThemeMode() {
        assertEquals(ThemeMode.LIGHT, themeModeFromPreference("LIGHT"))
        assertEquals(ThemeMode.DARK, themeModeFromPreference("DARK"))
        assertEquals(ThemeMode.AMOLED, themeModeFromPreference("AMOLED"))
        assertEquals(ThemeMode.SYSTEM, themeModeFromPreference("SYSTEM"))
        assertEquals(ThemeMode.AMOLED, themeModeFromPreference("AMOLED_DARK"))
    }

    @Test
    fun unknownThemePreferenceUsesDefaultMode() {
        assertEquals(ThemeMode.LIGHT, themeModeFromPreference(null))
        assertEquals(ThemeMode.LIGHT, themeModeFromPreference("invalid"))
    }
}
