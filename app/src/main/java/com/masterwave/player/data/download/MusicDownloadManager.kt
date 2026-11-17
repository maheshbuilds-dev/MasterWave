package com.masterwave.player.data.download

import android.content.Context
import android.media.MediaMetadataRetriever
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
 * - Downloads audio streams (Opus/AAC/M4A).
 * - Saves directly to Public Internal Storage under /Music/MarshallMusic/ (accessible to user file managers).
 * - Downloads maximum-resolution artwork thumbnail (.jpg) alongside each track.
 * - Registers files with Android MediaScannerConnection so external file managers show artwork thumbnails.
 * - Exposes live downloadedTracks StateFlow.
 */
class MusicDownloadManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // Public Internal Storage /Music/MarshallMusic/ or App External Files Dir
    val musicDir: File by lazy {
        var chosenDir: File? = null
        try {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MarshallMusic")
            if (!publicDir.exists()) publicDir.mkdirs()
            val testFile = File(publicDir, ".test_${System.currentTimeMillis()}")
            if (testFile.createNewFile()) {
                testFile.delete()
                chosenDir = publicDir
            }
        } catch (ignored: Exception) {}

        if (chosenDir == null) {
            val appDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MarshallMusic")
            if (!appDir.exists()) appDir.mkdirs()
            chosenDir = appDir
        }
        chosenDir
    }

    // Also scan all potential legacy and standard directories
    private fun getScanDirectories(): List<File> {
        val dirs = mutableListOf<File>()
        try {
            dirs.add(musicDir)
            dirs.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MarshallMusic"))
            dirs.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MasterWave"))
            dirs.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), ""))
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let {
                dirs.add(File(it, "MarshallMusic"))
                dirs.add(File(it, "MasterWave"))
                dirs.add(it)
            }
            dirs.add(File(context.filesDir, "downloads"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dirs.distinctBy { it.absolutePath }.filter { it.exists() && it.isDirectory }
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
        scope.launch {
            val directories = getScanDirectories()
            val ids = mutableSetOf<String>()
            val tracks = mutableListOf<Track>()

            val retriever = MediaMetadataRetriever()

            for (dir in directories) {
                val files = dir.listFiles { _, name ->
                    name.endsWith(".opus", ignoreCase = true) ||
                            name.endsWith(".m4a", ignoreCase = true) ||
                            name.endsWith(".mp3", ignoreCase = true) ||
                            name.endsWith(".flac", ignoreCase = true) ||
                            name.endsWith(".ogg", ignoreCase = true)
                } ?: continue

                for (file in files) {
                    val baseName = file.nameWithoutExtension
                    ids.add(baseName)

                    // Find or extract matching artwork file
                    val artFile = File(dir, "$baseName.jpg").let {
                        if (it.exists() && it.length() > 0) it else File(dir, "$baseName.png")
                    }

                    var title = baseName
                    var artist = "Downloaded Track"
                    var durationMs = 0L

                    try {
                        retriever.setDataSource(file.absolutePath)
                        val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                        if (!metaTitle.isNullOrBlank()) title = metaTitle
                        if (!metaArtist.isNullOrBlank()) artist = metaArtist
                        if (!metaDuration.isNullOrBlank()) durationMs = metaDuration.toLongOrNull() ?: 0L

                        // If no artwork file exists on disk, extract embedded picture from tags
                        if (!artFile.exists() || artFile.length() == 0L) {
                            val embedded = retriever.embeddedPicture
                            if (embedded != null && embedded.isNotEmpty()) {
                                try {
                                    val targetArt = File(dir, "$baseName.jpg")
                                    FileOutputStream(targetArt).use { out ->
                                        out.write(embedded)
                                        out.flush()
                                    }
                                } catch (ignored: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        if (baseName.contains(" - ")) {
                            val parts = baseName.split(" - ", limit = 2)
                            title = parts[0].trim()
                            artist = parts[1].trim()
                        }
                    }

                    val finalArt = File(dir, "$baseName.jpg").let {
                        if (it.exists() && it.length() > 0) it else File(dir, "$baseName.png")
                    }
                    val artworkPath = if (finalArt.exists() && finalArt.length() > 0) "file://${finalArt.absolutePath}" else null

                    val track = Track(
                        id = "download_${file.name}",
                        title = title,
                        artist = artist,
                        durationSeconds = durationMs / 1000L,
                        artworkUrl = artworkPath,
                        streamUrl = file.absolutePath,
                        isLocal = true,
                        localUri = file.absolutePath
                    )
                    tracks.add(track)
                }
            }

            try {
                retriever.release()
            } catch (ignored: Exception) {}

            val deduplicatedTracks = tracks.distinctBy {
                "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}"
            }
            val deduplicatedIds = deduplicatedTracks.map { it.id }.toSet() +
                    deduplicatedTracks.map { it.title.trim().lowercase() }.toSet() +
                    deduplicatedTracks.map { File(it.streamUrl).nameWithoutExtension }.toSet()

            _downloadedIds.value = deduplicatedIds
            _downloadedTracks.value = deduplicatedTracks
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        if (_downloadedIds.value.contains(trackId)) return true
        val track = repository.localTracks.value.find { it.id == trackId }
        return track != null && track.isLocal
    }

    /**
     * Downloads genuine audio stream + maximum resolution thumbnail directly
     * to device internal storage (/Music/MarshallMusic/).
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
                    ) { _, _ -> }

                    // 4. Update live state
                    _downloadedIds.value = _downloadedIds.value + track.id + baseName
                    _downloadProgress.value = _downloadProgress.value - track.id

                    val downloadedTrack = track.copy(
                        id = "download_${audioFile.name}",
                        streamUrl = audioFile.absolutePath,
                        artworkUrl = if (artworkFile.exists()) "file://${artworkFile.absolutePath}" else track.artworkUrl,
                        isLocal = true,
                        localUri = audioFile.absolutePath
                    )

                    _downloadedTracks.value = (_downloadedTracks.value + downloadedTrack).distinctBy {
                        "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}"
                    }

                    withContext(Dispatchers.Main) {
                        repository.addCustomPlaylist(
                            repository.playlists.value.find { it.id == "local_songs" }?.let { pl ->
                                pl.copy(trackCount = pl.trackCount + 1, tracks = pl.tracks + downloadedTrack)
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

    fun deleteDownload(track: Track) {
        scope.launch {
            try {
                val stream = track.streamUrl ?: return@launch
                val file = File(stream)
                if (file.exists()) {
                    file.delete()
                }
                val artFile = File(stream.substringBeforeLast(".") + ".jpg")
                if (artFile.exists()) {
                    artFile.delete()
                }
                scanDownloadedFiles()
            } catch (e: Exception) {
                e.printStackTrace()
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
