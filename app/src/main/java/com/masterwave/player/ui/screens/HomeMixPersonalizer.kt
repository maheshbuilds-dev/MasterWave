package com.masterwave.player.ui.screens

import com.masterwave.player.data.model.Track
import java.util.Locale

internal enum class HomeMixKind {
    ARTIST,
    STARTER
}

internal data class HomeMixOption(
    val label: String,
    val searchQuery: String,
    val kind: HomeMixKind
)

internal object HomeMixPersonalizer {
    private data class WeightedLabel(
        val label: String,
        val score: Int
    )

    private val starterMixes = listOf(
        "Top Hits" to "global top music hits",
        "Acoustic Sessions" to "acoustic studio live sessions",
        "Electronic Wave" to "synthwave electronic music hits",
        "Late Night Beats" to "late night lo-fi instrumental beats",
        "Indie Essentials" to "indie rock alternative essentials",
        "Deep Bass Radio" to "heavy bass edm electronic music",
        "Soundtrack Anthems" to "epic cinematic soundtrack songs"
    )

    fun build(
        favorites: List<Track>,
        recentTracks: List<Track>,
        limit: Int = 9
    ): List<HomeMixOption> {
        if (limit <= 0) return emptyList()

        val artistScores = linkedMapOf<String, WeightedLabel>()

        fun addScore(scores: MutableMap<String, WeightedLabel>, rawLabel: String?, weight: Int) {
            val label = rawLabel?.trim()?.takeIf {
                it.isNotEmpty() && it.lowercase(Locale.ROOT) !in excludedLabels
            } ?: return
            val key = label.lowercase(Locale.ROOT)
            val previous = scores[key]
            scores[key] = WeightedLabel(previous?.label ?: label, (previous?.score ?: 0) + weight)
        }

        favorites.forEach { track ->
            addScore(artistScores, track.artist, weight = 4)
        }
        recentTracks.forEachIndexed { index, track ->
            val weight = (5 - index / 4).coerceAtLeast(1)
            addScore(artistScores, track.artist, weight)
        }

        val artists = artistScores.values
            .sortedWith(compareByDescending<WeightedLabel> { it.score }.thenBy { it.label.lowercase(Locale.ROOT) })
            .map { HomeMixOption(it.label, "${it.label} songs", HomeMixKind.ARTIST) }

        if (artists.isNotEmpty()) {
            return (artists + starterMixes.map { (title, query) ->
                HomeMixOption(title, query, HomeMixKind.STARTER)
            }).take(limit)
        }

        return starterMixes.take(limit).map { (title, query) ->
            HomeMixOption(title, query, HomeMixKind.STARTER)
        }
    }

    private val excludedLabels = setOf(
        "unknown",
        "unknown artist",
        "various artists",
        "artist",
        "archive audio"
    )
}
