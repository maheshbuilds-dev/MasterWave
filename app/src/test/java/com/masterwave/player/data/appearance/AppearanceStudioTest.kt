package com.masterwave.player.data.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceStudioTest {

    @Test
    fun testAllThemePresetsHaveNonEmptyTitlesAndColors() {
        for (preset in ThemePreset.values()) {
            assertTrue("Title must not be blank for ${preset.name}", preset.title.isNotBlank())
            assertTrue("Subtitle must not be blank for ${preset.name}", preset.subtitle.isNotBlank())
            assertNotNull("Primary color must exist for ${preset.name}", preset.primaryColor)
            assertNotNull("Secondary color must exist for ${preset.name}", preset.secondaryColor)
        }
    }

    @Test
    fun testAllNowPlayingTemplatesAreDefined() {
        val expected = listOf("CLASSIC", "IMMERSIVE", "MINIMAL", "COMPACT", "MODERN_GLASS")
        for (name in expected) {
            val template = NowPlayingTemplate.valueOf(name)
            assertNotNull(template)
            assertTrue(template.title.isNotBlank())
        }
    }

    @Test
    fun testAllNowPlayingBgStylesAreDefined() {
        val expected = listOf("BLURRED_ARTWORK", "SOLID", "GRADIENT", "CINEMATIC")
        for (name in expected) {
            val style = NowPlayingBgStyle.valueOf(name)
            assertNotNull(style)
            assertTrue(style.title.isNotBlank())
        }
    }

    @Test
    fun testAllIconStylesAreDefined() {
        val expected = listOf("MINIMAL_OUTLINE", "ROUNDED_OUTLINE", "FILLED", "DUOTONE", "SOFT_ROUNDED", "MONOCHROME")
        for (name in expected) {
            val style = AppIconStyle.valueOf(name)
            assertNotNull(style)
            assertTrue(style.title.isNotBlank())
        }
    }

    @Test
    fun testAllLibraryLayoutsAreDefined() {
        val expected = listOf("LIST", "GRID", "COMPACT")
        for (name in expected) {
            val layout = LibraryLayout.valueOf(name)
            assertNotNull(layout)
            assertTrue(layout.title.isNotBlank())
        }
    }
}
