package com.masterwave.player.data.cloud

import android.util.Log
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.PlaylistImporter
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Authentic, Production-Grade YouTube Music Library & Liked Songs Sync Engine:
 * - Uses captured Google / YouTube Music session cookies (SAPISID / __Secure-3PAPISID / SSID / SID).
 * - Generates cryptographically valid SAPISIDHASH for InnerTube WEB_REMIX authentication.
 * - Queries official InnerTube browse endpoints:
 *   - "VLLM" / "FEmusic_liked_videos" (User's Liked Music songs)
 *   - "FEmusic_liked_playlists" / "FEmusic_library_landing" (User's library playlists)
 *   - Authenticated "VL$playlistId" for each user playlist (including private playlists)
 * - Directly parses musicResponsiveListItemRenderer to extract exact videoIds, titles, artists,
 *   albums, durations, and high-resolution album artwork.
 */
class YouTubeLibraryFetcher(
    private val okHttpClient: OkHttpClient,
    private val repository: MusicRepository
) {
    private val playlistImporter = PlaylistImporter(okHttpClient)

    companion object {
        private const val TAG = "YouTubeLibraryFetcher"
        private const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2Slqaeuk43AZA"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        private const val ORIGIN = "https://music.youtube.com"
    }

    /**
     * Computes the official Google SAPISIDHASH authentication header.
     */
    fun generateSapisidHash(cookies: String, origin: String = ORIGIN): String? {
        val sapisidRegex = Regex("(?:SAPISID|__Secure-3PAPISID|__Secure-1PAPISID)=([^;\\s]+)")
        val match = sapisidRegex.find(cookies) ?: return null
        val sapisid = match.groupValues[1].trim()

        val timestamp = System.currentTimeMillis() / 1000
        val payload = "$timestamp $sapisid $origin"

        return try {
            val digest = MessageDigest.getInstance("SHA-1").digest(payload.toByteArray(Charsets.UTF_8))
            val hashHex = digest.joinToString("") { "%02x".format(it) }
            "SAPISIDHASH ${timestamp}_${hashHex}"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating SAPISIDHASH", e)
            null
        }
    }

    private fun buildInnerTubeRequest(url: String, payloadJson: JSONObject, cookies: String): Request {
        val authHeader = generateSapisidHash(cookies)
        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("X-Origin", ORIGIN)
            .header("X-Goog-AuthUser", "0")
            .header("Cookie", cookies)
            .post(payloadJson.toString().toRequestBody("application/json".toMediaType()))

        authHeader?.let { reqBuilder.header("Authorization", it) }
        return reqBuilder.build()
    }

    /**
     * Extracts user profile info (name, avatar) from YouTube Music account cookies.
     */
    suspend fun fetchUserProfile(cookies: String): Pair<String, String?> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20241113.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val request = buildInnerTubeRequest(
                url = "https://music.youtube.com/youtubei/v1/account/account_menu?key=$INNERTUBE_API_KEY&prettyPrint=false",
                payloadJson = payload,
                cookies = cookies
            )

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val root = JSONObject(body)
                val actions = root.optJSONArray("actions")
                if (actions != null && actions.length() > 0) {
                    val header = actions.getJSONObject(0)
                        .optJSONObject("openPopupAction")
                        ?.optJSONObject("popup")
                        ?.optJSONObject("multiPageMenuRenderer")
                        ?.optJSONObject("header")
                        ?.optJSONObject("activeAccountHeaderRenderer")

                    val name = header?.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: "Google Account"
                    val avatarUrl = header?.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")?.let { thumbs ->
                        thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                    }
                    return@withContext Pair(name, avatarUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching account menu", e)
        }

        Pair("Google Account", null)
    }

    /**
     * Fetches the user's REAL Liked Songs playlist ("VLLM" / "FEmusic_liked_videos").
     */
    suspend fun fetchRealLikedSongs(cookies: String): List<Track> = withContext(Dispatchers.IO) {
        val likedEndpoints = listOf("VLLM", "FEmusic_liked_videos")
        val tracks = mutableListOf<Track>()

        for (browseId in likedEndpoints) {
            try {
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20241113.01.00")
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                    put("browseId", browseId)
                }

                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/browse?key=$INNERTUBE_API_KEY&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching liked songs with $browseId", e)
            }
        }

        // Return distinct tracks marked as favorite
        tracks.distinctBy { it.id }.map { it.copy(isFavorite = true) }
    }

    /**
     * Fetches tracks for any playlist (public or private) using authenticated InnerTube browse.
     */
    suspend fun fetchPlaylistTracks(playlistId: String, cookies: String): List<Track> = withContext(Dispatchers.IO) {
        val cleanId = playlistId.removePrefix("VL")
        val browseId = if (cleanId.startsWith("VL")) cleanId else "VL$cleanId"

        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20241113.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("browseId", browseId)
            }

            val request = buildInnerTubeRequest(
                url = "https://music.youtube.com/youtubei/v1/browse?key=$INNERTUBE_API_KEY&prettyPrint=false",
                payloadJson = payload,
                cookies = cookies
            )

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val tracks = parseTracksFromInnerTube(body)
                if (tracks.isNotEmpty()) {
                    return@withContext tracks
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error browsing playlist $playlistId via InnerTube", e)
        }

        // Fallback to unauthenticated NewPipeExtractor for public playlists
        try {
            val res = playlistImporter.importYouTubePlaylist(cleanId)
            if (res.isSuccess) {
                return@withContext res.getOrThrow().tracks
            }
        } catch (_: Exception) {}

        emptyList()
    }

    /**
     * Fetches user's REAL library playlists using authenticated InnerTube browse endpoints.
     */
    suspend fun fetchRealUserPlaylists(cookies: String): List<Playlist> = withContext(Dispatchers.IO) {
        val discoveredPlaylists = mutableListOf<PlaylistSummary>()
        val browseIds = listOf("FEmusic_liked_playlists", "FEmusic_library_landing")

        for (browseId in browseIds) {
            try {
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20241113.01.00")
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                    put("browseId", browseId)
                }

                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/browse?key=$INNERTUBE_API_KEY&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    parsePlaylistsFromInnerTube(body, discoveredPlaylists)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error browsing $browseId", e)
            }

            if (discoveredPlaylists.isNotEmpty()) break
        }

        // 1. Fetch real Liked Songs
        val likedTracks = fetchRealLikedSongs(cookies)
        val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        val importedPlaylists = mutableListOf<Playlist>()

        if (likedTracks.isNotEmpty()) {
            val ytLikedPlaylist = Playlist(
                id = "yt_liked_songs",
                title = "YouTube Liked",
                description = "Your favorite tracks from YouTube Music",
                trackCount = likedTracks.size,
                tracks = likedTracks,
                artworkUrl = likedTracks.firstOrNull()?.artworkUrl,
                source = "YouTube Music",
                isPinned = true,
                createdAt = today
            )
            importedPlaylists.add(ytLikedPlaylist)
            repository.addCustomPlaylist(ytLikedPlaylist)
        }

        // 2. Import tracks for each real playlist discovered
        for (summary in discoveredPlaylists.distinctBy { it.id }) {
            try {
                val tracks = fetchPlaylistTracks(summary.id, cookies)
                val playlist = Playlist(
                    id = "yt_${summary.id}",
                    title = summary.title,
                    description = "Synced from YouTube Music",
                    trackCount = if (tracks.isNotEmpty()) tracks.size else summary.trackCount,
                    tracks = tracks,
                    artworkUrl = summary.thumbnailUrl ?: tracks.firstOrNull()?.artworkUrl,
                    source = "YouTube Music",
                    isPinned = false,
                    createdAt = today
                )
                importedPlaylists.add(playlist)
                repository.addCustomPlaylist(playlist)
            } catch (e: Exception) {
                Log.e(TAG, "Error importing playlist ${summary.id}", e)
            }
        }

        importedPlaylists
    }

    /**
     * Parses tracks out of InnerTube JSON response.
     */
    fun parseTracksFromInnerTube(jsonStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)

            fun scanObject(obj: JSONObject) {
                val renderer = when {
                    obj.has("musicResponsiveListItemRenderer") -> obj.getJSONObject("musicResponsiveListItemRenderer")
                    obj.has("playlistVideoRenderer") -> obj.getJSONObject("playlistVideoRenderer")
                    else -> null
                }

                if (renderer != null) {
                    val track = parseSingleTrackRenderer(renderer)
                    if (track != null) {
                        tracks.add(track)
                    }
                }

                // Recurse into children
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = obj.get(key)) {
                        is JSONObject -> scanObject(value)
                        is JSONArray -> {
                            for (i in 0 until value.length()) {
                                val item = value.opt(i)
                                if (item is JSONObject) scanObject(item)
                            }
                        }
                    }
                }
            }

            scanObject(root)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing InnerTube tracks", e)
        }
        return tracks.distinctBy { it.id }
    }

    private fun parseSingleTrackRenderer(renderer: JSONObject): Track? {
        val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?: renderer.optString("videoId").takeIf { it.isNotBlank() }
            ?: findVideoIdRecursively(renderer)
            ?: return null

        val flexColumns = renderer.optJSONArray("flexColumns")
        var title = ""
        var artist = "Unknown Artist"
        var album = ""
        var durationSec = 0L

        if (flexColumns != null && flexColumns.length() > 0) {
            // Column 0: Title runs (joined completely, never taking only first run)
            val col0Runs = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (col0Runs != null) {
                val sb = StringBuilder()
                for (i in 0 until col0Runs.length()) {
                    sb.append(col0Runs.optJSONObject(i)?.optString("text", "") ?: "")
                }
                title = sb.toString().trim()
            }

            // Column 1: Artist, Album, Duration
            if (flexColumns.length() > 1) {
                val col1Runs = flexColumns.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")

                if (col1Runs != null) {
                    for (i in 0 until col1Runs.length()) {
                        val run = col1Runs.optJSONObject(i) ?: continue
                        val text = run.optString("text", "").trim()
                        val browseId = run.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")
                            ?.optString("browseId", "") ?: ""

                        when {
                            browseId.startsWith("UC") || (i == 0 && text.isNotBlank() && text != "•") -> {
                                if (artist == "Unknown Artist") artist = text
                            }
                            browseId.startsWith("MPRE") || (i > 1 && text.isNotBlank() && text != "•" && !text.matches(Regex("\\d+:\\d+"))) -> {
                                if (album.isBlank()) album = text
                            }
                            text.matches(Regex("\\d+:\\d+")) -> {
                                val parts = text.split(":")
                                if (parts.size == 2) {
                                    durationSec = (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fallback title / subtitle for playlistVideoRenderer
        if (title.isBlank()) {
            title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: renderer.optJSONObject("title")?.optString("simpleText") ?: ""
        }
        if (title.isBlank()) return null

        // Duration fallback from fixedColumns or lengthSeconds
        if (durationSec == 0L) {
            val lengthSec = renderer.optString("lengthSeconds").toLongOrNull()
            if (lengthSec != null && lengthSec > 0) {
                durationSec = lengthSec
            } else {
                val fixed = renderer.optJSONArray("fixedColumns")
                val fixedText = fixed?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                if (fixedText.matches(Regex("\\d+:\\d+"))) {
                    val parts = fixedText.split(":")
                    if (parts.size == 2) {
                        durationSec = (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                    }
                }
            }
        }

        // High resolution artwork
        val rawArtwork = renderer.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null
            }
        val highResArtwork = upgradeToHighResArtwork(rawArtwork)

        return Track(
            id = videoId,
            title = title,
            artist = artist,
            album = album.ifBlank { "YouTube Music" },
            durationSeconds = durationSec,
            artworkUrl = highResArtwork,
            streamUrl = null,
            isLocal = false
        )
    }

    private fun findVideoIdRecursively(obj: JSONObject): String? {
        val nav = obj.optJSONObject("navigationEndpoint")
        val watch = nav?.optJSONObject("watchEndpoint")
        val vId = watch?.optString("videoId")
        if (!vId.isNullOrBlank()) return vId

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            if (value is JSONObject) {
                val found = findVideoIdRecursively(value)
                if (found != null) return found
            }
        }
        return null
    }

    private fun upgradeToHighResArtwork(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return url
            .replace(Regex("=w\\d+-h\\d+.*"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*"), "=s1200")
            .replace("hqdefault.jpg", "maxresdefault.jpg")
    }

    /**
     * Parses playlists out of InnerTube JSON response.
     */
    private fun parsePlaylistsFromInnerTube(jsonStr: String, outList: MutableList<PlaylistSummary>) {
        try {
            val root = JSONObject(jsonStr)

            fun scanObject(obj: JSONObject) {
                if (obj.has("musicTwoRowItemRenderer")) {
                    val renderer = obj.getJSONObject("musicTwoRowItemRenderer")
                    val navEndpoint = renderer.optJSONObject("navigationEndpoint")
                    val browseEndpoint = navEndpoint?.optJSONObject("browseEndpoint")
                    val browseId = browseEndpoint?.optString("browseId", "") ?: ""

                    if (browseId.startsWith("VLPL") || browseId.startsWith("PL") || browseId.startsWith("VL") || browseId.startsWith("FEmusic_liked")) {
                        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.let { runs ->
                            (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text", "") }
                        } ?: "Untitled Playlist"

                        val cleanId = browseId.removePrefix("VL")
                        val thumbUrl = renderer.optJSONObject("thumbnailRenderer")
                            ?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")
                            ?.optJSONArray("thumbnails")
                            ?.let { arr ->
                                if (arr.length() > 0) arr.getJSONObject(arr.length() - 1).optString("url") else null
                            }

                        val trackCountText = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.let { runs ->
                            (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text", "") }
                        } ?: ""

                        val count = Regex("(\\d+)\\s*(?:songs|tracks)").find(trackCountText)?.groupValues?.get(1)?.toIntOrNull() ?: 25

                        outList.add(PlaylistSummary(cleanId, title, upgradeToHighResArtwork(thumbUrl), count))
                    }
                }

                // Recurse into children
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = obj.get(key)) {
                        is JSONObject -> scanObject(value)
                        is JSONArray -> {
                            for (i in 0 until value.length()) {
                                val item = value.opt(i)
                                if (item is JSONObject) scanObject(item)
                            }
                        }
                    }
                }
            }

            scanObject(root)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing InnerTube playlists", e)
        }
    }

    private data class PlaylistSummary(
        val id: String,
        val title: String,
        val thumbnailUrl: String?,
        val trackCount: Int
    )
}
