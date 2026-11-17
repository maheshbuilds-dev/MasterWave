package com.masterwave.player.data.recommendation.engine

import kotlin.math.exp
import kotlin.math.ln

/**
 * Affinity record for a music artist with exponential time decay.
 */
data class ArtistAffinity(
    val artistName: String,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val completionCount: Int = 0,
    val lastPlayedEpochMs: Long = System.currentTimeMillis(),
    val rawScore: Float = 0f
) {
    /**
     * Calculates time-decayed affinity score.
     * Uses exponential decay with a 14-day half-life so older habits gently recede
     * while preserving baseline preferences.
     */
    fun getDecayedScore(nowMs: Long = System.currentTimeMillis(), halfLifeDays: Double = 14.0): Float {
        val daysElapsed = ((nowMs - lastPlayedEpochMs).coerceAtLeast(0L) / (1000.0 * 60 * 60 * 24))
        val lambda = ln(2.0) / halfLifeDays
        val decay = exp(-lambda * daysElapsed).toFloat()
        // Retain 15% minimum baseline for artists the user played many times
        val baseRetention = (completionCount * 0.5f).coerceAtMost(5.0f)
        return (rawScore * decay) + baseRetention
    }
}

/**
 * Affinity record for an individual track.
 */
data class TrackAffinity(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String? = null,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val completionCount: Int = 0,
    val lastPlayedEpochMs: Long = System.currentTimeMillis(),
    val rawScore: Float = 0f,
    val isFavorite: Boolean = false
) {
    fun getDecayedScore(nowMs: Long = System.currentTimeMillis()): Float {
        val daysElapsed = ((nowMs - lastPlayedEpochMs).coerceAtLeast(0L) / (1000.0 * 60 * 60 * 24))
        val lambda = ln(2.0) / 10.0 // 10 day half-life for individual tracks
        val decay = exp(-lambda * daysElapsed).toFloat()
        val favBonus = if (isFavorite) 6.0f else 0f
        return (rawScore * decay) + favBonus
    }
}

/**
 * Comprehensive User Taste Profile.
 */
data class TasteProfile(
    val artistAffinities: Map<String, ArtistAffinity> = emptyMap(),
    val trackAffinities: Map<String, TrackAffinity> = emptyMap(),
    val genreAffinities: Map<String, Float> = emptyMap(),
    val languageAffinities: Map<String, Float> = emptyMap(),
    val dislikedArtists: Set<String> = emptySet(),
    val dislikedTracks: Set<String> = emptySet(),
    val recentTrackIds: List<String> = emptyList(),
    val totalListeningSeconds: Long = 0L,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)
