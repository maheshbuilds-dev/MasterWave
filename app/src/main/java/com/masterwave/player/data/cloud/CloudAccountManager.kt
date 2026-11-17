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
import com.masterwave.player.MasterWaveApp
import kotlinx.coroutines.launch
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

    private val ytFetcher by lazy {
        YouTubeLibraryFetcher(MasterWaveApp.instance.okHttpClient, repository)
    }

    init {
        // Auto-sync library in background if already signed in
        val savedCookies = _ytCookies.value
        if (!savedCookies.isNullOrBlank()) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    ytFetcher.fetchRealUserPlaylists(savedCookies)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Genuine YouTube Music authentication captured via official web sign-in.
     */
    fun saveYouTubeAuth(cookies: String) {
        _ytCookies.value = cookies
        prefs.edit().putString("yt_cookies", cookies).apply()

        // Fetch real account profile and sync library asynchronously
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val (name, avatar) = ytFetcher.fetchUserProfile(cookies)
                val profile = UserProfile(
                    isSignedIn = true,
                    displayName = name,
                    email = "Google Account Synchronized",
                    photoUrl = avatar,
                    provider = "YouTube Music"
                )
                _userProfile.value = profile
                prefs.edit()
                    .putBoolean("is_signed_in", true)
                    .putString("display_name", profile.displayName)
                    .putString("email", profile.email)
                    .putString("photo_url", avatar)
                    .putString("provider", profile.provider)
                    .apply()

                // Immediately fetch Real Liked Songs & User Playlists
                ytFetcher.fetchRealUserPlaylists(cookies)
            } catch (e: Exception) {
                android.util.Log.e("CloudAccountManager", "Error during YouTube Music sync", e)
            }
        }
    }

    suspend fun syncRealYouTubeMusicLibrary(): List<Playlist> = withContext(Dispatchers.IO) {
        val cookies = _ytCookies.value
        if (cookies.isNullOrBlank()) {
            return@withContext emptyList()
        }
        ytFetcher.fetchRealUserPlaylists(cookies)
    }

    suspend fun importYouTubePlaylistUrl(urlOrId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        val importer = com.masterwave.player.data.provider.PlaylistImporter(MasterWaveApp.instance.okHttpClient)
        val result = importer.importYouTubePlaylist(urlOrId)
        result.onSuccess { pl ->
            repository.addCustomPlaylist(pl)
        }
        result
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

    suspend fun syncAllPresetPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        syncRealYouTubeMusicLibrary()
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
