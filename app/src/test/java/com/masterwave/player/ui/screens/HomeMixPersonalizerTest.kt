package com.masterwave.player.ui.screens

import com.masterwave.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMixPersonalizerTest {
    @Test
    fun ranksArtistsUsingFavoritesAndRecentPlays() {
        val mixes = HomeMixPersonalizer.build(
            favorites = listOf(
                track("a", "Artist A"),
                track("b", "Artist B")
            ),
            recentTracks = listOf(track("c", "Artist A")),
            limit = 2
        )

        assertEquals(listOf("Artist A", "Artist B"), mixes.map { it.label })
        assertTrue(mixes.all { it.kind == HomeMixKind.ARTIST })
        assertEquals("Artist A songs", mixes.first().searchQuery)
    }

    @Test
    fun fallsBackToStarterMixesForNewListeners() {
        val mixes = HomeMixPersonalizer.build(emptyList(), emptyList(), limit = 2)

        assertEquals(listOf("Top Hits", "Acoustic Sessions"), mixes.map { it.label })
        assertTrue(mixes.all { it.kind == HomeMixKind.STARTER })
    }

    @Test
    fun ignoresPlaceholderMetadataAndNonPositiveLimits() {
        val mixes = HomeMixPersonalizer.build(
            favorites = listOf(track("a", "Unknown Artist")),
            recentTracks = emptyList(),
            limit = 2
        )

        assertTrue(mixes.all { it.kind == HomeMixKind.STARTER })
        assertEquals(emptyList<HomeMixOption>(), HomeMixPersonalizer.build(emptyList(), emptyList(), limit = 0))
    }

    private fun track(id: String, artist: String): Track = Track(
        id = id,
        title = "Track $id",
        artist = artist,
        album = "Album",
        durationSeconds = 180,
        artworkUrl = null
    )
}
