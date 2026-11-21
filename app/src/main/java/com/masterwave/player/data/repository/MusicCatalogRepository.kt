package com.masterwave.player.data.repository

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.LocalMusicSourceAdapter
import com.masterwave.player.data.provider.MusicSourceAdapter
import com.masterwave.player.data.provider.YouTubeMusicSourceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Aggregates, deduplicates, and resolves playback streams across local files and YouTube Music.
 */
class MusicCatalogRepository(
    val localAdapter: MusicSourceAdapter,
    val youtubeAdapter: MusicSourceAdapter
) {
    val allAdapters: List<MusicSourceAdapter> = listOf(
        localAdapter,
        youtubeAdapter
    )

    private val adapterMap: Map<String, MusicSourceAdapter> = allAdapters.associateBy { it.providerId }

    /**
     * Search tracks across all registered providers in parallel with deduplication.
     */
    suspend fun searchAll(
        query: String,
        filterProviderId: String? = null,
        limitPerProvider: Int = 15,
        totalLimit: Int = 40
    ): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val targetAdapters = if (!filterProviderId.isNullOrBlank()) {
            allAdapters.filter { it.providerId == filterProviderId }
        } else {
            allAdapters
        }

        val deferredResults = targetAdapters.map { adapter ->
            async {
                withTimeoutOrNull(4500L) {
                    try {
                        adapter.search(query, limitPerProvider)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }
        }

        val rawResults = deferredResults.awaitAll().flatten()
        deduplicateTracks(rawResults).take(totalLimit)
    }

    /**
     * Get trending music from YouTube Music.
     */
    suspend fun getTrendingAll(limit: Int = 30): List<Track> = withContext(Dispatchers.IO) {
        val trending = withTimeoutOrNull(4000L) {
            try { youtubeAdapter.getTrending(limit) } catch (_: Exception) { emptyList() }
        } ?: emptyList()

        deduplicateTracks(trending).take(limit)
    }

    /**
     * Dedicated Indian music discovery through YouTube Music.
     */
    suspend fun getIndianMusic(
        category: String,
        limit: Int = 25
    ): List<Track> = withContext(Dispatchers.IO) {
        val searchQuery = when (category.lowercase()) {
            "hindustani", "hindustani classical" -> "Hindustani classical raga"
            "carnatic", "carnatic classical" -> "Carnatic classical sangeet"
            "devotional" -> "bhajan kirtan stuti devotional"
            "sufi", "ghazal" -> "sufi ghazal qawwali"
            "tamil" -> "Tamil songs"
            "telugu" -> "Telugu songs"
            "hindi", "bollywood" -> "Hindi songs"
            "punjabi" -> "Punjabi songs"
            "malayalam" -> "Malayalam songs"
            "kannada" -> "Kannada songs"
            "bengali" -> "Bengali Rabindra Sangeet"
            "marathi" -> "Marathi Natyasangeet Bhavgeet"
            else -> "$category Indian music"
        }

        val tracks = withTimeoutOrNull(4500L) {
            try {
                youtubeAdapter.search(searchQuery, limit)
            } catch (_: Exception) {
                emptyList()
            }
        }

        deduplicateTracks(tracks ?: emptyList()).take(limit)
    }

    /**
     * Resolves a stream from local files or YouTube Music.
     */
    suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        // 1. Direct local file or existing absolute file path
        if (track.isLocal && !track.localUri.isNullOrBlank()) {
            return@withContext track.localUri
        }
        if (track.providerId !in removedProviderIds) {
            val existingStream = track.streamUrl
            if (!existingStream.isNullOrBlank()) {
                if (existingStream.startsWith("http://") || existingStream.startsWith("https://") || existingStream.startsWith("content://")) {
                    return@withContext existingStream
                }
                val f = java.io.File(existingStream.removePrefix("file://"))
                if (f.exists()) return@withContext f.absolutePath
            }
        }

        // 2. Query target adapter by providerId
        val adapter = adapterMap[track.providerId]
        if (adapter != null) {
            try {
                val resolved = adapter.resolveStreamUrl(track)
                if (!resolved.isNullOrBlank()) {
                    return@withContext resolved
                }
            } catch (_: Exception) {}
        }

        // 3. Graceful fallback: Search YouTube Music by title and artist
        val fallbackQuery = "${track.title} ${track.artist}".trim()
        if (track.providerId != "youtube") {
            try {
                val ytCandidates = youtubeAdapter.search(fallbackQuery, 3)
                val bestYt = ytCandidates.firstOrNull()
                if (bestYt != null) {
                    return@withContext youtubeAdapter.resolveStreamUrl(bestYt)
                }
            } catch (_: Exception) {}
        }

        null
    }

    /**
     * Deduplicates tracks, preferring local files over YouTube Music results.
     */
    fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<Track>()

        val prioritized = tracks.sortedBy { track ->
            when (track.providerId) {
                "local" -> 0
                "youtube" -> 1
                else -> 2
            }
        }

        for (track in prioritized) {
            val key = normalizeKey(track.title, track.artist)
            if (seenKeys.add(key)) {
                result.add(track)
            }
        }

        return result
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase()
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
        val cleanArtist = artist.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
        return "$cleanTitle::$cleanArtist"
    }

    private companion object {
        val removedProviderIds = setOf("audius", "internet_archive", "jamendo")
    }
}
