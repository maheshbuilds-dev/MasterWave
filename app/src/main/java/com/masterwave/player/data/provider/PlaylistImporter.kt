package com.masterwave.player.data.provider

import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PlaylistImporter(private val okHttpClient: OkHttpClient) {

    /**
     * Import a playlist from a YouTube playlist URL or ID.
     * Uses NewPipeExtractor (zero API keys).
     */
    suspend fun importYouTubePlaylist(urlOrId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistUrl = if (urlOrId.startsWith("http://") || urlOrId.startsWith("https://")) {
                urlOrId
            } else {
                "https://www.youtube.com/playlist?list=$urlOrId"
            }

            val playlistInfo = PlaylistInfo.getInfo(NewPipe.getService(0), playlistUrl)
            val tracks = playlistInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { item ->
                Track(
                    id = item.url.substringAfter("v=").substringBefore("&").ifBlank { UUID.randomUUID().toString() },
                    title = item.name ?: "Unknown Track",
                    artist = item.uploaderName ?: "Unknown Artist",
                    album = playlistInfo.name ?: "YouTube Playlist",
                    durationSeconds = item.duration,
                    artworkUrl = item.thumbnails.lastOrNull()?.url,
                    streamUrl = null,
                    codec = "Opus",
                    bitrateKbps = 160
                )
            }

            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

            Playlist(
                id = "yt_${playlistInfo.id ?: UUID.randomUUID().toString()}",
                title = playlistInfo.name ?: "YouTube Playlist",
                description = playlistInfo.description?.content ?: "Imported from YouTube",
                trackCount = tracks.size,
                tracks = tracks,
                artworkUrl = playlistInfo.thumbnails.lastOrNull()?.url ?: tracks.firstOrNull()?.artworkUrl,
                source = "YouTube",
                isPinned = false,
                createdAt = today
            )
        }
    }

    /**
     * Import a playlist from a Spotify URL using the public Embed HTML / JSON parser.
     * Zero Spotify API credentials or tokens needed.
     */
    suspend fun importSpotifyPlaylist(urlOrId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val playlistId = extractSpotifyPlaylistId(urlOrId)
                ?: throw IllegalArgumentException("Invalid Spotify playlist URL or ID")

            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to fetch Spotify embed: HTTP ${response.code}")
            }

            val html = response.body?.string() ?: throw IllegalStateException("Empty Spotify response")
            val doc = Jsoup.parse(html)

            var playlistTitle = doc.title().replace(" | Spotify", "").trim()
            if (playlistTitle.isBlank()) playlistTitle = "Spotify Playlist"

            val tracks = mutableListOf<Track>()
            val scriptElem = doc.selectFirst("script#__NEXT_DATA__")

            if (scriptElem != null) {
                val jsonText = scriptElem.data()
                val root = JSONObject(jsonText)
                val props = root.optJSONObject("props")
                val pageProps = props?.optJSONObject("pageProps")
                val state = pageProps?.optJSONObject("state")
                val data = state?.optJSONObject("data")
                val entity = data?.optJSONObject("entity")

                if (entity != null) {
                    val name = entity.optString("name", playlistTitle)
                    if (name.isNotBlank()) playlistTitle = name

                    val trackList = entity.optJSONArray("trackList")
                    if (trackList != null) {
                        for (i in 0 until trackList.length()) {
                            val trackObj = trackList.getJSONObject(i)
                            val title = trackObj.optString("title", "Unknown")
                            val subtitle = trackObj.optString("subtitle", "Unknown Artist")
                            val durationMs = trackObj.optLong("duration", 0L)

                            tracks.add(
                                Track(
                                    id = "sp_${playlistId}_$i",
                                    title = title,
                                    artist = subtitle,
                                    album = playlistTitle,
                                    durationSeconds = durationMs / 1000L,
                                    artworkUrl = null,
                                    streamUrl = null,
                                    codec = "Opus",
                                    bitrateKbps = 160
                                )
                            )
                        }
                    }
                }
            }

            // Fallback scraping if __NEXT_DATA__ structure changed
            if (tracks.isEmpty()) {
                val trackRows = doc.select("li, [data-testid='tracklist-row']")
                trackRows.forEachIndexed { index, row ->
                    val name = row.select(".track-name, [data-testid='track-name']").text()
                    val artist = row.select(".artist-name, [data-testid='artist-name']").text()
                    if (name.isNotBlank()) {
                        tracks.add(
                            Track(
                                id = "sp_${playlistId}_$index",
                                title = name,
                                artist = artist.ifBlank { "Unknown Artist" },
                                album = playlistTitle,
                                durationSeconds = 180L,
                                artworkUrl = null,
                                streamUrl = null,
                                codec = "Opus",
                                bitrateKbps = 160
                            )
                        )
                    }
                }
            }

            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

            Playlist(
                id = "spotify_$playlistId",
                title = playlistTitle,
                description = "Imported from Spotify",
                trackCount = tracks.size,
                tracks = tracks,
                artworkUrl = null,
                source = "Spotify",
                isPinned = false,
                createdAt = today
            )
        }
    }

    private fun extractSpotifyPlaylistId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("^[a-zA-Z0-9]{22}$"))) {
            return trimmed
        }
        val match = Regex("playlist[/:]+([a-zA-Z0-9]{22})").find(trimmed)
        return match?.groupValues?.get(1)
    }
}
