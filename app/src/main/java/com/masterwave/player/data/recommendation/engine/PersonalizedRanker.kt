package com.masterwave.player.data.recommendation.engine

import com.masterwave.player.data.model.Track
import kotlin.random.Random

/**
 * Multi-factor Scoring and Diversity Ranking Engine.
 *
 * Scoring Formula:
 * S = (w_artist * A_decayed) + (w_track * T_decayed) + (w_fav * FavBonus) + (w_fresh * Freshness) - RepetitionPenalty
 *
 * Enforces:
 * 1. Hard filtering of blacklisted artists and tracks.
 * 2. Strict anti-clustering: Max 2 songs per artist in any 10-track recommendation shelf.
 * 3. Recent-fatigue suppression: Tracks played in the last 12-24 hours receive heavy score penalties.
 */
class PersonalizedRanker {

    data class ScoredTrack(
        val track: Track,
        val score: Float,
        val reason: String
    )

    fun rankCandidates(
        candidates: List<Track>,
        profile: TasteProfile,
        seedTrack: Track? = null,
        maxResults: Int = 15,
        maxPerArtist: Int = 2
    ): List<Track> {
        val now = System.currentTimeMillis()
        val recentIds = profile.recentTrackIds.take(15).toSet()

        // 1. Hard filter blacklisted artists and tracks
        val eligible = candidates.filter { track ->
            !profile.dislikedTracks.contains(track.id) &&
            !profile.dislikedArtists.any { it.equals(track.artist, ignoreCase = true) } &&
            (seedTrack == null || track.id != seedTrack.id)
        }

        // 2. Score each eligible track
        val scoredList = eligible.map { track ->
            var score = 0f
            var primaryReason = "Recommended for you"

            // A. Artist Affinity (Decayed)
            val artistAff = profile.artistAffinities.entries.firstOrNull {
                it.key.equals(track.artist, ignoreCase = true)
            }?.value
            if (artistAff != null) {
                val decayedArtistScore = artistAff.getDecayedScore(now)
                score += decayedArtistScore * 0.40f
                if (decayedArtistScore > 2.0f) {
                    primaryReason = "Because you like ${track.artist}"
                }
            }

            // B. Seed Track Similarity (if applicable)
            if (seedTrack != null) {
                if (seedTrack.artist.equals(track.artist, ignoreCase = true)) {
                    score += 5.0f
                    primaryReason = "More by ${seedTrack.artist}"
                } else if (seedTrack.album.isNotBlank() && seedTrack.album.equals(track.album, ignoreCase = true)) {
                    score += 4.0f
                    primaryReason = "From album ${seedTrack.album}"
                }
            }

            // C. Track-level Affinity & Favorites
            val trackAff = profile.trackAffinities[track.id]
            if (trackAff != null) {
                score += trackAff.getDecayedScore(now) * 0.30f
            }
            if (track.isFavorite) {
                score += 4.5f
                primaryReason = "From your favorites"
            }

            // D. Recent Repetition Fatigue Penalty
            if (recentIds.contains(track.id)) {
                score -= 8.0f // Heavy penalty for tracks just listened to
            }

            // E. Controlled exploration jitter (prevents deterministic starvation)
            score += Random.nextFloat() * 1.2f

            ScoredTrack(track, score, primaryReason)
        }.sortedByDescending { it.score }

        // 3. Apply Diversity & Anti-Clustering Constraints
        val result = mutableListOf<Track>()
        val artistCounts = mutableMapOf<String, Int>()

        for (scored in scoredList) {
            val normalizedArtist = scored.track.artist.trim().lowercase()
            val currentCount = artistCounts[normalizedArtist] ?: 0
            if (currentCount < maxPerArtist) {
                result.add(scored.track)
                artistCounts[normalizedArtist] = currentCount + 1
                if (result.size >= maxResults) break
            }
        }

        return result
    }
}
