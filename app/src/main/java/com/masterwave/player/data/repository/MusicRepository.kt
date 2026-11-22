package com.masterwave.player.data.repository

import android.content.Context
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.lyrics.LrclibLyricsProvider
import com.masterwave.player.data.model.Artist
import com.masterwave.player.data.model.LyricsResult
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.SearchResult
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.LocalMusicProvider
import com.masterwave.player.data.provider.PlaylistImporter
import com.masterwave.player.data.provider.YouTubeMusicProvider
import com.masterwave.player.data.provider.LocalMusicSourceAdapter
import com.masterwave.player.data.provider.YouTubeMusicSourceAdapter
import com.masterwave.player.data.provider.LastFmDiscoveryAdapter
import com.masterwave.player.data.recommendation.engine.RecommendationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PlayHistoryItem(
    val track: Track,
    val playedAt: Long = System.currentTimeMillis()
)

class MusicRepository(private val context: Context) {

    val okHttpClient = MasterWaveApp.instance.okHttpClient
    private val ytProvider = YouTubeMusicProvider(MasterWaveApp.instance.okHttpClient)
    private val localProvider = LocalMusicProvider(context)
    private val lyricsProvider = LrclibLyricsProvider(MasterWaveApp.instance.okHttpClient)
    private val playlistImporter = PlaylistImporter(MasterWaveApp.instance.okHttpClient)

    val localAdapter = LocalMusicSourceAdapter(localProvider)
    val youtubeAdapter = YouTubeMusicSourceAdapter(ytProvider)
    val lastFmAdapter = LastFmDiscoveryAdapter(okHttpClient)

    val catalogRepository = MusicCatalogRepository(
        localAdapter = localAdapter,
        youtubeAdapter = youtubeAdapter
    )
    val recommendationManager: RecommendationManager by lazy {
        RecommendationManager.getInstance(context, ytProvider, this)
    }

    // Favorites
    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    val favorites: StateFlow<List<Track>> = _favorites.asStateFlow()

    // Followed / Favorite Artists (Persistent, fully editable)
    private val _followedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val followedArtists: StateFlow<List<Artist>> = _followedArtists.asStateFlow()

    // Recent History
    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    // 3-Day Playback History
    private val _playHistory = MutableStateFlow<List<PlayHistoryItem>>(emptyList())
    val playHistory: StateFlow<List<PlayHistoryItem>> = _playHistory.asStateFlow()

    // Local Tracks
    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    // Playlists
    private val defaultPlaylists = listOf(
        Playlist(
            id = "liked_songs",
            title = "Liked Songs",
            description = "Your favorite tracks",
            trackCount = 0,
            source = "Liked",
            isPinned = true,
            createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        ),
        Playlist(
            id = "yt_liked_songs",
            title = "YouTube Liked",
            description = "Your favorite tracks from YouTube Music",
            trackCount = 0,
            source = "YouTube Music",
            isPinned = true,
            createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        ),
        Playlist(
            id = "local_songs",
            title = "Local Audio",
            description = "On-device audio tracks",
            trackCount = 0,
            source = "Local",
            isPinned = true,
            createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        ),
        Playlist(
            id = "custom_empty_1",
            title = "Playlist 1",
            description = "Custom playlist • Tap to add songs",
            trackCount = 0,
            tracks = emptyList(),
            source = "Custom",
            isPinned = false,
            createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        ),
        Playlist(
            id = "custom_empty_2",
            title = "Playlist 2",
            description = "Custom playlist • Tap to add songs",
            trackCount = 0,
            tracks = emptyList(),
            source = "Custom",
            isPinned = false,
            createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        )
    )

    private val _playlists = MutableStateFlow<List<Playlist>>(defaultPlaylists)
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val cachePrefs by lazy {
        context.getSharedPreferences("masterwave_music_cache", Context.MODE_PRIVATE)
    }

    init {
        loadCachedFavorites()
        loadCachedPlaylists()
        loadCachedFollowedArtists()
        loadPlayHistory()
    }

    // Listening Stats
    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts.asStateFlow()

    private val _totalPlays = MutableStateFlow(0)
    val totalPlays: StateFlow<Int> = _totalPlays.asStateFlow()

