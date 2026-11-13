package com.masterwave.player.data.recommendation

import com.masterwave.player.data.model.Track
import java.util.Calendar
import kotlin.random.Random

enum class MusicMood(val label: String, val keywords: List<String>) {
    ENERGETIC("Energetic & Party", listOf("beat", "dance", "remix", "party", "club", "trap", "dj", "bass", "rap", "drill", "fast", "drill", "high")),
    ROMANCE("Romance & Melodic", listOf("love", "dil", "pyar", "ishq", "tera", "saiyaara", "tum", "heart", "soul", "romantic", "sanwali", "meri", "humdum")),
    CHILL("Chill & Relax", listOf("chill", "lofi", "acoustic", "slow", "peace", "sahiba", "night", "breeze", "soft", "calm", "relax", "coffee", "breathe")),
    MELANCHOLY("Deep & Emotional", listOf("sad", "tears", "alone", "dard", "judaai", "cry", "broken", "pain", "dark", "khamoshi")),
    FOCUS("Focus & Flow", listOf("ambient", "piano", "synth", "wave", "focus", "study", "instrumental", "deep", "meditation", "theta"))
}

data class RecommendationReason(
    val track: Track,
    val reason: String
)

class AiMusicRecommender {

    /**
     * Classifies a track into an emotional mood vector using semantic analysis.
     */
    fun detectMood(track: Track): MusicMood {
        val text = "${track.title} ${track.artist} ${track.album}".lowercase()
        for (mood in MusicMood.values()) {
            if (mood.keywords.any { text.contains(it) }) {
                return mood
            }
        }
        // Time of day fallback
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11 -> MusicMood.ENERGETIC
            hour in 12..17 -> MusicMood.FOCUS
            hour in 18..22 -> MusicMood.ROMANCE
            else -> MusicMood.CHILL
        }
    }

    /**
     * AI Weighted Multi-Factor Ranking Engine:
     * - Artist match: +6.0
     * - Mood alignment: +5.0
     * - User Favorite bonus: +4.0
     * - Recency fatigue penalty: -10.0
     * - Controlled entropy jitter: +0..2.5
     */
    fun selectNextTrack(
        currentTrack: Track?,
        candidatePool: List<Track>,
        recentlyPlayed: List<Track>
    ): Track? {
        if (candidatePool.isEmpty()) return null
        if (currentTrack == null) return candidatePool.randomOrNull()

        val currentMood = detectMood(currentTrack)
        val recentIds = recentlyPlayed.take(8).map { it.id }.toSet()

        // Filter out tracks just played to prevent fatigue
        val freshCandidates = candidatePool.filter { it.id != currentTrack.id && !recentIds.contains(it.id) }
            .ifEmpty { candidatePool.filter { it.id != currentTrack.id } }

        if (freshCandidates.isEmpty()) return candidatePool.firstOrNull()

        return freshCandidates.maxByOrNull { candidate ->
            var score = 0f

            // 1. Mood alignment
            if (detectMood(candidate) == currentMood) {
                score += 5.0f
            }

            // 2. Artist affinity
            if (candidate.artist.equals(currentTrack.artist, ignoreCase = true)) {
                score += 6.0f
            } else if (candidate.artist.contains(currentTrack.artist, ignoreCase = true) ||
                currentTrack.artist.contains(candidate.artist, ignoreCase = true)
            ) {
                score += 3.5f
            }

            // 3. User Favorite bonus
            if (candidate.isFavorite) {
                score += 4.0f
            }

            // 4. Controlled entropy jitter to avoid monotonic loops
            score += Random.nextFloat() * 2.5f

            score
        } ?: freshCandidates.random()
    }

    /**
     * Finds tracks similar to a given artist or track.
     */
    fun recommendSimilar(
        seedTrack: Track,
        pool: List<Track>,
        count: Int = 6
    ): List<Track> {
        val targetMood = detectMood(seedTrack)
        return pool.filter { it.id != seedTrack.id }
            .sortedByDescending { candidate ->
                var score = 0f
                if (candidate.artist.equals(seedTrack.artist, ignoreCase = true)) score += 8f
                if (detectMood(candidate) == targetMood) score += 4f
                if (candidate.isFavorite) score += 2f
                score += Random.nextFloat() * 1.5f
                score
            }
            .take(count)
    }

    /**
     * Generates an Infinite Radio stream based on seed track.
     */
    fun generateInfiniteRadio(
        seedTrack: Track,
        pool: List<Track>,
        size: Int = 15
    ): List<Track> {
        val list = mutableListOf(seedTrack)
        val remaining = pool.filter { it.id != seedTrack.id }.toMutableList()

        var current = seedTrack
        while (list.size < size && remaining.isNotEmpty()) {
            val next = selectNextTrack(current, remaining, list) ?: remaining.removeAt(0)
            list.add(next)
            remaining.remove(next)
            current = next
        }
        return list
    }
}
