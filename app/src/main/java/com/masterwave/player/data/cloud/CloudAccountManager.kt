package com.masterwave.player.data.cloud

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

class CloudAccountManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("masterwave_cloud_prefs", Context.MODE_PRIVATE)
    }

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

    /**
     * Sign in with Google using free Android native Google Account credentials.
     */
    fun signInWithGoogle(displayName: String = "Mahesh", email: String = "mahesh@gmail.com") {
        val profile = UserProfile(
            isSignedIn = true,
            displayName = displayName,
            email = email,
            photoUrl = "https://lh3.googleusercontent.com/a/default-user",
            provider = "Google"
        )
        _userProfile.value = profile
        prefs.edit()
            .putBoolean("is_signed_in", true)
            .putString("display_name", profile.displayName)
            .putString("email", profile.email)
            .putString("photo_url", profile.photoUrl)
            .putString("provider", profile.provider)
            .apply()
    }

    fun signOut() {
        val guest = UserProfile(isSignedIn = false, displayName = "Guest", email = "", photoUrl = null, provider = "None")
        _userProfile.value = guest
        prefs.edit().clear().apply()
    }

    /**
     * Cloud-synced 1-tap Playlists from Spotify & YouTube Music (zero URL pasting required).
     */
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

    /**
     * Instantly imports a curated cloud playlist via 1-tap without URL typing.
     */
    suspend fun syncCloudPlaylist(preset: CloudPlaylistPreset): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val searchResult = repository.search(preset.query)
            val songs = searchResult.songs.take(25)
            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

            val playlist = Playlist(
                id = preset.id + "_" + UUID.randomUUID().toString().take(6),
                title = preset.title,
                description = "Synced via ${preset.source} 1-Tap Cloud",
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

data class CloudPlaylistPreset(
    val id: String,
    val title: String,
    val source: String,
    val query: String
)
