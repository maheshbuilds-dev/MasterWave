package com.masterwave.player.ui.screens

import com.masterwave.player.data.model.Track
import java.util.Locale

internal data class HomeHeroSelection(
    val tracks: List<Track>,
    val isPersonalized: Boolean
)

internal object HomeHeroRecommendations {
    fun build(
        personalizedTracks: List<Track>,
        trendingTracks: List<Track>,
        recentlyPlayedTracks: List<Track> = emptyList(),
        previousCarouselTracks: List<Track> = emptyList(),
        limit: Int = 24
    ): HomeHeroSelection {
        if (limit <= 0) return HomeHeroSelection(emptyList(), isPersonalized = false)

        val personalizedKeys = personalizedTracks.mapTo(mutableSetOf(), ::identity)
        val candidates = (personalizedTracks + trendingTracks)
            .filter { it.isPlayable }
        val recentlyPlayedIds = recentlyPlayedTracks.mapTo(mutableSetOf()) { it.id }
        val recentlyPlayedKeys = recentlyPlayedTracks.mapTo(mutableSetOf(), ::identity)
        val previousIds = previousCarouselTracks.mapTo(mutableSetOf()) { it.id }
        val previousKeys = previousCarouselTracks.mapTo(mutableSetOf(), ::identity)

        val fresh = select(candidates) { track ->
            track.id !in recentlyPlayedIds &&
                identity(track) !in recentlyPlayedKeys &&
                track.id !in previousIds &&
                identity(track) !in previousKeys
        }.take(limit)
        val selected = if (fresh.size >= limit) {
            fresh
        } else {
            val selectedIds = fresh.mapTo(mutableSetOf()) { it.id }
            val selectedKeys = fresh.mapTo(mutableSetOf(), ::identity)
            val fill = select(candidates) { track ->
                track.id !in recentlyPlayedIds &&
                    identity(track) !in recentlyPlayedKeys &&
                    track.id !in selectedIds &&
                    identity(track) !in selectedKeys
            }
            (fresh + fill).take(limit)
        }
        return HomeHeroSelection(
            tracks = selected,
            isPersonalized = selected.any { identity(it) in personalizedKeys }
        )
    }

    fun buildQuickPicks(
        personalizedTracks: List<Track>,
        trendingTracks: List<Track>,
        carouselTracks: List<Track>,
        recentlyPlayedTracks: List<Track>,
        limit: Int = 24
    ): List<Track> {
        if (limit <= 0) return emptyList()

        val excludedIds = (carouselTracks + recentlyPlayedTracks).mapTo(mutableSetOf()) { it.id }
        val excludedKeys = (carouselTracks + recentlyPlayedTracks).mapTo(mutableSetOf(), ::identity)
        return select(personalizedTracks + trendingTracks) { track ->
            track.id !in excludedIds && identity(track) !in excludedKeys
        }.take(limit)
    }

    private fun select(
        candidates: List<Track>,
        include: (Track) -> Boolean
    ): List<Track> {
        val seenIds = mutableSetOf<String>()
        val seenSongs = mutableSetOf<String>()
        return candidates.filter { track ->
            val songIdentity = identity(track)
            track.isPlayable &&
                include(track) &&
                seenIds.add(track.id) &&
                seenSongs.add(songIdentity)
        }
    }

    private fun identity(track: Track): String {
        fun normalize(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        return "${normalize(track.title)}|${normalize(track.artist)}"
    }
}
