package com.masterwave.player.data.cloud

import android.util.Log
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.PlaylistImporter
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Authentic, Production-Grade Spotify Library & Liked Songs Sync Engine:
 * - Uses captured Spotify session cookies (sp_dc / sp_key).
 * - Computes dynamic TOTP tokens against Spotify's Web Player token endpoint.
 * - Queries official Spotify Web API endpoints:
 *   - "https://api.spotify.com/v1/me" (User profile & avatar)
 *   - "https://api.spotify.com/v1/me/tracks" (User's Liked Songs)
 *   - "https://api.spotify.com/v1/me/playlists" (User's library playlists)
 *   - "https://api.spotify.com/v1/playlists/{id}/tracks" (Tracks for each playlist)
 * - Directly parses tracks with titles, artists, albums, durations, and high-resolution album artwork.
 * - Imports tracks into favorites and custom playlists in MusicRepository.
 * - Includes robust fallback to public Embed scraping for unauthenticated playlist links.
 */
class SpotifyLibraryFetcher(
    private val okHttpClient: OkHttpClient,
    private val repository: MusicRepository
) {
    private val playlistImporter = PlaylistImporter(okHttpClient)

    companion object {
        private const val TAG = "SpotifyLibraryFetcher"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val SECRETS_DICT_URL = "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/refs/heads/main/secrets/secretDict.json"
        private const val SESSION_TOKEN_URL = "https://open.spotify.com/api/token"
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val SPOTIFY_APP_VERSION = "1.2.40.520.g08960714"
        private const val SPOTIFY_ORIGIN = "https://open.spotify.com"

        // Embedded offline fallback secret in case raw.githubusercontent is unreachable
        private val FALLBACK_SECRET_CIPHER = listOf(
            44, 55, 47, 42, 70, 40, 34, 114, 76, 74, 50, 111, 120, 97, 75, 76, 94, 102, 43, 69, 49, 120, 118, 80, 64, 78
        )
        private const val FALLBACK_VERSION = 61
    }

    fun extractSpdc(cookies: String): String? {
        val regex = Regex("(?:sp_dc)=([^;\\s]+)")
        return regex.find(cookies)?.groupValues?.get(1)?.trim()
    }

    private fun generateOtp(secretKeyBytes: ByteArray, timestampSeconds: Long): String {
        val counter = timestampSeconds / 30L
        val counterBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array()

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secretKeyBytes, "HmacSHA1"))
        val hmacResult = mac.doFinal(counterBytes)

        val offset = hmacResult[hmacResult.size - 1].toInt() and 0x0F
        val binary = ((hmacResult[offset].toInt() and 0x7F) shl 24) or
            ((hmacResult[offset + 1].toInt() and 0xFF) shl 16) or
            ((hmacResult[offset + 2].toInt() and 0xFF) shl 8) or
            (hmacResult[offset + 3].toInt() and 0xFF)

        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private suspend fun fetchSecret(): Pair<ByteArray, Int> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(SECRETS_DICT_URL).header("User-Agent", USER_AGENT).build()
            val res = okHttpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val jsonStr = res.body?.string() ?: ""
                val obj = JSONObject(jsonStr)
                val keys = obj.keys().asSequence().mapNotNull { it.toIntOrNull() }.sortedDescending().toList()
                if (keys.isNotEmpty()) {
                    val latestVersion = keys.first()
                    val arr = obj.getJSONArray(latestVersion.toString())
                    val cipherBytes = (0 until arr.length()).map { arr.getInt(it) }
                    val secretString = cipherBytes.map { it.toChar() }.joinToString("")
                    val plainBytes = secretString.map { it.code }
                    val secretKeyString = buildString {
                        for ((index, byteVal) in plainBytes.withIndex()) {
                            val decoded = byteVal xor ((index % 33) + 9)
                            append(decoded.toString())
                        }
                    }
                    return@withContext Pair(secretKeyString.toByteArray(Charsets.US_ASCII), latestVersion)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch online secret, using fallback", e)
        }

        // Fallback
        val secretString = FALLBACK_SECRET_CIPHER.map { it.toChar() }.joinToString("")
        val plainBytes = secretString.map { it.code }
        val secretKeyString = buildString {
            for ((index, byteVal) in plainBytes.withIndex()) {
                val decoded = byteVal xor ((index % 33) + 9)
                append(decoded.toString())
            }
        }
        Pair(secretKeyString.toByteArray(Charsets.US_ASCII), FALLBACK_VERSION)
    }

    suspend fun getAccessToken(cookies: String?): String? = withContext(Dispatchers.IO) {
        val spdc = cookies?.let { extractSpdc(it) }
        val (secretKeyBytes, version) = fetchSecret()

        val serverTimeSeconds: Long = try {
            val req = Request.Builder().url(SERVER_TIME_URL).header("User-Agent", USER_AGENT).build()
            val res = okHttpClient.newCall(req).execute()
            if (res.isSuccessful) {
                JSONObject(res.body?.string() ?: "").optLong("serverTime", System.currentTimeMillis() / 1000L)
            } else {
                System.currentTimeMillis() / 1000L
            }
        } catch (_: Exception) {
            System.currentTimeMillis() / 1000L
        }

        val clientTimeSeconds = System.currentTimeMillis() / 1000L
        val otpClient = generateOtp(secretKeyBytes, clientTimeSeconds)
        val otpServer = generateOtp(secretKeyBytes, serverTimeSeconds)

        val tokenUrl = "$SESSION_TOKEN_URL?reason=transport&productType=web-player&totp=$otpClient&totpServer=$otpServer&totpVer=$version"
        val reqBuilder = Request.Builder()
            .url(tokenUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("App-platform", "WebPlayer")
            .header("Spotify-App-Version", SPOTIFY_APP_VERSION)
            .header("Origin", SPOTIFY_ORIGIN)
            .header("Referer", "$SPOTIFY_ORIGIN/")

        if (!spdc.isNullOrBlank()) {
            reqBuilder.header("Cookie", "sp_dc=$spdc")
        }

        try {
            val res = okHttpClient.newCall(reqBuilder.build()).execute()
            if (res.isSuccessful) {
                val json = JSONObject(res.body?.string() ?: "")
                val token = json.optString("accessToken")
                if (token.isNotBlank()) return@withContext token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring Spotify token", e)
        }
        null
    }

    suspend fun fetchUserProfile(cookies: String): Pair<String, String?> = withContext(Dispatchers.IO) {
        val token = getAccessToken(cookies)
        if (!token.isNullOrBlank()) {
            try {
                val req = Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", USER_AGENT)
                    .build()
                val res = okHttpClient.newCall(req).execute()
                if (res.isSuccessful) {
                    val root = JSONObject(res.body?.string() ?: "")
                    val name = root.optString("display_name", "Spotify User").ifBlank { "Spotify User" }
                    val avatar = root.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                    return@withContext Pair(name, avatar)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Spotify user profile", e)
            }
        }
        Pair("Spotify Account", null)
    }

    suspend fun fetchRealLikedSongs(cookies: String): List<Track> = withContext(Dispatchers.IO) {
        val token = getAccessToken(cookies) ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()

        var offset = 0
        val limit = 50
        val maxTracksToFetch = 150

        while (offset < maxTracksToFetch) {
            try {
                val req = Request.Builder()
                    .url("https://api.spotify.com/v1/me/tracks?limit=$limit&offset=$offset")
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", USER_AGENT)
                    .build()
                val res = okHttpClient.newCall(req).execute()
                if (!res.isSuccessful) break
                val root = JSONObject(res.body?.string() ?: "")
                val items = root.optJSONArray("items") ?: break
                if (items.length() == 0) break

                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val trackObj = item.optJSONObject("track") ?: continue
                    val id = trackObj.optString("id")
                    val name = trackObj.optString("name")
                    if (id.isBlank() || name.isBlank()) continue

                    val artistsArr = trackObj.optJSONArray("artists")
                    val artistNames = mutableListOf<String>()
                    if (artistsArr != null) {
                        for (a in 0 until artistsArr.length()) {
                            val aObj = artistsArr.optJSONObject(a)
                            val aName = aObj?.optString("name")
                            if (!aName.isNullOrBlank()) artistNames.add(aName)
                        }
                    }
                    val artist = if (artistNames.isNotEmpty()) artistNames.joinToString(", ") else "Unknown Artist"

                    val albumObj = trackObj.optJSONObject("album")
                    val albumName = albumObj?.optString("name") ?: "Spotify"
                    val artwork = albumObj?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                    val durationMs = trackObj.optLong("duration_ms", 180000L)

                    tracks.add(
                        Track(
                            id = "sp_$id",
                            title = name,
                            artist = artist,
                            album = albumName,
                            durationSeconds = durationMs / 1000L,
                            artworkUrl = artwork,
                            streamUrl = null,
                            codec = "Opus",
                            bitrateKbps = 160,
                            isFavorite = true
                        )
                    )
                }

                if (items.length() < limit) break
                offset += limit
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching liked songs at offset $offset", e)
                break
            }
        }

        tracks.distinctBy { it.id }
    }

    suspend fun fetchRealUserPlaylists(cookies: String): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken(cookies) ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()
        val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists?limit=25")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", USER_AGENT)
                .build()
            val res = okHttpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val root = JSONObject(res.body?.string() ?: "")
                val items = root.optJSONArray("items")
                if (items != null) {
                    for (p in 0 until items.length()) {
                        val plObj = items.optJSONObject(p) ?: continue
                        val plId = plObj.optString("id")
                        val plTitle = plObj.optString("name")
                        val plDesc = plObj.optString("description")
                        val plArtwork = plObj.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                        if (plId.isBlank() || plTitle.isBlank()) continue

                        val tracks = fetchPlaylistTracks(plId, token)
                        playlists.add(
                            Playlist(
                                id = "spotify_$plId",
                                title = plTitle,
                                description = plDesc.ifBlank { "Synced from Spotify" },
                                trackCount = if (tracks.isNotEmpty()) tracks.size else plObj.optJSONObject("tracks")?.optInt("total", 0) ?: 0,
                                tracks = tracks,
                                artworkUrl = plArtwork ?: tracks.firstOrNull()?.artworkUrl,
                                source = "Spotify",
                                isPinned = false,
                                createdAt = today
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user playlists", e)
        }

        playlists
    }

    private suspend fun fetchPlaylistTracks(playlistId: String, token: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=100")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", USER_AGENT)
                .build()
            val res = okHttpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val root = JSONObject(res.body?.string() ?: "")
                val items = root.optJSONArray("items") ?: return@withContext emptyList()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val trackObj = item.optJSONObject("track") ?: continue
                    val id = trackObj.optString("id")
                    val name = trackObj.optString("name")
                    if (id.isBlank() || name.isBlank()) continue

                    val artistsArr = trackObj.optJSONArray("artists")
                    val artistNames = mutableListOf<String>()
                    if (artistsArr != null) {
                        for (a in 0 until artistsArr.length()) {
                            val aObj = artistsArr.optJSONObject(a)
                            val aName = aObj?.optString("name")
                            if (!aName.isNullOrBlank()) artistNames.add(aName)
                        }
                    }
                    val artist = if (artistNames.isNotEmpty()) artistNames.joinToString(", ") else "Unknown Artist"
                    val albumObj = trackObj.optJSONObject("album")
                    val albumName = albumObj?.optString("name") ?: "Spotify"
                    val artwork = albumObj?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                    val durationMs = trackObj.optLong("duration_ms", 180000L)

                    tracks.add(
                        Track(
                            id = "sp_$id",
                            title = name,
                            artist = artist,
                            album = albumName,
                            durationSeconds = durationMs / 1000L,
                            artworkUrl = artwork,
                            streamUrl = null,
                            codec = "Opus",
                            bitrateKbps = 160
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tracks for playlist $playlistId", e)
        }

        // Fallback to public embed scraper if token fetch returned empty
        if (tracks.isEmpty()) {
            try {
                val res = playlistImporter.importSpotifyPlaylist(playlistId)
                if (res.isSuccess) {
                    return@withContext res.getOrThrow().tracks
                }
            } catch (_: Exception) {}
        }

        tracks
    }

    suspend fun fetchRealUserLibrary(cookies: String): List<Playlist> = withContext(Dispatchers.IO) {
        val importedPlaylists = mutableListOf<Playlist>()
        val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

        // 1. Fetch Real Liked Songs
        val likedTracks = fetchRealLikedSongs(cookies)
        if (likedTracks.isNotEmpty()) {
            repository.addAllToFavorites(likedTracks)
            val likedPlaylist = Playlist(
                id = "spotify_liked_songs",
                title = "Liked Songs (Spotify)",
                description = "Your favorite tracks synchronized from Spotify",
                trackCount = likedTracks.size,
                tracks = likedTracks,
                artworkUrl = likedTracks.firstOrNull()?.artworkUrl,
                source = "Spotify",
                isPinned = true,
                createdAt = today
            )
            importedPlaylists.add(likedPlaylist)
            repository.addCustomPlaylist(likedPlaylist)
        }

        // 2. Fetch User Playlists
        val userPlaylists = fetchRealUserPlaylists(cookies)
        for (pl in userPlaylists) {
            importedPlaylists.add(pl)
            repository.addCustomPlaylist(pl)
        }

        importedPlaylists
    }
}
