package com.masterwave.player.data.download

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Real Offline Music Download Manager:
 * - Downloads genuine high-quality audio streams (Opus/AAC).
 * - Saves directly to Public Internal Storage under /Music/MasterWave/ so files are accessible to user file managers.
 * - Downloads maximum-resolution artwork thumbnail (.jpg) alongside each track.
 * - Registers files with Android MediaScannerConnection so they immediately appear in system media library.
 */
class MusicDownloadManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // Public Internal Storage /Music/MasterWave/ (accessible by Files by Google, etc.)
    val musicDir: File by lazy {
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MasterWave")
        if (!publicDir.exists()) {
            publicDir.mkdirs()
        }
        if (publicDir.exists() && publicDir.canWrite()) {
            publicDir
        } else {
            val appDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MasterWave")
            if (!appDir.exists()) appDir.mkdirs()
            appDir
        }
    }

    init {
        scanDownloadedFiles()
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun getHighResArtworkUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return url
            .replace(Regex("=w\\d+-h\\d+.*"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*"), "=s1200")
            .replace(Regex("/default\\.jpg$"), "/maxresdefault.jpg")
            .replace(Regex("/hqdefault\\.jpg$"), "/maxresdefault.jpg")
    }

    fun scanDownloadedFiles() {
        val files = musicDir.listFiles { _, name ->
            name.endsWith(".opus") || name.endsWith(".m4a") || name.endsWith(".mp3")
        }
        val ids = mutableSetOf<String>()
        files?.forEach { file ->
            val name = file.nameWithoutExtension
            // Check if name has trackId encoded or base name
            ids.add(name)
        }
        _downloadedIds.value = ids
    }

    fun isDownloaded(trackId: String): Boolean {
        if (_downloadedIds.value.contains(trackId)) return true
        val track = repository.localTracks.value.find { it.id == trackId }
        return track != null && track.isLocal
    }

    /**
     * Downloads genuine audio stream + maximum resolution thumbnail directly
     * to device internal storage (/Music/MasterWave/).
     */
    fun downloadTrack(track: Track) {
        if (isDownloaded(track.id)) return

        scope.launch {
            try {
                _downloadProgress.value = _downloadProgress.value + (track.id to 0.05f)
                val streamUrl = repository.resolveStreamUrl(track)

                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body ?: return@launch
                    val contentLength = body.contentLength().coerceAtLeast(1L)
                    val ext = if (track.codec.contains("AAC", ignoreCase = true)) "m4a" else "opus"

                    val safeTitle = sanitizeFilename(track.title)
                    val safeArtist = sanitizeFilename(track.artist)
                    val baseName = if (safeArtist.isNotBlank() && safeArtist != "Unknown") "$safeTitle - $safeArtist" else safeTitle

                    val audioFile = File(musicDir, "$baseName.$ext")
                    val artworkFile = File(musicDir, "$baseName.jpg")

                    // 1. Download Audio Stream
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(audioFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        _downloadProgress.value = _downloadProgress.value + (track.id to (totalRead.toFloat() / contentLength.toFloat()))
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    // 2. Download Full High-Quality Artwork Thumbnail
                    val highResUrl = getHighResArtworkUrl(track.artworkUrl) ?: track.artworkUrl
                    if (!highResUrl.isNullOrBlank()) {
                        try {
                            val artRequest = Request.Builder().url(highResUrl).build()
                            val artResp = okHttpClient.newCall(artRequest).execute()
                            if (artResp.isSuccessful) {
                                artResp.body?.byteStream()?.use { artIn ->
                                    FileOutputStream(artworkFile).use { artOut ->
                                        artIn.copyTo(artOut)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // 3. Register with Android MediaScanner for Public Media Library Indexing
                    val filesToScan = if (artworkFile.exists()) {
                        arrayOf(audioFile.absolutePath, artworkFile.absolutePath)
                    } else {
                        arrayOf(audioFile.absolutePath)
                    }
                    val mimesToScan = if (artworkFile.exists()) {
                        arrayOf("audio/*", "image/jpeg")
                    } else {
                        arrayOf("audio/*")
                    }

                    MediaScannerConnection.scanFile(
                        context,
                        filesToScan,
                        mimesToScan
                    ) { path, uri ->
                        // Scanned and indexed by Android system
                    }

                    // 4. Mark as downloaded and link locally in app
                    _downloadedIds.value = _downloadedIds.value + track.id + baseName
                    _downloadProgress.value = _downloadProgress.value - track.id

                    val localTrack = track.copy(
                        id = "local_${audioFile.nameWithoutExtension}",
                        streamUrl = audioFile.absolutePath,
                        artworkUrl = if (artworkFile.exists()) artworkFile.absolutePath else track.artworkUrl,
                        isLocal = true,
                        localUri = audioFile.absolutePath
                    )
                    withContext(Dispatchers.Main) {
                        repository.addCustomPlaylist(
                            repository.playlists.value.find { it.id == "local_songs" }?.let { pl ->
                                pl.copy(trackCount = pl.trackCount + 1, tracks = pl.tracks + localTrack)
                            } ?: return@withContext
                        )
                        repository.loadLocalTracks()
                    }
                } else {
                    _downloadProgress.value = _downloadProgress.value - track.id
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadProgress.value = _downloadProgress.value - track.id
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MusicDownloadManager? = null

        fun getInstance(context: Context, okHttpClient: OkHttpClient, repository: MusicRepository): MusicDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicDownloadManager(context.applicationContext, okHttpClient, repository).also { INSTANCE = it }
            }
        }
    }
}
