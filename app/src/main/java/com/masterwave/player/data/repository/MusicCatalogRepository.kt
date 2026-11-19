package com.masterwave.player.data.repository

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability
import com.masterwave.player.data.provider.AudiusSourceAdapter
import com.masterwave.player.data.provider.InternetArchiveSourceAdapter
import com.masterwave.player.data.provider.JamendoSourceAdapter
import com.masterwave.player.data.provider.LocalMusicSourceAdapter
import com.masterwave.player.data.provider.MusicSourceAdapter
import com.masterwave.player.data.provider.YouTubeMusicSourceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Unified multi-source music repository.
 * Aggregates, deduplicates, and resolves playback streams across local files,
 * decentralized Audius streams, Internet Archive collections, Jamendo, and YouTube Music.
 */
class MusicCatalogRepository(
    val localAdapter: MusicSourceAdapter,
    val audiusAdapter: MusicSourceAdapter,
    val internetArchiveAdapter: MusicSourceAdapter,
    val jamendoAdapter: MusicSourceAdapter,
    val youtubeAdapter: MusicSourceAdapter
) {
    val allAdapters: List<MusicSourceAdapter> = listOf(
        localAdapter,
        audiusAdapter,
        internetArchiveAdapter,
        jamendoAdapter,
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
     * Get trending music across open and authorized catalogues.
     */
    suspend fun getTrendingAll(limit: Int = 30): List<Track> = withContext(Dispatchers.IO) {
        val deferredAudius = async {
            withTimeoutOrNull(4000L) {
                try { audiusAdapter.getTrending(limit) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }
        val deferredArchive = async {
            withTimeoutOrNull(4000L) {
                try { internetArchiveAdapter.getTrending(limit) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }
        val deferredYouTube = async {
            withTimeoutOrNull(4000L) {
                try { youtubeAdapter.getTrending(limit) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }

        val combined = deferredAudius.await() + deferredArchive.await() + deferredYouTube.await()
        deduplicateTracks(combined).take(limit)
    }

    /**
     * Dedicated Indian music discovery across authorized public domain, classical archives,
     * and contemporary catalogs.
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

        val deferredArchive = async {
            withTimeoutOrNull(4500L) {
                try {
                    internetArchiveAdapter.search(searchQuery, limit)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

        val deferredYouTube = async {
            withTimeoutOrNull(4500L) {
                try {
                    youtubeAdapter.search(searchQuery, limit)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

        val combined = deferredArchive.await() + deferredYouTube.await()
        deduplicateTracks(combined).take(limit)
    }

    /**
     * Resolves the playable audio stream URL for a given track.
     * Tries the native adapter first, followed by graceful fallback across other adapters.
     */
    suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        // 1. Direct local file or existing absolute file path
        if (track.isLocal && !track.localUri.isNullOrBlank()) {
            return@withContext track.localUri
        }
        val existingStream = track.streamUrl
        if (!existingStream.isNullOrBlank()) {
            if (existingStream.startsWith("http://") || existingStream.startsWith("https://") || existingStream.startsWith("content://")) {
                return@withContext existingStream
            }
            val f = java.io.File(existingStream.removePrefix("file://"))
            if (f.exists()) return@withContext f.absolutePath
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

        // 3. Graceful fallback: Search on authorized adapters by Title + Artist
        val fallbackQuery = "${track.title} ${track.artist}".trim()
        if (fallbackQuery.isNotBlank() && track.providerId != "audius") {
            try {
                val audiusCandidates = audiusAdapter.search(fallbackQuery, 3)
                val bestAudius = audiusCandidates.firstOrNull { candidate ->
                    isFuzzyMatch(candidate.title, track.title)
                }
                if (bestAudius != null) {
                    val resolved = audiusAdapter.resolveStreamUrl(bestAudius)
                    if (!resolved.isNullOrBlank()) return@withContext resolved
                }
            } catch (_: Exception) {}
        }

        // 4. Secondary fallback: YouTube Music resolver if not already tried
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
     * Deduplicates a list of tracks, prioritizing local and authorized full-length recordings.
     */
    fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<Track>()

        // Sort to prioritize local > audius > internet_archive > jamendo > youtube
        val prioritized = tracks.sortedBy { track ->
            when (track.providerId) {
                "local" -> 0
                "audius" -> 1
                "internet_archive" -> 2
                "jamendo" -> 3
                else -> 4
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

    private fun isFuzzyMatch(str1: String, str2: String): Boolean {
        val c1 = str1.lowercase().replace(Regex("[^a-z0-9]"), "")
        val c2 = str2.lowercase().replace(Regex("[^a-z0-9]"), "")
        return c1.contains(c2) || c2.contains(c1)
    }
}
