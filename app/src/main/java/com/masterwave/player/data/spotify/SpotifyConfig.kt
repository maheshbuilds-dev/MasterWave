package com.masterwave.player.data.spotify

import android.content.Context
import com.masterwave.player.MasterWaveApp

/**
 * Official Spotify Web API Configuration (Single Source of Truth).
 * Uses strictly official endpoints and Authorization Code Flow with PKCE.
 * No client secrets, no private APIs.
 */
object SpotifyConfig {
    // Default placeholder Client ID. The user can also enter their Client ID directly
    // in the app UI (Settings -> Cloud & Integrations -> Spotify) which is persisted securely.
    const val DEFAULT_CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID"
    private const val PREFS_NAME = "masterwave_spotify_config"
    private const val KEY_CUSTOM_CLIENT_ID = "custom_spotify_client_id"

    const val REDIRECT_URI = "masterwave://spotify-callback"
    const val AUTH_URL = "https://accounts.spotify.com/authorize"
    const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    const val API_BASE_URL = "https://api.spotify.com/v1"

    // Only read-only scopes as mandated by safety constraints
    val SCOPES = listOf(
        "user-library-read",
        "playlist-read-private",
        "playlist-read-collaborative"
    ).joinToString(" ")

    fun getClientId(): String {
        val prefs = MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_CUSTOM_CLIENT_ID, null)?.trim()
        return if (!custom.isNullOrBlank()) custom else DEFAULT_CLIENT_ID
    }

    fun setClientId(clientId: String) {
        val prefs = MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_CLIENT_ID, clientId.trim()).apply()
    }

    fun isConfigured(): Boolean {
        val id = getClientId()
        return id.isNotBlank() && id != DEFAULT_CLIENT_ID
    }
}
