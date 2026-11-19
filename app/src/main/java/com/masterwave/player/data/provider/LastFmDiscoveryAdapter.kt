package com.masterwave.player.data.provider

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

/**
 * Last.fm discovery and recommendation adapter.
 * Used exclusively for metadata, charts, tags, and musical discovery (never as an audio stream provider).
 */
class LastFmDiscoveryAdapter(
    private val httpClient: OkHttpClient,
    private val apiKey: String = "" // Configurable via Settings or preferences
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "http://ws.audioscrobbler.com/2.0/"

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun getTopTracksByTag(tag: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()
        val encodedTag = URLEncoder.encode(tag.trim(), "UTF-8")
        val url = "$baseUrl?method=tag.gettoptracks&tag=$encodedTag&api_key=$apiKey&format=json&limit=$limit"
        fetchLastFmTracks(url, genre = tag)
    }

    suspend fun getSimilarTracks(trackTitle: String, artist: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()
        val encodedTrack = URLEncoder.encode(trackTitle.trim(), "UTF-8")
        val encodedArtist = URLEncoder.encode(artist.trim(), "UTF-8")
        val url = "$baseUrl?method=track.getsimilar&track=$encodedTrack&artist=$encodedArtist&api_key=$apiKey&format=json&limit=$limit"
        fetchLastFmTracks(url, genre = null)
    }

    private fun fetchLastFmTracks(url: String, genre: String?): List<Track> {
        val request = Request.Builder().url(url).header("User-Agent", "MasterWave/2.0").build()
        return try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val tracksObj = root["tracks"]?.jsonObject ?: root["similartracks"]?.jsonObject ?: return emptyList()
                val trackList = tracksObj["track"]?.jsonArray ?: return emptyList()

                trackList.mapNotNull { trackElem ->
                    val item = trackElem.jsonObject
                    val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val artistObj = item["artist"]?.jsonObject
                    val artistName = artistObj?.get("name")?.jsonPrimitive?.content
                        ?: item["artist"]?.jsonPrimitive?.content
                        ?: "Artist"
                    val duration = item["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 200L

                    // Last.fm provides artwork via images array
                    val images = item["image"]?.jsonArray
                    val largeImg = images?.lastOrNull()?.jsonObject?.get("#text")?.jsonPrimitive?.content

                    Track(
                        id = "lastfm_${name.hashCode()}_${artistName.hashCode()}",
                        title = name,
                        artist = artistName,
                        durationSeconds = duration,
                        artworkUrl = largeImg,
                        providerId = "lastfm",
                        genre = genre,
                        availability = TrackAvailability.METADATA_ONLY,
                        attribution = "Metadata and discovery via Last.fm"
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
