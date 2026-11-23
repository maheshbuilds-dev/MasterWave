package com.masterwave.player.data.spotify

import android.net.Uri
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * File Import Fallback Engine:
 * Parses offline playlist export files (CSV e.g. Exportify, or JSON).
 * Converts items into read-only metadata Track models and creates a Spotify-sourced Playlist.
 */
object SpotifyFileImporter {

    suspend fun importFromFile(
        uri: Uri,
        customPlaylistTitle: String? = null
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val context = MasterWaveApp.instance
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file at selected URI."))

            val content = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }.trim()
            if (content.isBlank()) {
                return@withContext Result.failure(Exception("Selected file is empty."))
            }

            val tracks: List<Track> = if (content.startsWith("{") || content.startsWith("[")) {
                parseJsonPlaylist(content)
            } else {
                parseCsvPlaylist(content)
            }

            if (tracks.isEmpty()) {
                return@withContext Result.failure(Exception("No valid tracks could be recognized from this file."))
            }

            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
            val title = customPlaylistTitle?.ifBlank { null }
                ?: "Imported Spotify Playlist (${tracks.size} tracks)"

            val playlist = Playlist(
                id = "spotify_file_${UUID.randomUUID().toString().take(8)}",
                title = title,
                description = "Imported from playlist file on $today",
                trackCount = tracks.size,
                tracks = tracks,
                artworkUrl = tracks.firstOrNull()?.artworkUrl,
                source = "Spotify",
                isPinned = false,
                createdAt = today
            )

            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(Exception("Failed importing file: ${e.message}"))
        }
    }

    private fun parseCsvPlaylist(csvText: String): List<Track> {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val headerLine = lines[0]
        val headers = parseCsvRow(headerLine).map { it.trim().lowercase(Locale.ROOT) }

        // Locate column indices
        val idIdx = headers.indexOfFirst { it.contains("spotify id") || it == "id" || it.contains("track id") }
        val titleIdx = headers.indexOfFirst { it.contains("track name") || it == "title" || it == "name" }
        val artistIdx = headers.indexOfFirst { it.contains("artist") }
        val albumIdx = headers.indexOfFirst { it.contains("album") }
        val durationIdx = headers.indexOfFirst { it.contains("duration") }
        val imageIdx = headers.indexOfFirst { it.contains("image") || it.contains("artwork") || it.contains("url") }

        if (titleIdx == -1) return emptyList()

        val tracks = mutableListOf<Track>()
        for (i in 1 until lines.size) {
            val row = parseCsvRow(lines[i])
            if (row.size <= titleIdx) continue

            val title = row[titleIdx].trim()
            if (title.isBlank()) continue

            val rawId = if (idIdx != -1 && idIdx < row.size) row[idIdx].trim() else ""
            val trackId = if (rawId.isNotBlank()) "sp_$rawId" else "sp_file_${UUID.randomUUID().toString().take(8)}"
            val artist = if (artistIdx != -1 && artistIdx < row.size) row[artistIdx].trim().ifBlank { "Unknown Artist" } else "Unknown Artist"
            val album = if (albumIdx != -1 && albumIdx < row.size) row[albumIdx].trim().ifBlank { "Spotify" } else "Spotify"
            val artworkUrl = if (imageIdx != -1 && imageIdx < row.size) row[imageIdx].trim().ifBlank { null } else null

            var durationSeconds = 0L
            if (durationIdx != -1 && durationIdx < row.size) {
                val durStr = row[durationIdx].trim()
                val durLong = durStr.toLongOrNull()
                durationSeconds = if (durLong != null) {
                    if (durLong > 1000) durLong / 1000L else durLong
                } else 0L
            }

            tracks.add(
                Track(
                    id = trackId,
                    title = title,
                    artist = artist,
                    album = album,
                    durationSeconds = durationSeconds,
                    artworkUrl = artworkUrl,
                    streamUrl = null,
                    isLocal = false,
                    codec = "Spotify Metadata",
                    bitrateKbps = 320,
                    providerId = "spotify",
                    externalSourceUrl = if (rawId.isNotBlank()) "https://open.spotify.com/track/$rawId" else null,
                    availability = TrackAvailability.METADATA_ONLY
                )
            )
        }

        return tracks.distinctBy { it.id }
    }

    private fun parseCsvRow(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun parseJsonPlaylist(jsonText: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val jsonArray = if (jsonText.startsWith("[")) {
                JSONArray(jsonText)
            } else {
                val obj = JSONObject(jsonText)
                obj.optJSONArray("tracks") ?: obj.optJSONArray("items") ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val trackObj = item.optJSONObject("track") ?: item
                val id = trackObj.optString("id", trackObj.optString("track_id", ""))
                val title = trackObj.optString("name", trackObj.optString("title", ""))
                if (title.isBlank()) continue

                val trackId = if (id.isNotBlank()) "sp_$id" else "sp_file_${UUID.randomUUID().toString().take(8)}"
                val artist = trackObj.optString("artist", trackObj.optString("artist_name", "Unknown Artist"))
                val album = trackObj.optString("album", trackObj.optString("album_name", "Spotify"))
                val durationMs = trackObj.optLong("duration_ms", trackObj.optLong("duration", 0L))
                val artwork = trackObj.optString("artworkUrl", trackObj.optString("image_url", ""))

                tracks.add(
                    Track(
                        id = trackId,
                        title = title,
                        artist = artist,
                        album = album,
                        durationSeconds = if (durationMs > 1000) durationMs / 1000L else durationMs,
                        artworkUrl = artwork.ifBlank { null },
                        streamUrl = null,
                        isLocal = false,
                        codec = "Spotify Metadata",
                        bitrateKbps = 320,
                        providerId = "spotify",
                        externalSourceUrl = if (id.isNotBlank()) "https://open.spotify.com/track/$id" else null,
                        availability = TrackAvailability.METADATA_ONLY
                    )
                )
            }
        } catch (_: Exception) {}

        return tracks.distinctBy { it.id }
    }
}
