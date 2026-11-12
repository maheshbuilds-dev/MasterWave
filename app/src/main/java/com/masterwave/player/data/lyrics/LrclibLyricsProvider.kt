package com.masterwave.player.data.lyrics

import com.masterwave.player.data.model.LyricLine
import com.masterwave.player.data.model.LyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

@Serializable
data class LrclibRecord(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

class LrclibLyricsProvider(
    private val client: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cache = mutableMapOf<String, LyricsResult?>()
    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    suspend fun getLyrics(
        trackId: String,
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Long = 0L
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val cacheKey = "$artist - $title"
        if (cache.containsKey(cacheKey)) {
            return@withContext cache[cacheKey]
        }

        val record = fetchLrclibRecord(title, artist, album, durationSeconds)
        val result = if (record != null) {
            val synced = record.syncedLyrics
            if (!synced.isNullOrBlank()) {
                val lines = parseLrc(synced)
                LyricsResult(
                    trackId = trackId,
                    isSynced = true,
                    lines = lines,
                    plainLyrics = record.plainLyrics ?: lines.joinToString("\n") { it.text }
                )
            } else if (!record.plainLyrics.isNullOrBlank()) {
                val plainLines = record.plainLyrics.lines().mapIndexed { index, text ->
                    LyricLine(timestampMs = index * 3000L, text = text)
                }
                LyricsResult(
                    trackId = trackId,
                    isSynced = false,
                    lines = plainLines,
                    plainLyrics = record.plainLyrics
                )
            } else {
                null
            }
        } else {
            null
        }

        cache[cacheKey] = result
        result
    }

    private fun fetchLrclibRecord(
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Long
    ): LrclibRecord? {
        // 1. Try exact match
        tryExactMatch(title, artist, album, durationSeconds)?.let { return it }

        // 2. Try exact match without album
        tryExactMatch(title, artist, null, durationSeconds)?.let { return it }

        // 3. Clean title and artist
        val cleanTitle = cleanTitle(title)
        val cleanArtist = cleanArtist(artist)
        if (cleanTitle != title || cleanArtist != artist) {
            tryExactMatch(cleanTitle, cleanArtist, null, durationSeconds)?.let { return it }
        }

        // 4. Try search endpoint
        return trySearch(cleanTitle, cleanArtist) ?: trySearch(title, artist)
    }

    private fun tryExactMatch(
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Long
    ): LrclibRecord? {
        val urlBuilder = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder() ?: return null
        urlBuilder.addQueryParameter("track_name", title.trim())
        urlBuilder.addQueryParameter("artist_name", artist.trim())
        if (!album.isNullOrBlank()) {
            urlBuilder.addQueryParameter("album_name", album.trim())
        }
        if (durationSeconds > 0) {
            urlBuilder.addQueryParameter("duration", durationSeconds.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "MasterWave/1.0 (Android; https://github.com/masterwave)")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        json.decodeFromString<LrclibRecord>(body)
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun trySearch(title: String, artist: String): LrclibRecord? {
        val query = "$title $artist".trim()
        val urlBuilder = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder() ?: return null
        urlBuilder.addQueryParameter("q", query)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "MasterWave/1.0 (Android; https://github.com/masterwave)")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val list = json.decodeFromString<List<LrclibRecord>>(body)
                    list.firstOrNull { it.syncedLyrics != null } ?: list.firstOrNull()
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (rawLine in lrcText.lines()) {
            val matcher = lrcPattern.matcher(rawLine.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val fracStr = matcher.group(3) ?: "0"
                val frac = if (fracStr.length == 2) fracStr.toLong() * 10 else fracStr.toLong()
                val text = matcher.group(4)?.trim() ?: ""

                val timestampMs = min * 60_000L + sec * 1_000L + frac
                lines.add(LyricLine(timestampMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\s*[\\[(](?:official|audio|video|lyrics|hd|4k|hq|remastered|explicit|clean).*?[\\])]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*[\\[(]feat\\..*?[\\])]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*(?:official|audio|video|lyrics).*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*VEVO$", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
