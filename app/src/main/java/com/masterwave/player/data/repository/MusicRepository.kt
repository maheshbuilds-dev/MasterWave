package com.masterwave.player.data.repository

import android.content.Context
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.lyrics.LrclibLyricsProvider
import com.masterwave.player.data.model.LyricsResult
import com.masterwave.player.data.model.SearchResult
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.LocalMusicProvider
import com.masterwave.player.data.provider.YouTubeMusicProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class MusicRepository(private val context: Context) {

    private val ytProvider = YouTubeMusicProvider(MasterWaveApp.instance.okHttpClient)
    private val localProvider = LocalMusicProvider(context)
    private val lyricsProvider = LrclibLyricsProvider(MasterWaveApp.instance.okHttpClient)

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    fun toggleFavorite(trackId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        _favorites.value = current
    }

    fun isFavorite(trackId: String): Boolean {
        return _favorites.value.contains(trackId)
    }

    fun recordRecentTrack(track: Track) {
        val list = _recentTracks.value.toMutableList()
        list.removeAll { it.id == track.id }
        list.add(0, track)
        if (list.size > 50) {
            _recentTracks.value = list.take(50)
        } else {
            _recentTracks.value = list
        }
    }

    suspend fun loadLocalTracks(): List<Track> {
        val tracks = localProvider.loadLocalTracks()
        _localTracks.value = tracks
        return tracks
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