    fun toggleFavorite(track: Track) {
        val current = _favorites.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            recommendationManager.onUserUnlikedTrack(track)
        } else {
            val fav = track.copy(isFavorite = true)
            current.add(0, fav)
            recommendationManager.onUserLikedTrack(fav)
        }
        _favorites.value = current
        updateLikedPlaylistCount(current.size)
        saveCachedFavorites()
    }

    fun addToFavorites(track: Track) {
        if (!isFavorite(track.id)) {
            val current = _favorites.value.toMutableList()
            val fav = track.copy(isFavorite = true)
            current.add(0, fav)
            recommendationManager.onUserLikedTrack(fav)
            _favorites.value = current
            updateLikedPlaylistCount(current.size)
            saveCachedFavorites()
        }
    }

    fun addAllToFavorites(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val current = _favorites.value.toMutableList()
        val existingIds = current.map { it.id }.toSet()
        val newTracks = tracks.filterNot { it.id in existingIds }.map { it.copy(isFavorite = true) }
        if (newTracks.isNotEmpty()) {
            current.addAll(0, newTracks)
            _favorites.value = current
            updateLikedPlaylistCount(current.size)
            saveCachedFavorites()
        }
    }

    fun isFavorite(trackId: String): Boolean {
        return _favorites.value.any { it.id == trackId }
    }

    // --- Followed Artists API ---
    fun followArtist(artist: Artist) {
        val current = _followedArtists.value.toMutableList()
        val index = current.indexOfFirst {
            it.name.equals(artist.name, ignoreCase = true) || (it.id.isNotBlank() && it.id.equals(artist.id, ignoreCase = true))
        }
        val photo = artist.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: com.masterwave.player.data.recommendation.ArtistPhotoProvider.getPhotoUrlOrNull(artist.name)
        val updated = artist.copy(
            thumbnailUrl = photo,
            isFavorite = true,
            followedAtMs = if (artist.followedAtMs > 0) artist.followedAtMs else System.currentTimeMillis()
        )
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(0, updated)
        }
        _followedArtists.value = current
        saveCachedFollowedArtists()
        recommendationManager.onArtistFollowChanged(artist.name, isFollowing = true)
    }

    fun unfollowArtist(artistName: String) {
        val current = _followedArtists.value.toMutableList()
        val removed = current.removeAll {
            it.name.equals(artistName, ignoreCase = true) || it.id.equals(artistName, ignoreCase = true)
        }
        if (removed) {
            _followedArtists.value = current
            saveCachedFollowedArtists()
            recommendationManager.onArtistFollowChanged(artistName, isFollowing = false)
        }
    }

    fun isArtistFollowed(artistName: String): Boolean {
        if (artistName.isBlank()) return false
        return _followedArtists.value.any {
            it.name.equals(artistName, ignoreCase = true) || it.id.equals(artistName, ignoreCase = true)
        }
    }

    fun toggleFollowArtist(artist: Artist): Boolean {
        return if (isArtistFollowed(artist.name)) {
            unfollowArtist(artist.name)
            false
        } else {
            followArtist(artist)
            true
        }
    }

    private fun updateLikedPlaylistCount(count: Int) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == "liked_songs") {
                pl.copy(
                    trackCount = count,
                    tracks = _favorites.value,
                    artworkUrl = _favorites.value.firstOrNull()?.artworkUrl
                )
            } else {
                pl
            }
        }
    }

    fun recordRecentTrack(track: Track) {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)

        val list = _recentTracks.value.toMutableList()
        list.removeAll { it.id == track.id }
        list.add(0, track)
        _recentTracks.value = list.take(100)

        // 3-Day History List
        val history = _playHistory.value.toMutableList()
        history.add(0, PlayHistoryItem(track, now))
        _playHistory.value = history.filter { it.playedAt >= threeDaysAgo }

        // Increment stats
        _totalPlays.value += 1
        val currentMap = _playCounts.value.toMutableMap()
        currentMap[track.id] = (currentMap[track.id] ?: 0) + 1
        _playCounts.value = currentMap

        savePlayHistory()
    }

    private fun savePlayHistory() {
        try {
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            val valid = _playHistory.value.filter { it.playedAt >= threeDaysAgo }
            val arr = JSONArray()
            valid.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("playedAt", item.playedAt)
                        put("id", item.track.id)
                        put("title", item.track.title)
                        put("artist", item.track.artist)
                        put("album", item.track.album)
                        put("durationSeconds", item.track.durationSeconds)
                        put("artworkUrl", item.track.artworkUrl ?: "")
                        put("streamUrl", item.track.streamUrl ?: "")
                        put("isLocal", item.track.isLocal)
                    }
                )
            }
            cachePrefs.edit()
                .putString("cached_play_history", arr.toString())
                .putInt("cached_total_plays", _totalPlays.value)
                .apply()
        } catch (_: Exception) {}
    }

    private fun loadPlayHistory() {
        try {
            _totalPlays.value = cachePrefs.getInt("cached_total_plays", 0)
            val json = cachePrefs.getString("cached_play_history", null) ?: return
            val arr = JSONArray(json)
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            val list = mutableListOf<PlayHistoryItem>()
            val counts = mutableMapOf<String, Int>()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val playedAt = o.optLong("playedAt", 0L)
                if (playedAt >= threeDaysAgo) {
                    val track = Track(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        album = o.optString("album"),
                        durationSeconds = o.optLong("durationSeconds"),
                        artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() },
                        streamUrl = o.optString("streamUrl").takeIf { it.isNotBlank() },
                        isLocal = o.optBoolean("isLocal", false)
                    )
                    list.add(PlayHistoryItem(track, playedAt))
                    counts[track.id] = (counts[track.id] ?: 0) + 1
                }
            }
            _playHistory.value = list
            _playCounts.value = counts
            if (list.isNotEmpty()) {
                _recentTracks.value = list.map { it.track }.distinctBy { it.id }.take(50)
            }
        } catch (_: Exception) {}
    }

    suspend fun loadLocalTracks(): List<Track> {
        val tracks = localProvider.loadLocalTracks()
        _localTracks.value = tracks
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == "local_songs") {
                pl.copy(trackCount = tracks.size, tracks = tracks)
            } else {
                pl
            }
        }
        return tracks
    }

    suspend fun importYouTubePlaylist(urlOrId: String): Result<Playlist> {
        val result = playlistImporter.importYouTubePlaylist(urlOrId)
        result.onSuccess { newPlaylist ->
            addCustomPlaylist(newPlaylist)
        }
        return result
    }

    suspend fun importSpotifyPlaylist(urlOrId: String): Result<Playlist> {
        val result = playlistImporter.importSpotifyPlaylist(urlOrId)
        result.onSuccess { newPlaylist ->
            addCustomPlaylist(newPlaylist)
        }
        return result
    }

    fun addCustomPlaylists(newPlaylists: List<Playlist>) {
        if (newPlaylists.isEmpty()) return
        synchronized(this) {
            val list = _playlists.value.toMutableList()
            for (playlist in newPlaylists) {
                val index = list.indexOfFirst { it.id == playlist.id }
                if (index != -1) {
                    list[index] = playlist
                } else {
                    list.add(playlist)
                }
            }
            _playlists.value = list
            saveCachedPlaylists()
        }
    }

    fun addCustomPlaylist(playlist: Playlist) {
        addCustomPlaylists(listOf(playlist))
    }

    fun createCustomPlaylist(title: String, description: String = ""): Playlist {
        val newPl = Playlist(
            id = "user_${UUID.randomUUID()}",
            title = title,
            description = description,
            trackCount = 0,
            source = "User",
            isPinned = false,
            createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        )
        addCustomPlaylist(newPl)
        return newPl
    }

    fun createPlaylist(title: String, description: String = ""): Playlist = createCustomPlaylist(title, description)

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        val list = _playlists.value.toMutableList()
        val index = list.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = list[index]
            if (pl.tracks.none { it.id == track.id }) {
                val updatedTracks = pl.tracks + track
                list[index] = pl.copy(
                    tracks = updatedTracks,
                    trackCount = updatedTracks.size,
                    artworkUrl = pl.artworkUrl ?: track.artworkUrl
                )
                _playlists.value = list
                saveCachedPlaylists()
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val list = _playlists.value.toMutableList()
        val index = list.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = list[index]
            val updatedTracks = pl.tracks.filter { it.id != trackId }
            list[index] = pl.copy(
                tracks = updatedTracks,
                trackCount = updatedTracks.size,
                artworkUrl = updatedTracks.firstOrNull()?.artworkUrl
            )
            _playlists.value = list
            saveCachedPlaylists()
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId == "liked_songs" || playlistId == "local_songs" || playlistId == "yt_liked_songs") return
        _playlists.value = _playlists.value.filter { it.id != playlistId }
        saveCachedPlaylists()
    }

    private fun saveCachedFavorites() {
        try {
            val arr = JSONArray()
            _favorites.value.forEach { t ->
                val o = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("album", t.album)
                    put("durationSeconds", t.durationSeconds)
                    put("artworkUrl", t.artworkUrl ?: "")
                    put("streamUrl", t.streamUrl ?: "")
                    put("isLocal", t.isLocal)
                    put("localUri", t.localUri ?: "")
                }
                arr.put(o)
            }
            cachePrefs.edit().putString("cached_favorites", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadCachedFavorites() {
        try {
            val json = cachePrefs.getString("cached_favorites", null) ?: return
            val arr = JSONArray(json)
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Track(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        album = o.optString("album"),
                        durationSeconds = o.optLong("durationSeconds"),
                        artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() },
                        streamUrl = o.optString("streamUrl").takeIf { it.isNotBlank() },
                        isLocal = o.optBoolean("isLocal"),
                        localUri = o.optString("localUri").takeIf { it.isNotBlank() },
                        isFavorite = true
                    )
                )
            }
            if (list.isNotEmpty()) {
                _favorites.value = list
                updateLikedPlaylistCount(list.size)
            }
        } catch (_: Exception) {}
    }

    private fun saveCachedFollowedArtists() {
        try {
            val arr = JSONArray()
            _followedArtists.value.forEach { a ->
                val o = JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("thumbnailUrl", a.thumbnailUrl ?: "")
                    put("subscribers", a.subscribers ?: "")
                    put("description", a.description ?: "")
                    put("isFavorite", a.isFavorite)
                    put("followedAtMs", a.followedAtMs)
                }
                arr.put(o)
            }
            cachePrefs.edit().putString("cached_followed_artists", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadCachedFollowedArtists() {
        try {
            val json = cachePrefs.getString("cached_followed_artists", null)
            if (!json.isNullOrBlank()) {
                val arr = JSONArray(json)
                val list = mutableListOf<Artist>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.optString("name")
                    if (name.isNotBlank()) {
                        list.add(
                            Artist(
                                id = o.optString("id").ifBlank { name },
                                name = name,
                                thumbnailUrl = o.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                                    ?: com.masterwave.player.data.recommendation.ArtistPhotoProvider.getPhotoUrlOrNull(name),
                                subscribers = o.optString("subscribers").takeIf { it.isNotBlank() },
                                description = o.optString("description").takeIf { it.isNotBlank() },
                                isFavorite = o.optBoolean("isFavorite", true),
                                followedAtMs = o.optLong("followedAtMs", System.currentTimeMillis())
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _followedArtists.value = list
                    return
                }
            }
        } catch (_: Exception) {}

        // Baseline initial seed for cold start (Fully editable and removable by the user!)
        val initialSeeds = listOf(
            "Arijit Singh",
            "Anirudh Ravichander",
            "Sai Abhyankkar",
            "Diljit Dosanjh",
            "A.R. Rahman",
            "The Weeknd",
            "Sid Sriram",
            "Shreya Ghoshal",
            "Dua Lipa",
            "Badshah"
        ).map { name ->
            Artist(
                id = name,
                name = name,
                thumbnailUrl = com.masterwave.player.data.recommendation.ArtistPhotoProvider.getPhotoUrlOrNull(name),
                isFavorite = true,
                followedAtMs = System.currentTimeMillis()
            )
        }
        _followedArtists.value = initialSeeds
        saveCachedFollowedArtists()
    }

    private fun saveCachedPlaylists() {
        try {
            val arr = JSONArray()
            _playlists.value.filter { it.id != "liked_songs" && it.id != "local_songs" }.forEach { pl ->
                val plObj = JSONObject().apply {
                    put("id", pl.id)
                    put("title", pl.title)
                    put("description", pl.description)
                    put("trackCount", pl.trackCount)
                    put("artworkUrl", pl.artworkUrl ?: "")
                    put("source", pl.source)
                    put("createdAt", pl.createdAt)

                    val tracksArr = JSONArray()
                    pl.tracks.forEach { t ->
                        tracksArr.put(
                            JSONObject().apply {
                                put("id", t.id)
                                put("title", t.title)
                                put("artist", t.artist)
                                put("album", t.album)
                                put("durationSeconds", t.durationSeconds)
                                put("artworkUrl", t.artworkUrl ?: "")
                            }
                        )
                    }
                    put("tracks", tracksArr)
                }
                arr.put(plObj)
            }
            cachePrefs.edit().putString("cached_playlists", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadCachedPlaylists() {
        try {
            val json = cachePrefs.getString("cached_playlists", null) ?: return
            val arr = JSONArray(json)
            val list = _playlists.value.toMutableList()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tracksArr = o.optJSONArray("tracks")
                val tracks = mutableListOf<Track>()
                if (tracksArr != null) {
                    for (j in 0 until tracksArr.length()) {
                        val to = tracksArr.getJSONObject(j)
                        tracks.add(
                            Track(
                                id = to.optString("id"),
                                title = to.optString("title"),
                                artist = to.optString("artist"),
                                album = to.optString("album"),
                                durationSeconds = to.optLong("durationSeconds"),
                                artworkUrl = to.optString("artworkUrl").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                val pl = Playlist(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    description = o.optString("description"),
                    trackCount = if (tracks.isNotEmpty()) tracks.size else o.optInt("trackCount"),
                    tracks = tracks,
                    artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() },
                    source = o.optString("source", "YouTube Music"),
                    isPinned = false,
                    createdAt = o.optString("createdAt")
                )
                val existing = list.indexOfFirst { it.id == pl.id }
                if (existing != -1) list[existing] = pl else list.add(pl)
            }
            _playlists.value = list
        } catch (_: Exception) {}
    }

    suspend fun search(query: String): SearchResult {
        val catalogTracks = catalogRepository.searchAll(query)
        val ytResult = try { ytProvider.search(query) } catch (_: Exception) { SearchResult(query = query) }
        val combinedSongs = catalogRepository.deduplicateTracks(catalogTracks + ytResult.songs)
        return SearchResult(
            query = query,
            songs = combinedSongs,
            artists = ytResult.artists,
            albums = ytResult.albums
        )
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        return ytProvider.getSearchSuggestions(query)
    }

    suspend fun getTrending(): List<Track> {
        val catalogTrending = catalogRepository.getTrendingAll()
        return if (catalogTrending.isNotEmpty()) {
            catalogTrending
        } else {
            ytProvider.getTrending()
        }
    }

    suspend fun getTrendingTracks(): List<Track> = getTrending()

    suspend fun getIndianMusic(category: String, limit: Int = 25): List<Track> {
        return catalogRepository.getIndianMusic(category, limit)
    }

    suspend fun resolveStreamUrl(track: Track): String {
        // 1. Direct local file
        if (track.isLocal && !track.localUri.isNullOrBlank()) {
            return track.localUri
        }
        if (track.id.startsWith("download_") || track.isLocal) {
            val candidate = track.localUri ?: track.streamUrl
            if (!candidate.isNullOrBlank()) {
                val cleanPath = candidate.removePrefix("file://")
                val f = java.io.File(cleanPath)
                if (f.exists()) return f.absolutePath
            }
        }
        // 2. Local and YouTube Music catalog resolver
        val resolved = catalogRepository.resolveStreamUrl(track)
        if (!resolved.isNullOrBlank()) {
            return resolved
        }

        // 3. Fallback for Spotify imported tracks
        if (track.id.startsWith("sp_")) {
            val searchRes = ytProvider.search("${track.title} ${track.artist}")
            val bestMatch = searchRes.songs.firstOrNull()
            if (bestMatch != null) {
                return ytProvider.resolveStreamUrl(bestMatch.id)
            }
        }

        if (track.providerId == "youtube") {
            return ytProvider.resolveStreamUrl(track.id.removePrefix("yt_"))
        }

        throw IllegalStateException("Unable to resolve a playable stream for ${track.title}.")
    }

    /**
     * Resolves stream URL for downloading with preferred container format (e.g., M4A for universal artwork/metadata support).
     */
    suspend fun resolveDownloadStream(track: Track): Pair<String, String> {
        val ytId = when {
            track.id.startsWith("yt_") -> track.id.removePrefix("yt_")
            track.providerId == "youtube" -> track.id
            track.id.startsWith("sp_") -> {
                val match = ytProvider.search("${track.title} ${track.artist}").songs.firstOrNull()
                match?.id
            }
            else -> null
        }

        if (ytId != null) {
            try {
                return ytProvider.resolveDownloadStream(ytId)
            } catch (_: Exception) {}
        }

        // Fallback to general stream URL resolver
        val resolved = resolveStreamUrl(track)
        val ext = if (track.codec.contains("AAC", ignoreCase = true) || resolved.endsWith(".m4a", ignoreCase = true)) {
            "m4a"
        } else if (resolved.endsWith(".mp3", ignoreCase = true)) {
            "mp3"
        } else {
            "m4a"
        }
        return Pair(resolved, ext)
    }

    suspend fun getLyrics(track: Track): LyricsResult? {
        return lyricsProvider.getLyrics(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MusicRepository? = null

        fun getInstance(context: Context): MusicRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
