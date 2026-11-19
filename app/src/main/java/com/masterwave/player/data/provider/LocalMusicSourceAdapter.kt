package com.masterwave.player.data.provider

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability

/**
 * Adapter for on-device local music files discovered via MediaStore.
 */
class LocalMusicSourceAdapter(
    private val localMusicProvider: LocalMusicProvider
) : MusicSourceAdapter {

    override val providerId: String = "local"
    override val displayName: String = "Local Device"
    override val isAuthorizedStream: Boolean = true
    override val requiresAuthentication: Boolean = false
    override val attributionNotice: String = "Local storage on device."

    override suspend fun search(query: String, limit: Int): List<Track> {
        val tracks = localMusicProvider.loadLocalTracks()
        if (query.isBlank()) return tracks.take(limit)
        val q = query.trim().lowercase()
        return tracks.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }.take(limit)
    }

    override suspend fun getTrending(limit: Int): List<Track> {
        return localMusicProvider.loadLocalTracks().take(limit)
    }

    override suspend fun resolveStreamUrl(track: Track): String? {
        return track.localUri ?: track.streamUrl
    }
}
