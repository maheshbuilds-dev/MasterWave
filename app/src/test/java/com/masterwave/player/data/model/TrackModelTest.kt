package com.masterwave.player.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackModelTest {

    @Test
    fun testTrackAvailabilityAndPlayability() {
        val fullTrack = Track(
            id = "track_1",
            title = "Fragments",
            artist = "Xsetra",
            availability = TrackAvailability.FULL_TRACK
        )
        assertTrue(fullTrack.isPlayable)

        val previewTrack = Track(
            id = "preview_1",
            title = "Preview Song",
            artist = "Artist",
            availability = TrackAvailability.PREVIEW_ONLY
        )
        assertTrue(previewTrack.isPlayable)

        val metadataTrack = Track(
            id = "lastfm_1",
            title = "Metadata Only",
            artist = "Artist",
            availability = TrackAvailability.METADATA_ONLY
        )
        assertFalse(metadataTrack.isPlayable)

        val unavailableTrack = Track(
            id = "unavail_1",
            title = "Unavailable",
            artist = "Artist",
            availability = TrackAvailability.UNAVAILABLE
        )
        assertFalse(unavailableTrack.isPlayable)
    }

    @Test
    fun testDurationFormatting() {
        val track1 = Track(id = "1", title = "T", artist = "A", durationSeconds = 215)
        assertEquals("3:35", track1.durationFormatted)

        val track2 = Track(id = "2", title = "T", artist = "A", durationSeconds = 65)
        assertEquals("1:05", track2.durationFormatted)

        val track3 = Track(id = "3", title = "T", artist = "A", durationSeconds = 0)
        assertEquals("0:00", track3.durationFormatted)
    }
}
