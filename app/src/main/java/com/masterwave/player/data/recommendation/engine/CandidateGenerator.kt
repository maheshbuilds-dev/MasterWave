package com.masterwave.player.data.recommendation.engine

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.YouTubeMusicProvider
import com.masterwave.player.data.recommendation.CommunityPlaylistsProvider
import com.masterwave.player.data.recommendation.LanguageMusicRecommender
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Multi-source Candidate Generator pulling real tracks from InnerTube, NewPipe, and local library.
 */
class CandidateGenerator(
    private val ytProvider: YouTubeMusicProvider,
    private val repository: MusicRepository
) {
    /**
     * Generates candidates related to a specific seed track (e.g. for Now Playing & Queue next).
     */
    suspend fun getCandidatesForSeed(seedTrack: Track, limit: Int = 15): List<Track> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<Track>()

        // 1. Search artist top songs
        try {
            val artistRes = ytProvider.search("${seedTrack.artist} songs")
            candidates.addAll(artistRes.songs.filter { it.id != seedTrack.id })
        } catch (_: Exception) {}

        // 2. Search related genre / track context
        if (candidates.size < limit) {
            try {
                val query = "${seedTrack.artist} ${seedTrack.title}".trim()
                val relatedRes = ytProvider.search(query)
                candidates.addAll(relatedRes.songs.filter { it.id != seedTrack.id })
            } catch (_: Exception) {}
        }

        candidates.distinctBy { it.id }.take(limit)
    }

    /**
     * Generates candidates across the user's top affinity artists.
     */
    suspend fun getCandidatesFromTopArtists(artists: List<String>, limitPerArtist: Int = 6): List<Track> = coroutineScope {
        artists.take(5).map { artistName ->
            async(Dispatchers.IO) {
                try {
                    val res = ytProvider.search(artistName)
                    res.songs.take(limitPerArtist)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }
    }

    /**
     * Generates discovery candidates from high-performing viral and community playlists.
     */
    suspend fun getDiscoveryCandidates(languageHint: String = "Trending"): List<Track> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<Track>()

        // 1. Language viral recommendations
        try {
            val (_, viral) = LanguageMusicRecommender.getLanguageViralTracks(repository, null)
            candidates.addAll(viral)
        } catch (_: Exception) {}

        // 2. Curated community query candidates
        try {
            val communityPlaylists = CommunityPlaylistsProvider.getPlaylistsForLanguage("Tamil") +
                    CommunityPlaylistsProvider.getPlaylistsForLanguage("Hindi")
            val topPicks = communityPlaylists.shuffled().take(2)
            topPicks.forEach { cp ->
                val searchRes = ytProvider.search(cp.searchQuery)
                candidates.addAll(searchRes.songs.take(6))
            }
        } catch (_: Exception) {}

        candidates.distinctBy { it.id }
    }
}
