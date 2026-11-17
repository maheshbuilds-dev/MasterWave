package com.masterwave.player.data.repository

import android.content.Context
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.lyrics.LrclibLyricsProvider
import com.masterwave.player.data.model.LyricsResult
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.SearchResult
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.LocalMusicProvider
import com.masterwave.player.data.provider.PlaylistImporter
import com.masterwave.player.data.provider.YouTubeMusicProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MusicRepository(private val context: Context) {

    val okHttpClient = MasterWaveApp.instance.okHttpClient
    private val ytProvider = YouTubeMusicProvider(MasterWaveApp.instance.okHttpClient)
    private val localProvider = LocalMusicProvider(context)
    private val lyricsProvider = LrclibLyricsProvider(MasterWaveApp.instance.okHttpClient)
    private val playlistImporter = PlaylistImporter(MasterWaveApp.instance.okHttpClient)

    // Favorites
    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    val favorites: StateFlow<List<Track>> = _favorites.asStateFlow()

    // Recent History
    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

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
            id = "local_songs",
            title = "Local songs",
            description = "On-device audio tracks",
            trackCount = 0,
            source = "Local",
            isPinned = true,
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
        } else {
            current.add(0, track.copy(isFavorite = true))
        }
        _favorites.value = current
        updateLikedPlaylistCount(current.size)
        saveCachedFavorites()
    }

    fun addToFavorites(track: Track) {
        if (!isFavorite(track.id)) {
            val current = _favorites.value.toMutableList()
            current.add(0, track.copy(isFavorite = true))
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
        val list = _recentTracks.value.toMutableList()
        list.removeAll { it.id == track.id }
        list.add(0, track)
        _recentTracks.value = list.take(50)

        // Increment stats
        _totalPlays.value += 1
        val currentMap = _playCounts.value.toMutableMap()
        currentMap[track.id] = (currentMap[track.id] ?: 0) + 1
        _playCounts.value = currentMap
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

    fun addCustomPlaylist(playlist: Playlist) {
        val list = _playlists.value.toMutableList()
        val index = list.indexOfFirst { it.id == playlist.id }
        if (index != -1) {
            list[index] = playlist
        } else {
            list.add(playlist)
        }
        _playlists.value = list
        saveCachedPlaylists()
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
        return ytProvider.search(query)
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        return ytProvider.getSearchSuggestions(query)
    }

    suspend fun getTrending(): List<Track> {
        return ytProvider.getTrending()
    }

    suspend fun getTrendingTracks(): List<Track> = getTrending()

    suspend fun resolveStreamUrl(track: Track): String {
        if (track.isLocal && !track.localUri.isNullOrBlank()) {
            return track.localUri
        }
        if (track.id.startsWith("download_") || track.isLocal) {
            val candidate = track.localUri ?: track.streamUrl
            if (!candidate.isNullOrBlank()) {
                val cleanPath = candidate.removePrefix("file://")
                val f = java.io.File(cleanPath)
                if (f.exists()) {
                    return f.absolutePath
                }
            }
        }
        // If track streamUrl is directly usable
        if (!track.streamUrl.isNullOrBlank()) {
            if (track.streamUrl.startsWith("http://") || track.streamUrl.startsWith("https://")) {
                return track.streamUrl
            }
            val f = java.io.File(track.streamUrl.removePrefix("file://"))
            if (f.exists()) {
                return f.absolutePath
            }
        }
        // If track originated from Spotify import, search YouTube Music by "Title Artist"
        if (track.id.startsWith("sp_")) {
            val searchRes = ytProvider.search("${track.title} ${track.artist}")
            val bestMatch = searchRes.songs.firstOrNull()
            if (bestMatch != null) {
                return ytProvider.resolveStreamUrl(bestMatch.id)
            }
        }
        return ytProvider.resolveStreamUrl(track.id)
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
