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
 * Official Internet Archive (archive.org) provider adapter.
 * Accesses millions of historical 78rpm recordings, live performances, Indian classical
 * archives, and Creative Commons audio collections with direct full-length streaming.
 */
class InternetArchiveSourceAdapter(
    private val httpClient: OkHttpClient
) : MusicSourceAdapter {

    override val providerId: String = "internet_archive"
    override val displayName: String = "Internet Archive"
    override val isAuthorizedStream: Boolean = true
    override val requiresAuthentication: Boolean = false
    override val attributionNotice: String = "Public domain & Creative Commons audio from archive.org."

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://archive.org"

    override suspend fun search(query: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/advancedsearch.php?q=mediatype:audio+AND+($encodedQuery)+AND+(format:\"VBR MP3\"+OR+format:MP3)&fl[]=identifier,title,creator,year,genre,description&rows=$limit&output=json"
        fetchFromArchiveSearch(url)
    }

    override suspend fun getTrending(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/advancedsearch.php?q=mediatype:audio+AND+(collection:netlabels+OR+collection:78rpm_records+OR+collection:geetanjali)&sort[]=downloads+desc&fl[]=identifier,title,creator,year,genre&rows=$limit&output=json"
        fetchFromArchiveSearch(url)
    }

    override suspend fun getTracksByGenre(genre: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val encodedGenre = URLEncoder.encode(genre.trim(), "UTF-8")
        val url = "$baseUrl/advancedsearch.php?q=mediatype:audio+AND+(genre:$encodedGenre+OR+title:$encodedGenre+OR+collection:$encodedGenre)&sort[]=downloads+desc&fl[]=identifier,title,creator,year,genre&rows=$limit&output=json"
        fetchFromArchiveSearch(url)
    }

    override suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        val identifier = track.id.removePrefix("ia_")
        val metaUrl = "$baseUrl/metadata/$identifier/files"
        val request = Request.Builder().url(metaUrl).header("User-Agent", "MasterWave/2.0").build()

        try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val files = root["result"]?.jsonArray ?: return@withContext null

                // Pick the best MP3 file (prefer VBR MP3 or standard MP3)
                var bestFilename: String? = null
                for (fileElem in files) {
                    val fileObj = fileElem.jsonObject
                    val name = fileObj["name"]?.jsonPrimitive?.content ?: continue
                    val format = fileObj["format"]?.jsonPrimitive?.content.orEmpty()
                    if (format.contains("MP3", ignoreCase = true) || name.endsWith(".mp3", ignoreCase = true)) {
                        bestFilename = name
                        if (format.contains("VBR", ignoreCase = true)) {
                            break // VBR MP3 is ideal
                        }
                    }
                }

                if (bestFilename != null) {
                    val encodedName = URLEncoder.encode(bestFilename, "UTF-8").replace("+", "%20")
                    "$baseUrl/download/$identifier/$encodedName"
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromArchiveSearch(url: String): List<Track> {
        val request = Request.Builder().url(url).header("User-Agent", "MasterWave/2.0").build()
        return try {
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val responseObj = root["response"]?.jsonObject ?: return emptyList()
                val docs = responseObj["docs"]?.jsonArray ?: return emptyList()

                docs.mapNotNull { docElem ->
                    val doc = docElem.jsonObject
                    val identifier = doc["identifier"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = doc["title"]?.jsonPrimitive?.content ?: identifier
                    val creator = doc["creator"]?.jsonPrimitive?.content ?: "Archive Artist"
                    val year = doc["year"]?.jsonPrimitive?.content?.toIntOrNull()
                    val genre = doc["genre"]?.jsonPrimitive?.content ?: "Archive Audio"

                    val artworkUrl = "$baseUrl/services/img/$identifier"

                    Track(
                        id = "ia_$identifier",
                        title = title,
                        artist = creator,
                        album = "Internet Archive ($identifier)",
                        durationSeconds = 240L, // Estimated average; updated dynamically upon stream parse
                        artworkUrl = artworkUrl,
                        providerId = providerId,
                        releaseYear = year,
                        genre = genre,
                        license = "Public Domain / Creative Commons",
                        attribution = "Internet Archive collection item $identifier",
                        availability = TrackAvailability.FULL_TRACK,
                        codec = "MP3",
                        bitrateKbps = 192
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
