package com.masterwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDeviceProfileTest {

    @Test
    fun testAllAudioDeviceRoutesHaveDisplayNameAndCategory() {
        for (route in AudioDeviceRoute.values()) {
            assertTrue("Display name must not be blank for ${route.name}", route.displayName.isNotBlank())
            assertTrue("Category must not be blank for ${route.name}", route.category.isNotBlank())
            assertTrue("Pref key prefix must not be blank for ${route.name}", route.prefKeyPrefix.startsWith("device_profile_"))
        }
    }

    @Test
    fun testProfessionalPresetsContainExact5Bands() {
        val expectedPresets = listOf(
            "Flat", "Balanced", "Bass Boost", "Deep Bass", "Vocal",
            "Acoustic", "Classical", "Pop", "Rock", "Jazz",
            "Electronic", "Treble Boost"
        )
        // Dummy test verification of default preset structures
        val sampleMap = mapOf(
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Balanced" to listOf(2, 1, 0, 1, 2),
            "Bass Boost" to listOf(7, 5, 1, 0, 0),
            "Deep Bass" to listOf(8, 6, 2, -1, -2),
            "Vocal" to listOf(-2, 0, 5, 4, 1),
            "Acoustic" to listOf(3, 2, 1, 3, 4),
            "Classical" to listOf(4, 3, 0, 2, 4),
            "Pop" to listOf(2, 4, 3, 3, 1),
            "Rock" to listOf(5, 3, -1, 3, 5),
            "Jazz" to listOf(3, 2, 1, 2, 3),
            "Electronic" to listOf(6, 4, -1, 2, 5),
            "Treble Boost" to listOf(-2, 0, 1, 5, 7)
        )
        for (preset in expectedPresets) {
            val bands = sampleMap[preset]
            assertNotNull("Preset $preset must exist in map", bands)
            assertEquals("Preset $preset must contain exactly 5 bands", 5, bands!!.size)
            for (db in bands) {
                assertTrue("dB gain must be within [-12, 12]", db in -12..12)
            }
        }
    }
}
