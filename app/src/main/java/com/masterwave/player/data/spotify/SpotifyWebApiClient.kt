package com.masterwave.player.data.spotify

import android.util.Log
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SpotifyPlaylistSummary(
    val id: String,
    val name: String,
    val description: String,
    val trackCount: Int,
    val artworkUrl: String?,
    val ownerName: String?
)

enum class SpotifyErrorType {
    NO_INTERNET,
    UNAUTHORIZED,
    DEV_MODE_RESTRICTION_403,
    EMPTY_PLAYLIST,
    ITEMS_UNAVAILABLE,
    RATE_LIMITED,
    GENERIC
}

sealed class SpotifyResult<out T> {
    data class Success<T>(val data: T) : SpotifyResult<T>()
    data class Error(val type: SpotifyErrorType, val message: String) : SpotifyResult<Nothing>()
}

data class SpotifyImportProgress(
    val currentStep: String,
    val currentCount: Int,
    val totalCount: Int
)

/**
 * Official Spotify Web API Client (Updated for February 2026 Guidelines):
 * - Uses strictly official Web API endpoints with Bearer Token authentication.
 * - Endpoints:
 *   - /v1/me/playlists (Current user's playlists)
 *   - /v1/playlists/{id}/items (Official February 2026 playlist contents endpoint)
 *   - /v1/me/tracks (User's Liked Songs)
 * - Automatic pagination via limit & offset until next == null.
 * - HTTP 429 Rate Limiting with dynamic Retry-After backoff.
 * - Strict metadata-only parsing (Title, Artist, Album, Duration, Artwork, Spotify Track URL).
 */
