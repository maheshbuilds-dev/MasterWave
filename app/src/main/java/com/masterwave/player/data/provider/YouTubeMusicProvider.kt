package com.masterwave.player.data.provider

import android.net.Uri
import com.masterwave.player.data.model.Album
import com.masterwave.player.data.model.Artist
import com.masterwave.player.data.model.SearchResult
import com.masterwave.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class YouTubeMusicProvider(
    private val httpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>() // trackId -> (streamUrl, expiryEpochMs)
    private var isNewPipeInitialized = false

    init {
        initNewPipe()
    }

    private fun initNewPipe() {
        if (!isNewPipeInitialized) {
            synchronized(this) {
                if (!isNewPipeInitialized) {
                    NewPipe.init(object : Downloader() {
                        override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
                            val reqBuilder = Request.Builder()
                                .url(request.url())
                                .method(request.httpMethod(), request.dataToSend()?.toRequestBody())

                            request.headers().forEach { (k, v) ->
                                v.forEach { reqBuilder.addHeader(k, it) }
                            }
                            reqBuilder.header("User-Agent", USER_AGENT)
                            reqBuilder.header("Accept-Language", "en-US,en;q=0.9")

                            return httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                                Response(
                                    resp.code,
                                    resp.message,
                                    resp.headers.names().associateWith { resp.headers(it) },
                                    resp.body?.string().orEmpty(),
                                    resp.request.url.toString()
                                )
                            }
                        }
                    })
                    isNewPipeInitialized = true
                }
            }
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=${Uri.encode(query.trim())}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val root = json.parseToJsonElement(body).jsonArray
                    if (root.size >= 2) {
                        return@withContext root[1].jsonArray.map { it.jsonPrimitive.content }
                    }
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext SearchResult(query = query)

        // Try InnerTube search first
        val innerTubeResults = searchViaInnerTube(query)
        if (innerTubeResults.songs.isNotEmpty()) {
            return@withContext innerTubeResults
        }

        // Fallback to NewPipe search
        searchViaNewPipe(query)
    }

    private suspend fun searchViaInnerTube(query: String): SearchResult = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240401.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    }
                }
                put("query", query.trim())
                put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA==") // Filter to Songs
            }

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("User-Agent", USER_AGENT)
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext SearchResult(query)
                val root = json.parseToJsonElement(body).jsonObject
                val songs = parseInnerTubeSongs(root)
                if (songs.isNotEmpty()) {
                    return@withContext SearchResult(query = query, songs = songs)
                }
            }
        } catch (_: Exception) {}

        SearchResult(query = query)
    }

    private fun parseInnerTubeSongs(root: JsonObject): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val contents = root["contents"]?.jsonObject
                ?.get("tabbedSearchResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            contents?.forEach { section ->
                val shelf = section.jsonObject["musicShelfRenderer"]?.jsonObject
                shelf?.get("contents")?.jsonArray?.forEach { item ->
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?: item.jsonObject["musicTwoColumnItemRenderer"]?.jsonObject
                    if (renderer != null) {
                        extractTrackFromItem(renderer)?.let { tracks.add(it) }
                    }
                }
            }
        } catch (_: Exception) {}
        return tracks
    }

    private fun extractTrackFromItem(item: JsonObject): Track? {
        try {
            val flexColumns = item["flexColumns"]?.jsonArray ?: return null
            if (flexColumns.isEmpty()) return null

            val col0 = flexColumns.getOrNull(0)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject?.get("runs")?.jsonArray

            val title = col0?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: return null
            val videoId = item["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: col0.firstOrNull()?.jsonObject?.get("navigationEndpoint")?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.content
                ?: return null

            val col1 = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject?.get("runs")?.jsonArray

            var artist = "Unknown Artist"
            var album = ""
            var durationSec = 0L

            col1?.forEachIndexed { idx, run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                if (idx == 0 && text.isNotBlank() && text != "•") {
                    artist = text
                } else if (text.matches(Regex("\\d+:\\d+"))) {
                    val parts = text.split(":")
                    if (parts.size == 2) {
                        durationSec = (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                    }
                } else if (text.isNotBlank() && text != "•" && album.isBlank()) {
                    album = text
                }
            }

            val thumbRuns = item["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray

            val rawArtworkUrl = thumbRuns?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            val artworkUrl = upgradeToHighResArtwork(rawArtworkUrl)

            return Track(
                id = videoId,
                title = title,
                artist = artist,
                album = album,
                durationSeconds = durationSec,
                artworkUrl = artworkUrl,
                streamUrl = null,
                isLocal = false
            )
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun searchViaNewPipe(query: String): SearchResult = withContext(Dispatchers.IO) {
        initNewPipe()
        try {
            val searchInfo = SearchInfo.getInfo(
                ServiceList.YouTube,
                ServiceList.YouTube.searchQHFactory.fromQuery(query, listOf("songs"), "")
            )

            val tracks = searchInfo.relatedItems.mapNotNull { item ->
                if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                    Track(
                        id = item.url.substringAfter("v=").substringBefore("&"),
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown Artist",
                        durationSeconds = item.duration,
                        artworkUrl = upgradeToHighResArtwork(item.thumbnails.lastOrNull()?.url),
                        streamUrl = null,
                        isLocal = false
                    )
                } else null
            }
            SearchResult(query = query, songs = tracks)
        } catch (e: Exception) {
            SearchResult(query = query)
        }
    }

    private fun upgradeToHighResArtwork(url: String?): String? {
        if (url.isNullOrBlank()) return null
        var upgraded = url
        if (upgraded.contains("=w") || upgraded.contains("=s")) {
            upgraded = upgraded.replace(Regex("=w\\d+-h\\d+.*"), "=w1080-h1080-l90-rj")
                .replace(Regex("=s\\d+.*"), "=s1080")
        }
        if (upgraded.contains("i.ytimg.com") || upgraded.contains("img.youtube.com")) {
            upgraded = upgraded.replace("/default.jpg", "/maxresdefault.jpg")
                .replace("/mqdefault.jpg", "/maxresdefault.jpg")
                .replace("/hqdefault.jpg", "/maxresdefault.jpg")
        }
        return upgraded
    }

    suspend fun resolveStreamUrl(trackId: String): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        streamCache[trackId]?.let { (cachedUrl, expiry) ->
            if (expiry > now + 60_000L) {
                return@withContext cachedUrl
            }
        }

        initNewPipe()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$trackId")
        val audioStreams = info.audioStreams.sortedWith(
            compareByDescending<AudioStream> { it.format?.name == "WEBMA" || it.codec == "opus" }
                .thenByDescending { it.bitrate }
        )

        for (stream in audioStreams) {
            val url = stream.content
            if (!url.isNullOrBlank() && probeStream(url)) {
                streamCache[trackId] = Pair(url, now + 5 * 60 * 1000L) // 5 min cache
                return@withContext url
            }
        }

        // Fallback: use first stream directly
        audioStreams.firstOrNull()?.content ?: throw IOException("No playable audio stream found for $trackId")
    }

    /**
     * Resolves an optimal downloadable stream URL and format extension.
     * Prefers M4A (AAC-LC) for standard embedded metadata/artwork compatibility across
     * all media players, Android lockscreen, and desktop systems.
     */
    suspend fun resolveDownloadStream(trackId: String): Pair<String, String> = withContext(Dispatchers.IO) {
        initNewPipe()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$trackId")
        val audioStreams = info.audioStreams

        // 1. Try to find highest-bitrate M4A stream
        val m4aStream = audioStreams
            .filter { it.format?.name == "M4A" || it.codec?.contains("mp4a", ignoreCase = true) == true }
            .maxByOrNull { it.bitrate }

        if (m4aStream != null && !m4aStream.content.isNullOrBlank() && probeStream(m4aStream.content)) {
            return@withContext Pair(m4aStream.content, "m4a")
        }

        // 2. Try probe on any stream sorted by bitrate
        for (stream in audioStreams.sortedByDescending { it.bitrate }) {
            val url = stream.content
            if (!url.isNullOrBlank() && probeStream(url)) {
                val ext = if (stream.format?.name == "M4A" || stream.codec?.contains("mp4a", ignoreCase = true) == true) "m4a" else "opus"
                return@withContext Pair(url, ext)
            }
        }

        // 3. Fallback to first stream
        val fallback = audioStreams.firstOrNull() ?: throw IOException("No audio streams for $trackId")
        val ext = if (fallback.format?.name == "M4A" || fallback.codec?.contains("mp4a", ignoreCase = true) == true) "m4a" else "opus"
        Pair(fallback.content, ext)
    }

    private fun probeStream(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-1048575") // 1MB probe
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val code = resp.code
                val type = resp.header("Content-Type").orEmpty().lowercase()
                (code == 200 || code == 206) && !type.contains("text/html") && !type.contains("json")
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getTrending(): List<Track> = withContext(Dispatchers.IO) {
        val topQueries = listOf("Top Hits 2026", "Global Top Tracks", "Trending Music")
        for (q in topQueries) {
            val res = search(q)
            if (res.songs.isNotEmpty()) {
                return@withContext res.songs.take(20)
            }
        }
        emptyList()
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}
