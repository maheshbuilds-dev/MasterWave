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
 * Jamendo Music provider adapter for Creative Commons-licensed independent music.
 * Supports full-length MP3 streaming when configured with a valid client_id.
 */
class JamendoSourceAdapter(
    private val httpClient: OkHttpClient,
    private val clientId: String = "b6747d04" // Default or user-provided Jamendo client_id
) : MusicSourceAdapter {

    override val providerId: String = "jamendo"
    override val displayName: String = "Jamendo Music"
    override val isAuthorizedStream: Boolean = true
    override val requiresAuthentication: Boolean = true
    override val attributionNotice: String = "Music licensed by Jamendo under Creative Commons."

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://api.jamendo.com/v3.0"

    override suspend fun search(query: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank() || clientId.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/tracks/?client_id=$clientId&format=json&limit=$limit&namesearch=$encodedQuery&include=musicinfo"
        fetchFromJamendo(url)
    }

    override suspend fun getTrending(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext emptyList()
        val url = "$baseUrl/tracks/?client_id=$clientId&format=json&limit=$limit&order=popularity_week_desc&include=musicinfo"
        fetchFromJamendo(url)
    }

    override suspend fun getTracksByGenre(genre: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext emptyList()
        val encodedTag = URLEncoder.encode(genre.trim(), "UTF-8")
        val url = "$baseUrl/tracks/?client_id=$clientId&format=json&limit=$limit&fuzzytags=$encodedTag&order=popularity_week_desc&include=musicinfo"
        fetchFromJamendo(url)
    }

    override suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        if (!track.streamUrl.isNullOrBlank()) {
            track.streamUrl
        } else {
            val rawId = track.id.removePrefix("jamendo_")
            val url = "$baseUrl/tracks/?client_id=$clientId&format=json&id=$rawId"
            val request = Request.Builder().url(url).header("User-Agent", "MasterWave/2.0").build()
            try {
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body?.string() ?: return@withContext null
                    val root = json.parseToJsonElement(body).jsonObject
                    val results = root["results"]?.jsonArray ?: return@withContext null
                    val item = results.firstOrNull()?.jsonObject ?: return@withContext null
                    item["audio"]?.jsonPrimitive?.content
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun fetchFromJamendo(url: String): List<Track> {
        val request = Request.Builder().url(url).header("User-Agent", "MasterWave/2.0").build()
        return try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val headers = root["headers"]?.jsonObject
                val status = headers?.get("status")?.jsonPrimitive?.content
                if (status != "success") return emptyList()

                val results = root["results"]?.jsonArray ?: return emptyList()
                results.mapNotNull { itemElem ->
                    val item = itemElem.jsonObject
                    val id = item["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val name = item["name"]?.jsonPrimitive?.content ?: "Untitled"
                    val artistName = item["artist_name"]?.jsonPrimitive?.content ?: "Jamendo Artist"
                    val albumName = item["album_name"]?.jsonPrimitive?.content ?: ""
                    val duration = item["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 180L
                    val image = item["image"]?.jsonPrimitive?.content
                    val streamUrl = item["audio"]?.jsonPrimitive?.content
                    val license = item["license_ccurl"]?.jsonPrimitive?.content ?: "Creative Commons"

                    Track(
                        id = "jamendo_$id",
                        title = name,
                        artist = artistName,
                        album = albumName,
                        durationSeconds = duration,
                        artworkUrl = image,
                        streamUrl = streamUrl,
                        providerId = providerId,
                        license = license,
                        attribution = "Jamendo Track #$id by $artistName ($license)",
                        availability = if (!streamUrl.isNullOrBlank()) TrackAvailability.FULL_TRACK else TrackAvailability.UNAVAILABLE,
                        codec = "MP3",
                        bitrateKbps = 128
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