class SpotifyWebApiClient(
    private val authManager: SpotifyAuthManager,
    private val httpClient: OkHttpClient = MasterWaveApp.instance.okHttpClient
) {
    companion object {
        private const val TAG = "SpotifyWebApiClient"
        private const val PAGE_SIZE = 50
    }

    /**
     * Fetches all playlists in the user's Spotify library.
     */
    suspend fun fetchUserPlaylists(): SpotifyResult<List<SpotifyPlaylistSummary>> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<SpotifyPlaylistSummary>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (offset < total) {
            val url = "${SpotifyConfig.API_BASE_URL}/me/playlists?limit=$PAGE_SIZE&offset=$offset"
            when (val res = executeGet(url)) {
                is RawApiResponse.Success -> {
                    try {
                        val json = JSONObject(res.body)
                        total = json.optInt("total", 0)
                        val items = json.optJSONArray("items") ?: break
                        if (items.length() == 0) break

                        for (i in 0 until items.length()) {
                            val plObj = items.optJSONObject(i) ?: continue
                            val id = plObj.optString("id")
                            val name = plObj.optString("name")
                            val desc = plObj.optString("description")
                            val count = plObj.optJSONObject("tracks")?.optInt("total", 0) ?: 0
                            val images = plObj.optJSONArray("images")
                            val artwork = if (images != null && images.length() > 0) {
                                images.optJSONObject(0)?.optString("url")
                            } else null
                            val owner = plObj.optJSONObject("owner")?.optString("display_name")

                            if (id.isNotBlank() && name.isNotBlank()) {
                                playlists.add(
                                    SpotifyPlaylistSummary(
                                        id = id,
                                        name = name,
                                        description = desc,
                                        trackCount = count,
                                        artworkUrl = artwork,
                                        ownerName = owner
                                    )
                                )
                            }
                        }

                        val next = json.optString("next", "null")
                        if (next == "null" || next.isBlank()) break
                        offset += PAGE_SIZE
                    } catch (e: Exception) {
                        return@withContext SpotifyResult.Error(SpotifyErrorType.GENERIC, "Failed parsing playlists: ${e.message}")
                    }
                }
                is RawApiResponse.Error -> {
                    return@withContext SpotifyResult.Error(res.type, res.message)
                }
            }
        }

        SpotifyResult.Success(playlists)
    }

    /**
     * Fetches tracks for a playlist using the official February 2026 /playlists/{id}/items endpoint.
     */
    suspend fun fetchPlaylistTracks(
        playlistId: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): SpotifyResult<List<Track>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (offset < total) {
            // Official February 2026 endpoint: /v1/playlists/{playlist_id}/items
            val url = "${SpotifyConfig.API_BASE_URL}/playlists/$playlistId/items?limit=$PAGE_SIZE&offset=$offset"
            when (val res = executeGet(url)) {
                is RawApiResponse.Success -> {
                    try {
                        val json = JSONObject(res.body)
                        total = json.optInt("total", 0)
                        val items = json.optJSONArray("items") ?: break
                        if (items.length() == 0) break

                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            // Support both track and item response shapes
                            val trackObj = item.optJSONObject("track") ?: item.optJSONObject("item") ?: continue
                            val track = parseTrackObject(trackObj)
                            if (track != null) {
                                tracks.add(track)
                            }
                        }

                        onProgress?.invoke(tracks.size, total)

                        val next = json.optString("next", "null")
                        if (next == "null" || next.isBlank()) break
                        offset += PAGE_SIZE
                    } catch (e: Exception) {
                        return@withContext SpotifyResult.Error(SpotifyErrorType.GENERIC, "Error parsing playlist items: ${e.message}")
                    }
                }
                is RawApiResponse.Error -> {
                    if (res.type == SpotifyErrorType.DEV_MODE_RESTRICTION_403) {
                        return@withContext SpotifyResult.Error(
                            SpotifyErrorType.DEV_MODE_RESTRICTION_403,
                            "Spotify Development Mode restriction (HTTP 403): You can only import playlists you own or collaborate on, and your account must have Spotify Premium and be registered as an Authorized User."
                        )
                    }
                    return@withContext SpotifyResult.Error(res.type, res.message)
                }
            }
        }

        if (tracks.isEmpty()) {
            return@withContext SpotifyResult.Error(SpotifyErrorType.EMPTY_PLAYLIST, "This playlist has no available tracks.")
        }

        SpotifyResult.Success(tracks)
    }

    /**
     * Fetches all user Liked Songs from /v1/me/tracks.
     */
    suspend fun fetchLikedSongs(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): SpotifyResult<List<Track>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (offset < total) {
            val url = "${SpotifyConfig.API_BASE_URL}/me/tracks?limit=$PAGE_SIZE&offset=$offset"
            when (val res = executeGet(url)) {
                is RawApiResponse.Success -> {
                    try {
                        val json = JSONObject(res.body)
                        total = json.optInt("total", 0)
                        val items = json.optJSONArray("items") ?: break
                        if (items.length() == 0) break

                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val trackObj = item.optJSONObject("track") ?: continue
                            val track = parseTrackObject(trackObj, isFavorite = true)
                            if (track != null) {
                                tracks.add(track)
                            }
                        }

                        onProgress?.invoke(tracks.size, total)

                        val next = json.optString("next", "null")
                        if (next == "null" || next.isBlank()) break
                        offset += PAGE_SIZE
                    } catch (e: Exception) {
                        return@withContext SpotifyResult.Error(SpotifyErrorType.GENERIC, "Error parsing Liked Songs: ${e.message}")
                    }
                }
                is RawApiResponse.Error -> {
                    return@withContext SpotifyResult.Error(res.type, res.message)
                }
            }
        }

        SpotifyResult.Success(tracks)
    }

    /**
     * Converts a raw Spotify track JSON object into MasterWave's read-only metadata Track model.
     */
    private fun parseTrackObject(trackObj: JSONObject, isFavorite: Boolean = false): Track? {
        val id = trackObj.optString("id")
        val title = trackObj.optString("name")
        if (id.isBlank() || title.isBlank()) return null

        val artistsArr = trackObj.optJSONArray("artists")
        val artistNames = mutableListOf<String>()
        if (artistsArr != null) {
            for (i in 0 until artistsArr.length()) {
                val a = artistsArr.optJSONObject(i)
                val aName = a?.optString("name")
                if (!aName.isNullOrBlank()) artistNames.add(aName)
            }
        }
        val artist = if (artistNames.isNotEmpty()) artistNames.joinToString(", ") else "Unknown Artist"

        val albumObj = trackObj.optJSONObject("album")
        val albumName = albumObj?.optString("name", "Spotify") ?: "Spotify"
        val imagesArr = albumObj?.optJSONArray("images")
        val artworkUrl = if (imagesArr != null && imagesArr.length() > 0) {
            imagesArr.optJSONObject(0)?.optString("url")
        } else null

        val durationMs = trackObj.optLong("duration_ms", 0L)
        val externalUrls = trackObj.optJSONObject("external_urls")
        val spotifyUrl = externalUrls?.optString("spotify", "https://open.spotify.com/track/$id")
            ?: "https://open.spotify.com/track/$id"

        return Track(
            id = "sp_$id",
            title = title,
            artist = artist,
            album = albumName,
            durationSeconds = durationMs / 1000L,
            artworkUrl = artworkUrl,
            streamUrl = null,
            isLocal = false,
            localUri = null,
            codec = "Spotify Metadata",
            bitrateKbps = 320,
            isFavorite = isFavorite,
            providerId = "spotify",
            externalSourceUrl = spotifyUrl,
            availability = TrackAvailability.METADATA_ONLY
        )
    }

    // -------------------------------------------------------------
    // HTTP Execution with 429 Rate Limiting & Error Categorization
    // -------------------------------------------------------------

    private sealed class RawApiResponse {
        data class Success(val body: String) : RawApiResponse()
        data class Error(val type: SpotifyErrorType, val message: String) : RawApiResponse()
    }

    private suspend fun executeGet(url: String, retryCount: Int = 0): RawApiResponse {
        val token = authManager.getValidAccessToken()
            ?: return RawApiResponse.Error(SpotifyErrorType.UNAUTHORIZED, "Spotify session expired. Please connect Spotify again.")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val code = response.code

            when (code) {
                in 200..299 -> {
                    val body = response.body?.string() ?: ""
                    RawApiResponse.Success(body)
                }
                429 -> {
                    // Rate Limit Exceeded: Check Retry-After header in seconds
                    val retryAfterStr = response.header("Retry-After")
                    val retrySeconds = retryAfterStr?.toLongOrNull() ?: 2L
                    Log.w(TAG, "HTTP 429 Rate limited. Backing off for $retrySeconds seconds.")
                    if (retryCount < 3) {
                        delay((retrySeconds + 1) * 1000L)
                        executeGet(url, retryCount + 1)
                    } else {
                        RawApiResponse.Error(SpotifyErrorType.RATE_LIMITED, "Spotify rate limit reached. Please wait $retrySeconds seconds and try again.")
                    }
                }
                401 -> {
                    RawApiResponse.Error(SpotifyErrorType.UNAUTHORIZED, "Authentication token expired. Please re-authenticate.")
                }
                403 -> {
                    RawApiResponse.Error(
                        SpotifyErrorType.DEV_MODE_RESTRICTION_403,
                        "Spotify Development Mode restriction (HTTP 403): Ensure your Spotify account is registered in your Spotify Developer App Dashboard and has Spotify Premium."
                    )
                }
                404 -> {
                    RawApiResponse.Error(SpotifyErrorType.ITEMS_UNAVAILABLE, "Requested Spotify playlist or items not found.")
                }
                else -> {
                    RawApiResponse.Error(SpotifyErrorType.GENERIC, "Spotify API returned HTTP $code.")
                }
            }
        } catch (e: IOException) {
            RawApiResponse.Error(SpotifyErrorType.NO_INTERNET, "Cannot reach Spotify: No internet connection.")
        } catch (e: Exception) {
            RawApiResponse.Error(SpotifyErrorType.GENERIC, "Unexpected error: ${e.message}")
        }
    }
}
