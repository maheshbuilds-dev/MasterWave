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

/**
 * Authentic, Production-Grade YouTube Music Library & Liked Songs Sync Engine:
 * - Uses captured Google / YouTube Music session cookies (SAPISID / __Secure-3PAPISID / SSID / SID).
 * - Generates cryptographically valid SAPISIDHASH for InnerTube WEB_REMIX authentication.
 * - Queries official InnerTube endpoints:
 *   - Browse "VLLM" / "FEmusic_liked_videos" (User's Liked Music songs)
 *   - Next endpoint for "VLM" / "LM" (YouTube Music queue stream for liked songs)
 *   - Browse "FEmusic_liked_playlists" / "FEmusic_library_landing" (User's library playlists)
 *   - Authenticated "VL$playlistId" and /next for each user playlist (including private playlists)
 * - Directly parses musicResponsiveListItemRenderer, playlistPanelVideoRenderer, and playlistVideoRenderer
 *   to extract exact videoIds, titles, artists, albums, durations, and high-resolution album artwork.
 * - Supports continuation tokens for multi-page library fetching.
 * - Strictly zero fake/trending song fallbacks.
 */
class YouTubeLibraryFetcher(
    private val okHttpClient: OkHttpClient,
    private val repository: MusicRepository
) {
    private val playlistImporter = PlaylistImporter(okHttpClient)

    companion object {
        private const val TAG = "YouTubeLibraryFetcher"
        private const val FALLBACK_INNERTUBE_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        private const val FALLBACK_CLIENT_VERSION = "1.20260707.12.00"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        private const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.107 Mobile Safari/537.36"
        private const val ORIGIN = "https://music.youtube.com"
        private const val CONSENT_COOKIES = "CONSENT=YES+cb.20210328-17-p0.en+FX+100; SOCS=CAISEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg"

        @Volatile
        private var dynamicApiKey: String? = null
        @Volatile
        private var dynamicClientVersion: String? = null
    }

    private fun getEffectiveApiKey(): String = dynamicApiKey ?: FALLBACK_INNERTUBE_KEY
    private fun getEffectiveClientVersion(): String = dynamicClientVersion ?: FALLBACK_CLIENT_VERSION

    private fun buildClientContext(): JSONObject {
        return JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", getEffectiveClientVersion())
                put("hl", "en")
                put("gl", "US")
            })
        }
    }

    /**
     * Computes the official Google SAPISIDHASH authentication header.
     */
    fun generateSapisidHash(cookies: String, origin: String = ORIGIN): String? {
        val secure3Match = Regex("__Secure-3PAPISID=([^;\\s]+)").find(cookies)
        val secure1Match = Regex("__Secure-1PAPISID=([^;\\s]+)").find(cookies)
        val plainMatch = Regex("(?:^|[;\\s])SAPISID=([^;\\s]+)").find(cookies)
        val apisidMatch = Regex("(?:^|[;\\s])APISID=([^;\\s]+)").find(cookies)

        val sapisid = plainMatch?.groupValues?.get(1)?.trim()
            ?: secure3Match?.groupValues?.get(1)?.trim()
            ?: secure1Match?.groupValues?.get(1)?.trim()
            ?: apisidMatch?.groupValues?.get(1)?.trim()
            ?: return null

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
        val fullCookies = if (cookies.isNotBlank()) "$CONSENT_COOKIES; $cookies" else CONSENT_COOKIES
        val reqBuilder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("User-Agent", USER_AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("X-Origin", ORIGIN)
            .header("X-Goog-Api-Format-Version", "1")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", getEffectiveClientVersion())
            .header("X-Goog-AuthUser", "0")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", fullCookies)
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
                put("context", buildClientContext())
            }

            val request = buildInnerTubeRequest(
                url = "https://music.youtube.com/youtubei/v1/account/account_menu?key=${getEffectiveApiKey()}&prettyPrint=false",
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
     * Fetches the user's REAL Liked Songs playlist ("VLLM", "FEmusic_liked_videos", "VLM", "LM").
     * Uses authentic multi-strategy retrieval matching LastWave-Native:
     * 1. Browse "VLLM"
     * 2. Browse "FEmusic_liked_videos"
     * 3. Next endpoint for playlistId "VLM"
     * 4. Next endpoint for playlistId "LM"
     * 5. Paginate continuation tokens up to 10 pages
     * NEVER falls back to trending or dummy songs. Returns strictly genuine user tracks.
     */
    suspend fun fetchRealLikedSongs(cookies: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        var continuationToken: String? = null

        // Strategy 1: InnerTube JSON browse "VLLM"
        try {
            val payload = JSONObject().apply {
                put("context", buildClientContext())
                put("browseId", "VLLM")
            }
            val request = buildInnerTubeRequest(
                url = "https://music.youtube.com/youtubei/v1/browse?key=${getEffectiveApiKey()}&prettyPrint=false",
                payloadJson = payload,
                cookies = cookies
            )
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val parsed = parseTracksFromInnerTube(body)
                if (parsed.isNotEmpty()) {
                    tracks.addAll(parsed)
                    continuationToken = extractContinuationToken(body)
                    Log.d(TAG, "Fetched ${parsed.size} liked tracks from VLLM")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error browsing VLLM", e)
        }

        // Strategy 2: InnerTube JSON browse "FEmusic_liked_videos"
        if (tracks.isEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildClientContext())
                    put("browseId", "FEmusic_liked_videos")
                }
                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/browse?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        continuationToken = extractContinuationToken(body)
                        Log.d(TAG, "Fetched ${parsed.size} liked tracks from FEmusic_liked_videos")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error browsing FEmusic_liked_videos", e)
            }
        }

        // Strategy 3: InnerTube Next endpoint with playlistId "VLM"
        if (tracks.isEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildClientContext())
                    put("playlistId", "VLM")
                    put("isAudioOnly", true)
                }
                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/next?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        continuationToken = extractContinuationToken(body)
                        Log.d(TAG, "Fetched ${parsed.size} liked tracks from next/VLM")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching liked songs from next/VLM", e)
            }
        }

        // Strategy 4: InnerTube Next endpoint with playlistId "LM"
        if (tracks.isEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildClientContext())
                    put("playlistId", "LM")
                    put("isAudioOnly", true)
                }
                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/next?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        continuationToken = extractContinuationToken(body)
                        Log.d(TAG, "Fetched ${parsed.size} liked tracks from next/LM")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching liked songs from next/LM", e)
            }
        }

        // Strategy 5: Standard YouTube browse "VLLL" (YouTube Liked Videos Playlist)
        if (tracks.isEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20241113.00.00")
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                    put("browseId", "VLLL")
                }
                val request = buildInnerTubeRequest(
                    url = "https://www.youtube.com/youtubei/v1/browse?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        continuationToken = extractContinuationToken(body)
                        Log.d(TAG, "Fetched ${parsed.size} liked tracks from YouTube browse VLLL")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error browsing YouTube VLLL", e)
            }
        }

        // Strategy 6: Standard YouTube browse "LL"
        if (tracks.isEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildClientContext())
                    put("browseId", "LL")
                }
                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/browse?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        continuationToken = extractContinuationToken(body)
                        Log.d(TAG, "Fetched ${parsed.size} liked tracks from browse LL")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error browsing LL", e)
            }
        }

        // Paginate continuations up to 10 pages to capture complete library
        var page = 0
        var currentToken = continuationToken
        while (!currentToken.isNullOrBlank() && page < 10) {
            val (nextTracks, nextToken) = fetchContinuationTracks(currentToken, cookies)
            if (nextTracks.isEmpty()) break
            tracks.addAll(nextTracks)
            if (nextToken == currentToken) break
            currentToken = nextToken
            page++
        }

        // Strictly genuine user liked tracks: zero fake fallback!
        val result = tracks.distinctBy { it.id }.map { it.copy(isFavorite = true) }
        if (result.isNotEmpty()) {
            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
            val ytLikedPlaylist = Playlist(
                id = "yt_liked_songs",
                title = "YouTube Liked",
                description = "Your favorite tracks from YouTube Music",
                trackCount = result.size,
                tracks = result,
                artworkUrl = result.firstOrNull()?.artworkUrl,
                source = "YouTube Music",
                isPinned = true,
                createdAt = today
            )
            repository.addCustomPlaylist(ytLikedPlaylist)
            repository.addAllToFavorites(result)
            Log.i(TAG, "Saved ${result.size} genuine YouTube Liked songs to repository and synced to Favorites")
        }

        result
    }

    private suspend fun fetchContinuationTracks(token: String, cookies: String): Pair<List<Track>, String?> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", buildClientContext())
            }
            val url = "https://music.youtube.com/youtubei/v1/browse?continuation=$token&key=${getEffectiveApiKey()}&prettyPrint=false"
            val request = buildInnerTubeRequest(url, payload, cookies)
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val tracks = parseTracksFromInnerTube(body)
                val nextToken = extractContinuationToken(body)
                return@withContext Pair(tracks, nextToken)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching continuation tracks", e)
        }
        Pair(emptyList(), null)
    }

    /**
     * Fetches tracks for any playlist (public or private) using authenticated InnerTube browse and /next.
     */
    suspend fun fetchPlaylistTracks(playlistId: String, cookies: String): List<Track> = withContext(Dispatchers.IO) {
        val cleanId = playlistId.removePrefix("VL")
        val browseId = if (cleanId.startsWith("VL")) cleanId else "VL$cleanId"
        val tracks = mutableListOf<Track>()
        var continuationToken: String? = null

        // 1. Authenticated InnerTube browse
        try {
            val payload = JSONObject().apply {
                put("context", buildClientContext())
                put("browseId", browseId)
            }

            val request = buildInnerTubeRequest(
                url = "https://music.youtube.com/youtubei/v1/browse?key=${getEffectiveApiKey()}&prettyPrint=false",
                payloadJson = payload,
                cookies = cookies
            )

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val parsed = parseTracksFromInnerTube(body)
                if (parsed.isNotEmpty()) {
                    tracks.addAll(parsed)
                    continuationToken = extractContinuationToken(body)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error browsing playlist $playlistId via InnerTube", e)
        }

        // 2. Fallback to /next endpoint
        if (tracks.isEmpty()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildClientContext())
                    put("playlistId", cleanId)
                    put("isAudioOnly", true)
                }
                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/next?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseTracksFromInnerTube(body)
                    if (parsed.isNotEmpty()) {
                        tracks.addAll(parsed)
                        continuationToken = extractContinuationToken(body)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching playlist $playlistId via next endpoint", e)
            }
        }

        // 3. Paginate continuations up to 10 pages
        var page = 0
        var currentToken = continuationToken
        while (!currentToken.isNullOrBlank() && page < 10) {
            val (nextTracks, nextToken) = fetchContinuationTracks(currentToken, cookies)
            if (nextTracks.isEmpty()) break
            tracks.addAll(nextTracks)
            if (nextToken == currentToken) break
            currentToken = nextToken
            page++
        }

        if (tracks.isNotEmpty()) {
            return@withContext tracks.distinctBy { it.id }
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
                    put("context", buildClientContext())
                    put("browseId", browseId)
                }

                val request = buildInnerTubeRequest(
                    url = "https://music.youtube.com/youtubei/v1/browse?key=${getEffectiveApiKey()}&prettyPrint=false",
                    payloadJson = payload,
                    cookies = cookies
                )

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    parsePlaylistsFromInnerTube(body, discoveredPlaylists)

                    // Check continuation for library playlists
                    var token = extractContinuationToken(body)
                    var page = 0
                    while (!token.isNullOrBlank() && page < 5) {
                        try {
                            val contUrl = "https://music.youtube.com/youtubei/v1/browse?continuation=$token&key=${getEffectiveApiKey()}&prettyPrint=false"
                            val contReq = buildInnerTubeRequest(contUrl, JSONObject().apply { put("context", buildClientContext()) }, cookies)
                            val contResp = okHttpClient.newCall(contReq).execute()
                            if (contResp.isSuccessful) {
                                val contBody = contResp.body?.string() ?: ""
                                parsePlaylistsFromInnerTube(contBody, discoveredPlaylists)
                                val next = extractContinuationToken(contBody)
                                if (next == token) break
                                token = next
                            } else break
                        } catch (_: Exception) { break }
                        page++
                    }
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
            repository.addAllToFavorites(likedTracks)
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
     * Parses tracks out of InnerTube JSON response or embedded HTML page (ytInitialData).
     * Supports:
     * - musicResponsiveListItemRenderer
     * - playlistPanelVideoRenderer (crucial for /next and queue items)
     * - playlistPanelVideoWrapperRenderer
     * - playlistVideoRenderer
     */
    fun parseTracksFromInnerTube(jsonOrHtml: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val jsonStr = if (jsonOrHtml.trimStart().startsWith("<")) {
                extractJsonFromHtml(jsonOrHtml) ?: return emptyList()
            } else {
                jsonOrHtml
            }
            val root = JSONObject(jsonStr)

            fun scanObject(obj: JSONObject) {
                when {
                    obj.has("playlistPanelVideoRenderer") -> {
                        val track = parsePlaylistPanelSong(obj.getJSONObject("playlistPanelVideoRenderer"))
                        if (track != null) tracks.add(track)
                    }
                    obj.has("playlistPanelVideoWrapperRenderer") -> {
                        val primary = obj.optJSONObject("playlistPanelVideoWrapperRenderer")?.optJSONObject("primaryRenderer")
                        val inner = primary?.optJSONObject("playlistPanelVideoRenderer")
                        if (inner != null) {
                            val track = parsePlaylistPanelSong(inner)
                            if (track != null) tracks.add(track)
                        }
                    }
                    obj.has("musicResponsiveListItemRenderer") -> {
                        val track = parseSingleTrackRenderer(obj.getJSONObject("musicResponsiveListItemRenderer"))
                        if (track != null) tracks.add(track)
                    }
                    obj.has("playlistVideoRenderer") -> {
                        val track = parseSingleTrackRenderer(obj.getJSONObject("playlistVideoRenderer"))
                        if (track != null) tracks.add(track)
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

    /**
     * Parses playlistPanelVideoRenderer objects returned by /next endpoint.
     */
    private fun parsePlaylistPanelSong(renderer: JSONObject): Track? {
        if (renderer.has("unplayableText")) return null
        val videoId = renderer.optString("videoId").takeIf { it.isNotBlank() }
            ?: renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
            ?: return null

        val titleRuns = renderer.optJSONObject("title")?.optJSONArray("runs")
        val title = titleRuns?.let { runs ->
            (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text", "") }
        }?.trim()?.takeIf { it.isNotBlank() }
            ?: renderer.optJSONObject("title")?.optString("simpleText")?.trim()?.takeIf { it.isNotBlank() }
            ?: return null

        val detailRuns = (renderer.optJSONObject("longBylineText") ?: renderer.optJSONObject("shortBylineText"))
            ?.optJSONArray("runs")

        var artist = "Unknown Artist"
        var album = "YouTube Music"

        if (detailRuns != null) {
            for (i in 0 until detailRuns.length()) {
                val run = detailRuns.getJSONObject(i)
                val text = run.optString("text", "").trim()
                val browseId = run.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId", "") ?: ""

                if (browseId.startsWith("UC")) {
                    artist = text
                } else if (browseId.startsWith("MPRE")) {
                    album = text
                } else if (artist == "Unknown Artist" && text.isNotBlank() && text != "•" && !text.contains("views") && !text.matches(Regex("\\d+:\\d+"))) {
                    artist = text
                }
            }
        }

        var durationSec = 0L
        val lengthRuns = renderer.optJSONObject("lengthText")?.optJSONArray("runs")
        val lengthText = if (lengthRuns != null && lengthRuns.length() > 0) {
            lengthRuns.getJSONObject(0).optString("text", "")
        } else {
            renderer.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
        }
        if (lengthText.matches(Regex("\\d+:\\d+"))) {
            val parts = lengthText.split(":")
            if (parts.size == 2) {
                durationSec = (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
            }
        }

        val rawThumb = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let { thumbs ->
            if (thumbs.length() > 0) thumbs.getJSONObject(thumbs.length() - 1).optString("url") else null
        }
        val artwork = upgradeToHighResArtwork(rawThumb) ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return Track(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSec,
            artworkUrl = artwork,
            streamUrl = null,
            isLocal = false
        )
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

        if (title.isBlank()) {
            title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: renderer.optJSONObject("title")?.optString("simpleText") ?: ""
        }
        if (title.isBlank()) return null

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

        val rawArtwork = renderer.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null
            }
        val highResArtwork = upgradeToHighResArtwork(rawArtwork) ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

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
        val nav = obj.optJSONObject("navigationEndpoint") ?: obj.optJSONObject("playNavigationEndpoint")
        val watch = nav?.optJSONObject("watchEndpoint")
        val vId = watch?.optString("videoId")
        if (!vId.isNullOrBlank()) return vId

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "videoId") {
                val direct = obj.optString("videoId")
                if (direct.isNotBlank()) return direct
            }
            when (val value = obj.opt(key)) {
                is JSONObject -> {
                    val found = findVideoIdRecursively(value)
                    if (found != null) return found
                }
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val item = value.opt(i)
                        if (item is JSONObject) {
                            val found = findVideoIdRecursively(item)
                            if (found != null) return found
                        }
                    }
                }
            }
        }
        return null
    }

    private fun extractContinuationToken(jsonStr: String): String? {
        try {
            val root = JSONObject(jsonStr)
            fun scan(obj: JSONObject): String? {
                if (obj.has("continuationCommand")) {
                    val token = obj.optJSONObject("continuationCommand")?.optString("token")
                    if (!token.isNullOrBlank()) return token
                }
                if (obj.has("nextContinuationData")) {
                    val token = obj.optJSONObject("nextContinuationData")?.optString("continuation")
                    if (!token.isNullOrBlank()) return token
                }
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    when (val v = obj.opt(k)) {
                        is JSONObject -> {
                            val t = scan(v)
                            if (t != null) return t
                        }
                        is JSONArray -> {
                            for (i in 0 until v.length()) {
                                val item = v.opt(i)
                                if (item is JSONObject) {
                                    val t = scan(item)
                                    if (t != null) return t
                                }
                            }
                        }
                    }
                }
                return null
            }
            return scan(root)
        } catch (_: Exception) {
            return null
        }
    }

    private fun upgradeToHighResArtwork(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return url
            .replace(Regex("=w\\d+-h\\d+.*"), "=w600-h600-l90-rj")
            .replace(Regex("=s\\d+.*"), "=s600")
    }

    private fun extractJsonFromHtml(html: String): String? {
        val patterns = listOf(
            Regex("(?:var\\s+ytInitialData|window\\[\"ytInitialData\"\\]|ytInitialData)\\s*=\\s*(\\{.+?\\});?\\s*<\\/script>", RegexOption.DOT_MATCHES_ALL),
            Regex("ytInitialData\\s*=\\s*(\\{.+?\\});", RegexOption.DOT_MATCHES_ALL)
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Parses playlists out of InnerTube JSON response.
     */
    private fun parsePlaylistsFromInnerTube(jsonStr: String, outList: MutableList<PlaylistSummary>) {
        try {
            val root = JSONObject(jsonStr)

            fun scanObject(obj: JSONObject) {
                // Check musicTwoRowItemRenderer
                if (obj.has("musicTwoRowItemRenderer")) {
                    val renderer = obj.getJSONObject("musicTwoRowItemRenderer")
                    val navEndpoint = renderer.optJSONObject("navigationEndpoint")
                    val browseEndpoint = navEndpoint?.optJSONObject("browseEndpoint")
                    val browseId = browseEndpoint?.optString("browseId", "") ?: ""

                    if (browseId.startsWith("VLPL") || browseId.startsWith("PL") || browseId.startsWith("VL") || browseId.startsWith("FEmusic_liked") || browseId.startsWith("RDCLAK") || browseId.startsWith("MPRE")) {
                        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.let { runs ->
                            (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text", "") }
                        } ?: renderer.optJSONObject("title")?.optString("simpleText", "") ?: "Untitled Playlist"

                        val cleanId = browseId.removePrefix("VL")
                        val thumbUrl = renderer.optJSONObject("thumbnailRenderer")
                            ?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")
                            ?.optJSONArray("thumbnails")
                            ?.let { arr ->
                                if (arr.length() > 0) arr.getJSONObject(arr.length() - 1).optString("url") else null
                            } ?: renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.let { arr ->
                                if (arr.length() > 0) arr.getJSONObject(arr.length() - 1).optString("url") else null
                            }

                        val trackCountText = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.let { runs ->
                            (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text", "") }
                        } ?: ""

                        val count = Regex("(\\d+)\\s*(?:songs|tracks)").find(trackCountText)?.groupValues?.get(1)?.toIntOrNull() ?: 25

                        if (cleanId.isNotBlank() && cleanId != "LM" && cleanId != "VLLM") {
                            outList.add(PlaylistSummary(cleanId, title, upgradeToHighResArtwork(thumbUrl), count))
                        }
                    }
                }

                // Check musicResponsiveListItemRenderer when representing a playlist
                if (obj.has("musicResponsiveListItemRenderer")) {
                    val renderer = obj.getJSONObject("musicResponsiveListItemRenderer")
                    val flexCols = renderer.optJSONArray("flexColumns")
                    val navEndpoint = flexCols?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?: renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")

                    val browseId = navEndpoint?.optString("browseId", "") ?: ""
                    if (browseId.startsWith("VLPL") || browseId.startsWith("PL") || browseId.startsWith("VL")) {
                        val title = flexCols?.optJSONObject(0)
                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")
                            ?.optJSONArray("runs")?.let { runs ->
                                (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text", "") }
                            } ?: ""

                        val cleanId = browseId.removePrefix("VL")
                        if (cleanId.isNotBlank() && title.isNotBlank() && cleanId != "LM" && cleanId != "VLLM") {
                            val thumbUrl = renderer.optJSONObject("thumbnail")
                                ?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")
                                ?.optJSONArray("thumbnails")
                                ?.let { arr ->
                                    if (arr.length() > 0) arr.getJSONObject(arr.length() - 1).optString("url") else null
                                }
                            outList.add(PlaylistSummary(cleanId, title, upgradeToHighResArtwork(thumbUrl), 25))
                        }
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
