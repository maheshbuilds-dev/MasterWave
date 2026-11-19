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

/**
 * Official Audius decentralized music provider adapter.
 * Streams royalty-free, authorized independent music at full length.
 */
class AudiusSourceAdapter(
    private val httpClient: OkHttpClient
) : MusicSourceAdapter {

    override val providerId: String = "audius"
    override val displayName: String = "Audius Music"
    override val isAuthorizedStream: Boolean = true
    override val requiresAuthentication: Boolean = false
    override val attributionNotice: String = "Music streamed via Audius Decentralized Protocol under creator license."

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val appName = "MasterWave"
    private val baseUrl = "https://api.audius.co/v1"

    override suspend fun search(query: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "$baseUrl/tracks/search?query=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}&limit=$limit&app_name=$appName"
        fetchTracksFromUrl(url)
    }

    override suspend fun getTrending(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/tracks/trending?limit=$limit&app_name=$appName"
        fetchTracksFromUrl(url)
    }

    override suspend fun getTracksByGenre(genre: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/tracks/trending?genre=${java.net.URLEncoder.encode(genre.trim(), "UTF-8")}&limit=$limit&app_name=$appName"
        fetchTracksFromUrl(url)
    }

    override suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        val rawId = track.id.removePrefix("audius_")
        "$baseUrl/tracks/$rawId/stream?app_name=$appName"
    }

    override suspend fun getTrackDetails(trackId: String): Track? = withContext(Dispatchers.IO) {
        val rawId = trackId.removePrefix("audius_")
        val url = "$baseUrl/tracks/$rawId?app_name=$appName"
        val request = Request.Builder().url(url).header("User-Agent", "MasterWave/2.0").build()
        try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val item = root["data"]?.jsonObject ?: return@withContext null
                parseTrackObject(item)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchTracksFromUrl(url: String): List<Track> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MasterWave/2.0")
            .build()

        return try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonArray ?: return emptyList()
                data.mapNotNull { element ->
                    parseTrackObject(element.jsonObject)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTrackObject(item: kotlinx.serialization.json.JsonObject): Track? {
        val id = item["id"]?.jsonPrimitive?.content
            ?: item["track_id"]?.jsonPrimitive?.content
            ?: return null
        val title = item["title"]?.jsonPrimitive?.content ?: "Untitled"
        val userObj = item["user"]?.jsonObject
        val artist = userObj?.get("name")?.jsonPrimitive?.content
            ?: item["user_name"]?.jsonPrimitive?.content
            ?: "Audius Artist"
        val duration = item["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 180L
        val genre = item["genre"]?.jsonPrimitive?.content ?: "Indie"
        val artworkObj = item["artwork"]?.jsonObject
        val artworkUrl = artworkObj?.get("480x480")?.jsonPrimitive?.content
            ?: artworkObj?.get("150x150")?.jsonPrimitive?.content
            ?: item["cover_art"]?.jsonPrimitive?.content

        val rawId = id
        val trackId = "audius_$rawId"
        val stream = "$baseUrl/tracks/$rawId/stream?app_name=$appName"

        return Track(
            id = trackId,
            title = title,
            artist = artist,
            album = "Audius Single",
            durationSeconds = duration,
            artworkUrl = artworkUrl,
            streamUrl = stream,
            providerId = providerId,
            genre = genre,
            license = "Audius Creator License",
            attribution = "Licensed on Audius by $artist",
            availability = TrackAvailability.FULL_TRACK,
            codec = "MP3",
            bitrateKbps = 320
        )
    }
}
