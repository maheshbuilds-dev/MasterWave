package com.masterwave.player.ui.screens

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHeroRecommendationsTest {
    @Test
    fun prioritizesPlayablePersonalizedTracksAndRemovesDuplicates() {
        val first = track("1")
        val duplicate = first.copy(title = "Duplicate title")
        val unavailable = track("2").copy(availability = TrackAvailability.UNAVAILABLE)

        val selection = HomeHeroRecommendations.build(
            personalizedTracks = listOf(first, duplicate, unavailable),
            trendingTracks = listOf(track("popular")),
            limit = 3
        )

        assertTrue(selection.isPersonalized)
        assertEquals(listOf("1", "popular"), selection.tracks.map { it.id })
    }

    @Test
    fun fallsBackToPlayableTrendingTracksWhenPersonalRecommendationsAreEmpty() {
        val unavailable = track("unavailable").copy(availability = TrackAvailability.METADATA_ONLY)

        val selection = HomeHeroRecommendations.build(
            personalizedTracks = listOf(unavailable),
            trendingTracks = listOf(track("popular")),
            limit = 3
        )

        assertFalse(selection.isPersonalized)
        assertEquals(listOf("popular"), selection.tracks.map { it.id })
    }

    @Test
    fun nonPositiveLimitReturnsNoTracks() {
        val selection = HomeHeroRecommendations.build(
            personalizedTracks = listOf(track("1")),
            trendingTracks = emptyList(),
            limit = 0
        )

        assertTrue(selection.tracks.isEmpty())
        assertFalse(selection.isPersonalized)
    }

    @Test
    fun defaultCarouselShowsTwentyFourDistinctRecommendationsWhenAvailable() {
        val candidates = (1..30).map { track(it.toString()) }

        val selection = HomeHeroRecommendations.build(
            personalizedTracks = candidates,
            trendingTracks = emptyList()
        )

        assertEquals(24, selection.tracks.size)
        assertEquals(24, selection.tracks.map { it.id }.distinct().size)
    }

    @Test
    fun removesSameSongWithDifferentProviderIds() {
        val original = track("1")
        val duplicate = track("2").copy(title = original.title, artist = original.artist)

        val selection = HomeHeroRecommendations.build(
            personalizedTracks = listOf(original, duplicate),
            trendingTracks = emptyList(),
            limit = 5
        )

        assertEquals(listOf("1"), selection.tracks.map { it.id })
    }

    @Test
    fun avoidsRecentAndPreviousCarouselSongsWhenEnoughFreshCandidatesExist() {
        val recent = track("recent")
        val previous = track("previous")
        val fresh = (1..3).map { track("fresh-$it") }

        val selection = HomeHeroRecommendations.build(
            personalizedTracks = listOf(recent, previous) + fresh,
            trendingTracks = emptyList(),
            recentlyPlayedTracks = listOf(recent),
            previousCarouselTracks = listOf(previous),
            limit = 3
        )

        assertEquals(fresh.map { it.id }, selection.tracks.map { it.id })
    }

    @Test
    fun rotatesPreviousCarouselSongsBackInOnlyWhenFreshPoolIsShort() {
        val previous = track("previous")
        val fresh = track("fresh")

        val selection = HomeHeroRecommendations.build(
            personalizedTracks = listOf(previous, fresh),
            trendingTracks = emptyList(),
            previousCarouselTracks = listOf(previous),
            limit = 2
        )

        assertEquals(listOf("fresh", "previous"), selection.tracks.map { it.id })
    }

    @Test
    fun quickPicksExcludeCarouselAndRecentlyPlayedSongs() {
        val carousel = track("carousel")
        val recent = track("recent")
        val picks = (1..3).map { track("pick-$it") }

        val selection = HomeHeroRecommendations.buildQuickPicks(
            personalizedTracks = listOf(carousel, recent) + picks,
            trendingTracks = emptyList(),
            carouselTracks = listOf(carousel),
            recentlyPlayedTracks = listOf(recent),
            limit = 3
        )

        assertEquals(picks.map { it.id }, selection.map { it.id })
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Song $id",
        artist = "Artist"
    )
}
