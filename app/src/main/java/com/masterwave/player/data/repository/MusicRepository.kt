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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MusicRepository(private val context: Context) {

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
    private val _playlists = MutableStateFlow<List<Playlist>>(
        listOf(
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
    )
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

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
    }

    fun isFavorite(trackId: String): Boolean {
        return _favorites.value.any { it.id == trackId }
    }

    private fun updateLikedPlaylistCount(count: Int) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == "liked_songs") {
                pl.copy(trackCount = count)
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
            _playlists.value = _playlists.value + newPlaylist
        }
        return result
    }

    suspend fun importSpotifyPlaylist(urlOrId: String): Result<Playlist> {
        val result = playlistImporter.importSpotifyPlaylist(urlOrId)
        result.onSuccess { newPlaylist ->
            _playlists.value = _playlists.value + newPlaylist
        }
        return result
    }

    fun addCustomPlaylist(playlist: Playlist) {
        _playlists.value = _playlists.value + playlist
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
        _playlists.value = _playlists.value + newPl
        return newPl
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

    suspend fun resolveStreamUrl(track: Track): String {
        if (track.isLocal && !track.localUri.isNullOrBlank()) {
            return track.localUri
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
