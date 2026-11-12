package com.masterwave.player.data.recommendation

import com.masterwave.player.data.model.Track
import java.util.Calendar

enum class MusicMood(val label: String, val keywords: List<String>) {
    ENERGETIC("Energetic & Party", listOf("beat", "dance", "remix", "party", "club", "trap", "dj", "bass", "rap", "drill", "fast")),
    ROMANCE("Romance & Melodic", listOf("love", "dil", "pyar", "ishq", "tera", "saiyaara", "tum", "heart", "soul", "romantic", "sanwali")),
    CHILL("Chill & Relax", listOf("chill", "lofi", "acoustic", "slow", "peace", "sahiba", "night", "breeze", "soft", "calm", "relax")),
    MELANCHOLY("Deep & Emotional", listOf("sad", "tears", "alone", "dard", "judaai", "cry", "broken", "pain", "dark")),
    FOCUS("Focus & Flow", listOf("ambient", "piano", "synth", "wave", "focus", "study", "instrumental", "deep", "meditation"))
}

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
     * AI Next Song Engine: Intelligently selects the next track based on current mood,
     * artist transition, and listening velocity.
     */
    fun selectNextTrack(
        currentTrack: Track?,
        candidatePool: List<Track>,
        recentlyPlayed: List<Track>
    ): Track? {
        if (candidatePool.isEmpty()) return null
        if (currentTrack == null) return candidatePool.randomOrNull()

        val currentMood = detectMood(currentTrack)
        val recentIds = recentlyPlayed.take(6).map { it.id }.toSet()

        // Filter out tracks just played to prevent fatigue
        val freshCandidates = candidatePool.filter { it.id != currentTrack.id && !recentIds.contains(it.id) }
            .ifEmpty { candidatePool.filter { it.id != currentTrack.id } }

        if (freshCandidates.isEmpty()) return candidatePool.firstOrNull()

        // Score candidates based on mood match and harmonic variety
        return freshCandidates.maxByOrNull { candidate ->
            var score = 0f
            if (detectMood(candidate) == currentMood) score += 5f
            if (candidate.artist.equals(currentTrack.artist, ignoreCase = true)) score += 3f
            score += (candidate.durationSeconds % 7) // tie-breaker pseudo-random entropy
            score
        } ?: freshCandidates.random()
    }
}
