package com.masterwave.player.data.cloud

import android.content.Context
import android.content.SharedPreferences
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UserProfile(
    val isSignedIn: Boolean = false,
    val displayName: String = "Guest",
    val email: String = "",
    val photoUrl: String? = null,
    val provider: String = "None"
)

data class CloudPlaylistPreset(
    val id: String,
    val title: String,
    val source: String,
    val query: String
)

class CloudAccountManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("masterwave_cloud_prefs", Context.MODE_PRIVATE)
    }

    private val httpClient = OkHttpClient()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            isSignedIn = prefs.getBoolean("is_signed_in", false),
            displayName = prefs.getString("display_name", "Guest") ?: "Guest",
            email = prefs.getString("email", "") ?: "",
            photoUrl = prefs.getString("photo_url", null),
            provider = prefs.getString("provider", "None") ?: "None"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _ytCookies = MutableStateFlow(prefs.getString("yt_cookies", null))
    val ytCookies: StateFlow<String?> = _ytCookies.asStateFlow()

    private val _spotifyCookies = MutableStateFlow(prefs.getString("sp_cookies", null))
    val spotifyCookies: StateFlow<String?> = _spotifyCookies.asStateFlow()

    val isYtAuthenticated: Boolean
        get() = !_ytCookies.value.isNullOrBlank()

    val isSpotifyAuthenticated: Boolean
        get() = !_spotifyCookies.value.isNullOrBlank()

    /**
     * Genuine YouTube Music authentication captured via official web sign-in.
     */
    fun saveYouTubeAuth(cookies: String) {
        _ytCookies.value = cookies
        val profile = UserProfile(
            isSignedIn = true,
            displayName = "YouTube Music Connected",
            email = "Authenticated via Google",
            photoUrl = null,
            provider = "YouTube Music"
        )
        _userProfile.value = profile
        prefs.edit()
            .putString("yt_cookies", cookies)
            .putBoolean("is_signed_in", true)
            .putString("display_name", profile.displayName)
            .putString("email", profile.email)
            .putString("provider", profile.provider)
            .apply()
    }

    fun signOutYouTube() {
        _ytCookies.value = null
        prefs.edit().remove("yt_cookies").apply()
        checkOverallAuth()
    }

    fun signInWithGoogle(displayName: String = "Google Account") {
        saveYouTubeAuth("active_session")
    }

    fun signOut() {
        signOutYouTube()
        signOutSpotify()
    }

    /**
     * Genuine Spotify authentication captured via official Spotify login.
     */
    fun saveSpotifyAuth(cookies: String) {
        _spotifyCookies.value = cookies
        prefs.edit().putString("sp_cookies", cookies).apply()
        checkOverallAuth()
    }

    fun signOutSpotify() {
        _spotifyCookies.value = null
        prefs.edit().remove("sp_cookies").apply()
        checkOverallAuth()
    }

    private fun checkOverallAuth() {
        if (_ytCookies.value.isNullOrBlank() && _spotifyCookies.value.isNullOrBlank()) {
            _userProfile.value = UserProfile(isSignedIn = false, displayName = "Guest", email = "", photoUrl = null, provider = "None")
            prefs.edit().putBoolean("is_signed_in", false).remove("display_name").remove("email").remove("provider").apply()
        }
    }

    val CuratedCloudPlaylists = listOf(
        CloudPlaylistPreset(
            id = "cloud_sp_top_hits",
            title = "Today's Top Hits",
            source = "Spotify Cloud",
            query = "Today's Top Hits popular songs"
        ),
        CloudPlaylistPreset(
            id = "cloud_sp_global_50",
            title = "Global Top 50 Hits",
            source = "Spotify Cloud",
            query = "Global Top 50 trending music"
        ),
        CloudPlaylistPreset(
            id = "cloud_yt_charts",
            title = "YouTube Music Top Charts",
            source = "YouTube Music",
            query = "YouTube Music Top 100 Charts"
        ),
        CloudPlaylistPreset(
            id = "cloud_yt_bollywood",
            title = "Bollywood Romance 2026",
            source = "YouTube Music",
            query = "Bollywood Romantic Hits 2026"
        ),
        CloudPlaylistPreset(
            id = "cloud_sp_lofi",
            title = "Chill Lofi Beats",
            source = "Spotify Cloud",
            query = "Lofi hip hop beats to study relax to"
        )
    )

    suspend fun syncCloudPlaylist(preset: CloudPlaylistPreset): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val searchResult = repository.search(preset.query)
            val songs = searchResult.songs.take(25)
            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

            val playlist = Playlist(
                id = preset.id + "_" + UUID.randomUUID().toString().take(6),
                title = preset.title,
                description = "Synced via ${preset.source}",
                trackCount = songs.size,
                tracks = songs,
                artworkUrl = songs.firstOrNull()?.artworkUrl,
                source = preset.source,
                isPinned = false,
                createdAt = today
            )

            repository.addCustomPlaylist(playlist)
            playlist
        }
    }

    suspend fun syncAllPresetPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val synced = mutableListOf<Playlist>()
        CuratedCloudPlaylists.forEach { preset ->
            syncCloudPlaylist(preset).getOrNull()?.let { synced.add(it) }
        }
        synced
    }

    companion object {
        @Volatile
        private var INSTANCE: CloudAccountManager? = null

        fun getInstance(context: Context, repository: MusicRepository): CloudAccountManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudAccountManager(context.applicationContext, repository).also { INSTANCE = it }
            }
        }
    }
}
