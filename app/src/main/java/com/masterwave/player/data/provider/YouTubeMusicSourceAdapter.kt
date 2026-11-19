package com.masterwave.player.data.provider

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability

/**
 * YouTube Music source adapter.
 * Provides transparent attribution, external app links ("Open in YouTube Music"),
 * and fallback stream resolution while maintaining legal transparency.
 */
class YouTubeMusicSourceAdapter(
    val ytProvider: YouTubeMusicProvider
) : MusicSourceAdapter {

    override val providerId: String = "youtube"
    override val displayName: String = "YouTube Music"
    override val isAuthorizedStream: Boolean = false // Transparent legal delineation
    override val requiresAuthentication: Boolean = false
    override val attributionNotice: String = "YouTube Music catalog. External listening available via official YouTube Music app."

    override suspend fun search(query: String, limit: Int): List<Track> {
        val result = ytProvider.search(query)
        return result.songs.take(limit).map { normalizeTrack(it) }
    }

    override suspend fun getTrending(limit: Int): List<Track> {
        val trending = ytProvider.getTrending()
        return trending.take(limit).map { normalizeTrack(it) }
    }

    override suspend fun resolveStreamUrl(track: Track): String? {
        val rawId = track.id.removePrefix("yt_")
        return try {
            ytProvider.resolveStreamUrl(rawId)
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeTrack(track: Track): Track {
        val rawId = track.id.removePrefix("yt_")
        return track.copy(
            id = "yt_$rawId",
            providerId = providerId,
            externalSourceUrl = "https://music.youtube.com/watch?v=$rawId",
            attribution = "YouTube Music (id: $rawId)",
            availability = TrackAvailability.FULL_TRACK
        )
    }
}
