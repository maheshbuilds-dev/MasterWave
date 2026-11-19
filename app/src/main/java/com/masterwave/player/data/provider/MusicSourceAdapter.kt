package com.masterwave.player.data.provider

import com.masterwave.player.data.model.Track

/**
 * Common normalized interface for music streaming and discovery providers.
 */
interface MusicSourceAdapter {
    val providerId: String
    val displayName: String
    val isAuthorizedStream: Boolean
    val requiresAuthentication: Boolean
        get() = false
    val attributionNotice: String?
        get() = null

    suspend fun search(query: String, limit: Int = 20): List<Track>
    suspend fun getTrending(limit: Int = 20): List<Track>
    suspend fun getTracksByGenre(genre: String, limit: Int = 20): List<Track> = emptyList()
    suspend fun resolveStreamUrl(track: Track): String?
    suspend fun getTrackDetails(trackId: String): Track? = null
}
