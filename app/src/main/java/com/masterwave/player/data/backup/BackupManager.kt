package com.masterwave.player.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeMode
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Launcher-grade Application Backup & Restore Engine for MasterWave Music:
 * Serializes and restores all user configurations, custom themes, playlists,
 * favorites, and audio engine settings via portable .mwbackup JSON files.
 */
class BackupManager(
    private val context: Context,
    private val repository: MusicRepository,
    private val player: MasterWavePlayer
) {
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("format", "MasterWaveBackup")
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // 1. Settings & Preferences
        val settings = JSONObject()
        settings.put("themeMode", ThemeState.themeMode.value.name)
        settings.put("accentColor", String.format("#%08X", ThemeState.accentColor.value.toArgb()))
        settings.put("dynamicColorEnabled", ThemeState.dynamicColorEnabled.value)
        settings.put("liquidGlassEnabled", ThemeState.liquidGlassEnabled.value)
        settings.put("wavySeekbarEnabled", ThemeState.wavySeekbarEnabled.value)
        settings.put("volume", player.volume.value)
        settings.put("playbackSpeed", player.playbackSpeed.value)
        settings.put("playbackPitch", player.playbackPitch.value)
        settings.put("isSpeedPitchLinked", player.isSpeedPitchLinked.value)
        settings.put("musicHapticsEnabled", player.musicHapticsEnabled.value)
        root.put("settings", settings)

        // 2. Favorites
        val favsArray = JSONArray()
        repository.favorites.value.forEach { track ->
            val trackObj = JSONObject()
            trackObj.put("id", track.id)
            trackObj.put("title", track.title)
            trackObj.put("artist", track.artist)
            trackObj.put("album", track.album)
            trackObj.put("durationSeconds", track.durationSeconds)
            trackObj.put("artworkUrl", track.artworkUrl ?: "")
            trackObj.put("streamUrl", track.streamUrl ?: "")
            favsArray.put(trackObj)
        }
        root.put("favorites", favsArray)

        // 3. Custom Playlists
        val playlistsArray = JSONArray()
        repository.playlists.value.forEach { pl ->
            val plObj = JSONObject()
            plObj.put("id", pl.id)
            plObj.put("title", pl.title)
            plObj.put("description", pl.description)
            val plTracks = JSONArray()
            pl.tracks.forEach { track ->
                val trackObj = JSONObject()
                trackObj.put("id", track.id)
                trackObj.put("title", track.title)
                trackObj.put("artist", track.artist)
                trackObj.put("album", track.album)
                trackObj.put("durationSeconds", track.durationSeconds)
                trackObj.put("artworkUrl", track.artworkUrl ?: "")
                trackObj.put("streamUrl", track.streamUrl ?: "")
                plTracks.put(trackObj)
            }
            plObj.put("tracks", plTracks)
            playlistsArray.put(plObj)
        }
        root.put("playlists", playlistsArray)

        // 4. Recent Tracks History
        val recentArray = JSONArray()
        repository.recentTracks.value.take(50).forEach { track ->
            val trackObj = JSONObject()
            trackObj.put("id", track.id)
            trackObj.put("title", track.title)
            trackObj.put("artist", track.artist)
            trackObj.put("artworkUrl", track.artworkUrl ?: "")
            recentArray.put(trackObj)
        }
        root.put("recentTracks", recentArray)

        root.toString(2)
    }

    suspend fun exportToDownloads(): File = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "MasterWave_Backup_$timeStamp.mwbackup"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFile = if (downloadsDir.exists() && downloadsDir.canWrite()) {
            File(downloadsDir, fileName)
        } else {
            val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            File(appDir, fileName)
        }

        FileOutputStream(targetFile).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.flush()
        }
        targetFile
    }

    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext false
            restoreFromJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromJson(jsonString: String): Boolean = withContext(Dispatchers.Main) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("format") || root.getString("format") != "MasterWaveBackup") {
                return@withContext false
            }

            // 1. Restore Settings
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                if (s.has("themeMode")) {
                    try {
                        val tm = ThemeMode.valueOf(s.getString("themeMode"))
                        ThemeState.setThemeMode(tm)
                    } catch (ignored: Exception) {}
                }
                if (s.has("accentColor")) {
                    try {
                        val hex = s.getString("accentColor")
                        val argb = android.graphics.Color.parseColor(hex)
                        ThemeState.setAccentColor(Color(argb))
                    } catch (ignored: Exception) {}
                }
                if (s.has("dynamicColorEnabled")) {
                    ThemeState.setDynamicColorEnabled(s.getBoolean("dynamicColorEnabled"))
                }
                if (s.has("liquidGlassEnabled")) {
                    ThemeState.setLiquidGlassEnabled(s.getBoolean("liquidGlassEnabled"))
                }
                if (s.has("wavySeekbarEnabled")) {
                    ThemeState.setWavySeekbarEnabled(s.getBoolean("wavySeekbarEnabled"))
                }
                if (s.has("volume")) {
                    player.setVolume(s.getDouble("volume").toFloat())
                }
                if (s.has("playbackSpeed")) {
                    player.setPlaybackSpeed(s.getDouble("playbackSpeed").toFloat())
                }
                if (s.has("playbackPitch")) {
                    player.setPlaybackPitch(s.getDouble("playbackPitch").toFloat())
                }
                if (s.has("musicHapticsEnabled")) {
                    player.toggleMusicHaptics(s.getBoolean("musicHapticsEnabled"))
                }
            }

            // 2. Restore Favorites
            if (root.has("favorites")) {
                val favsArray = root.getJSONArray("favorites")
                for (i in 0 until favsArray.length()) {
                    val obj = favsArray.getJSONObject(i)
                    val track = Track(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.optString("album", ""),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        artworkUrl = obj.optString("artworkUrl", "").takeIf { it.isNotBlank() },
                        streamUrl = obj.optString("streamUrl", "").takeIf { it.isNotBlank() }
                    )
                    repository.addToFavorites(track)
                }
            }

            // 3. Restore Playlists
            if (root.has("playlists")) {
                val plArray = root.getJSONArray("playlists")
                for (i in 0 until plArray.length()) {
                    val plObj = plArray.getJSONObject(i)
                    val id = plObj.getString("id")
                    val title = plObj.getString("title")
                    val desc = plObj.optString("description", "")
                    val tracksList = mutableListOf<Track>()

                    if (plObj.has("tracks")) {
                        val trkArr = plObj.getJSONArray("tracks")
                        for (j in 0 until trkArr.length()) {
                            val to = trkArr.getJSONObject(j)
                            tracksList.add(
                                Track(
                                    id = to.getString("id"),
                                    title = to.getString("title"),
                                    artist = to.getString("artist"),
                                    album = to.optString("album", ""),
                                    durationSeconds = to.optLong("durationSeconds", 0L),
                                    artworkUrl = to.optString("artworkUrl", "").takeIf { it.isNotBlank() },
                                    streamUrl = to.optString("streamUrl", "").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }

                    val playlist = Playlist(
                        id = id,
                        title = title,
                        description = desc,
                        trackCount = tracksList.size,
                        tracks = tracksList
                    )
                    repository.addCustomPlaylist(playlist)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Feature 9: Playlist Backup & Restore (Standard M3U8/M3U format).
     * Exports a playlist to standard Extended M3U representation.
     */
    fun exportPlaylistToM3u(playlist: Playlist): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        sb.append("#PLAYLIST:${playlist.title}\n")
        playlist.tracks.forEach { track ->
            val duration = track.durationSeconds
            sb.append("#EXTINF:$duration,${track.artist} - ${track.title}\n")
            val location = track.localUri ?: track.streamUrl ?: track.id
            sb.append(location).append("\n")
        }
        return sb.toString()
    }

    /**
     * Imports an M3U format playlist string with duplicate suppression.
     */
    suspend fun importPlaylistFromM3u(m3uContent: String, playlistName: String): Playlist = withContext(Dispatchers.IO) {
        val lines = m3uContent.lines()
        val tracks = mutableListOf<Track>()
        var currentTitle = ""
        var currentArtist = ""
        var currentDuration = 0L

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                val commaIdx = trimmed.indexOf(',')
                val info = trimmed.substring(8, if (commaIdx > 8) commaIdx else trimmed.length)
                currentDuration = info.toLongOrNull() ?: 0L
                if (commaIdx > 0 && commaIdx < trimmed.length - 1) {
                    val fullTitle = trimmed.substring(commaIdx + 1).trim()
                    if (fullTitle.contains(" - ")) {
                        val parts = fullTitle.split(" - ", limit = 2)
                        currentArtist = parts[0].trim()
                        currentTitle = parts[1].trim()
                    } else {
                        currentTitle = fullTitle
                        currentArtist = "Unknown"
                    }
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val id = "m3u_${System.currentTimeMillis()}_${tracks.size}"
                val isLocalPath = trimmed.startsWith("/") || trimmed.startsWith("file://") || trimmed.startsWith("content://")
                tracks.add(
                    Track(
                        id = id,
                        title = currentTitle.ifBlank { trimmed.substringAfterLast('/').substringBeforeLast('.') },
                        artist = currentArtist.ifBlank { "Unknown" },
                        durationSeconds = currentDuration,
                        streamUrl = trimmed,
                        localUri = if (isLocalPath) trimmed else null,
                        isLocal = isLocalPath
                    )
                )
                currentTitle = ""
                currentArtist = ""
                currentDuration = 0L
            }
        }

        val finalName = playlistName.ifBlank { "Imported M3U Playlist" }
        val imported = Playlist(
            id = "m3u_${System.currentTimeMillis()}",
            title = finalName,
            description = "Imported from standard M3U playlist file",
            trackCount = tracks.size,
            tracks = tracks.distinctBy { it.streamUrl ?: it.title },
            source = "Local"
        )
        repository.addCustomPlaylist(imported)
        imported
    }
}
