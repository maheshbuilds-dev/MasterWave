package com.masterwave.player.data.download

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.R
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
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
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Real Offline Music Download Manager:
 * - Downloads audio streams (Opus/AAC/M4A).
 * - Saves directly to Public Internal Storage under /Music/MasterWave/ (accessible to user file managers).
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

    private val downloadPreferences by lazy {
        context.getSharedPreferences(DOWNLOADS_PREFERENCES, Context.MODE_PRIVATE)
    }
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    @Volatile
    private var notificationPermissionWarningShown = false
    private val activeDownloadIds = ConcurrentHashMap.newKeySet<String>()
    private val downloadRegistryLock = Any()

    // Public Internal Storage /Music/MasterWave/ or App External Files Dir
    val musicDir: File by lazy {
        var chosenDir: File? = null
        try {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MasterWave")
            if (!publicDir.exists()) publicDir.mkdirs()
            val testFile = File(publicDir, ".test_${System.currentTimeMillis()}")
            if (testFile.createNewFile()) {
                testFile.delete()
                chosenDir = publicDir
            }
        } catch (ignored: Exception) {}

        if (chosenDir == null) {
            val appDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MasterWave")
            if (!appDir.exists()) appDir.mkdirs()
            chosenDir = appDir
        }
        chosenDir
    }

    private fun registeredDownloadPaths(): Set<String> = synchronized(downloadRegistryLock) {
        downloadPreferences.getStringSet(KEY_COMPLETED_DOWNLOADS, emptySet())?.toSet().orEmpty()
    }

    private fun registerDownload(file: File, sourceTrackId: String) {
        synchronized(downloadRegistryLock) {
            downloadPreferences.edit()
                .putStringSet(KEY_COMPLETED_DOWNLOADS, registeredDownloadPaths() + file.absolutePath)
                .putString(trackIdPreferenceKey(file), sourceTrackId)
                .apply()
        }
    }

    private fun unregisterDownload(file: File) {
        synchronized(downloadRegistryLock) {
            downloadPreferences.edit()
                .putStringSet(KEY_COMPLETED_DOWNLOADS, registeredDownloadPaths() - file.absolutePath)
                .remove(trackIdPreferenceKey(file))
                .apply()
        }
    }

    private fun finalizeAudioDownload(tempFile: File, requestedFile: File, sourceTrackId: String): File =
        synchronized(downloadRegistryLock) {
            var file = requestedFile
            while (file.exists() || file.absolutePath in registeredDownloadPaths()) {
                file = File(
                    requestedFile.parentFile,
                    "${requestedFile.nameWithoutExtension}-${System.nanoTime()}.${requestedFile.extension}"
                )
            }
            if (!tempFile.renameTo(file)) {
                throw IOException("Could not finalize downloaded audio file")
            }
            registerDownload(file, sourceTrackId)
            file
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
            val existingPaths = synchronized(downloadRegistryLock) {
                val registeredPaths = registeredDownloadPaths()
                val existing = registeredPaths.filter { File(it).isFile }.toSet()
                if (existing != registeredPaths) {
                    val editor = downloadPreferences.edit()
                        .putStringSet(KEY_COMPLETED_DOWNLOADS, existing)
                    (registeredPaths - existing).forEach { stalePath ->
                        editor.remove(KEY_TRACK_ID_PREFIX + stalePath)
                    }
                    editor.apply()
                }
                existing
            }

            val ids = mutableSetOf<String>()
            val tracks = mutableListOf<Track>()
            val retriever = MediaMetadataRetriever()

            try {
                for (path in existingPaths) {
                    val file = File(path)
                    val baseName = file.nameWithoutExtension
                    val parent = file.parentFile ?: continue
                    ids.add(baseName)

                    val artFile = File(parent, "$baseName.jpg").let {
                        if (it.exists() && it.length() > 0) it else File(parent, "$baseName.png")
                    }
                    val sourceTrackId = downloadPreferences.getString(trackIdPreferenceKey(file), null)
                    if (!sourceTrackId.isNullOrBlank()) ids.add(sourceTrackId)
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

                        if (!artFile.exists() || artFile.length() == 0L) {
                            retriever.embeddedPicture?.takeIf { it.isNotEmpty() }?.let { embedded ->
                                FileOutputStream(File(parent, "$baseName.jpg")).use { it.write(embedded) }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not read metadata for ${file.name}", e)
                        if (baseName.contains(" - ")) {
                            val parts = baseName.split(" - ", limit = 2)
                            title = parts[0].trim()
                            artist = parts[1].trim()
                        }
                    }

                    val finalArt = File(parent, "$baseName.jpg").let {
                        if (it.exists() && it.length() > 0) it else File(parent, "$baseName.png")
                    }
                    val artworkPath = if (finalArt.exists() && finalArt.length() > 0) {
                        "file://${finalArt.absolutePath}"
                    } else {
                        null
                    }
                    tracks.add(
                        Track(
                            id = "download_${file.name}",
                            title = title,
                            artist = artist,
                            durationSeconds = durationMs / 1000L,
                            artworkUrl = artworkPath,
                            streamUrl = file.absolutePath,
                            isLocal = true,
                            localUri = file.absolutePath
                        )
                    )
                }
            } finally {
                retriever.release()
            }

            val deduplicatedTracks = tracks.distinctBy {
                "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}"
            }
            val deduplicatedIds = deduplicatedTracks.map { it.id }.toSet() +
                    deduplicatedTracks.map { it.title.trim().lowercase() }.toSet() +
                    deduplicatedTracks.mapNotNull { track ->
                        track.streamUrl?.let { File(it).nameWithoutExtension }
                    }.toSet()

            _downloadedIds.value = deduplicatedIds
            _downloadedTracks.value = deduplicatedTracks
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        return _downloadedIds.value.contains(trackId)
    }

    /**
     * Downloads genuine audio stream + maximum resolution thumbnail directly
     * to device internal storage (/Music/MasterWave/).
     */
    fun downloadTrack(track: Track) {
        if (isDownloaded(track.id) || !activeDownloadIds.add(track.id)) return

        scope.launch {
            var temporaryAudioFile: File? = null
            var lastNotifiedPercent = -1

            fun updateProgress(progress: Float, indeterminate: Boolean = false) {
                val boundedProgress = progress.coerceIn(0f, 1f)
                _downloadProgress.value = _downloadProgress.value + (track.id to boundedProgress)
                val percent = (boundedProgress * 100).toInt()
                if (percent != lastNotifiedPercent) {
                    showDownloadProgress(track, percent, indeterminate)
                    lastNotifiedPercent = percent
                }
            }

            try {
                updateProgress(0f, indeterminate = true)
                val streamUrl = repository.resolveStreamUrl(track)
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Audio request failed with HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("Audio response was empty")
                    val contentLength = body.contentLength()
                    val ext = if (track.codec.contains("AAC", ignoreCase = true)) "m4a" else "opus"
                    val safeTitle = sanitizeFilename(track.title).ifBlank { "Track" }
                    val safeArtist = sanitizeFilename(track.artist)
                    val baseName = if (safeArtist.isNotBlank() && safeArtist != "Unknown") {
                        "$safeTitle - $safeArtist"
                    } else {
                        safeTitle
                    }
                    val requestedAudioFile = File(musicDir, "$baseName.$ext")
                    val tempFile = File(musicDir, "${requestedAudioFile.name}.${System.nanoTime()}.part")
                    temporaryAudioFile = tempFile
                    var totalRead = 0L
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).buffered().use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                val progress = if (contentLength > 0) {
                                    totalRead.toFloat() / contentLength
                                } else {
                                    0f
                                }
                                updateProgress(progress, indeterminate = contentLength <= 0)
                            }
                        }
                    }
                    if (totalRead == 0L) {
                        throw IOException("Audio response contained no data")
                    }
                    val audioFile = finalizeAudioDownload(tempFile, requestedAudioFile, track.id)
                    temporaryAudioFile = null
                    val artworkFile = File(musicDir, "${audioFile.nameWithoutExtension}.jpg")

                    val highResUrl = getHighResArtworkUrl(track.artworkUrl) ?: track.artworkUrl
                    if (!highResUrl.isNullOrBlank()) {
                        try {
                            val artRequest = Request.Builder().url(highResUrl).build()
                            okHttpClient.newCall(artRequest).execute().use { artResponse ->
                                if (artResponse.isSuccessful) {
                                    artResponse.body?.byteStream()?.use { artInput ->
                                        FileOutputStream(artworkFile).use { artOutput ->
                                            artInput.copyTo(artOutput)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not download artwork for ${track.title}", e)
                        }
                    }

                    try {
                        MediaScannerConnection.scanFile(
                            context,
                            if (artworkFile.exists()) {
                                arrayOf(audioFile.absolutePath, artworkFile.absolutePath)
                            } else {
                                arrayOf(audioFile.absolutePath)
                            },
                            if (artworkFile.exists()) arrayOf("audio/*", "image/jpeg") else arrayOf("audio/*")
                        ) { _, _ -> }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not index downloaded audio ${audioFile.name}", e)
                    }

                    val downloadedTrack = track.copy(
                        id = "download_${audioFile.name}",
                        streamUrl = audioFile.absolutePath,
                        artworkUrl = if (artworkFile.exists()) {
                            "file://${artworkFile.absolutePath}"
                        } else {
                            track.artworkUrl
                        },
                        isLocal = true,
                        localUri = audioFile.absolutePath
                    )
                    _downloadedIds.value = _downloadedIds.value +
                        setOf(track.id, audioFile.nameWithoutExtension, downloadedTrack.id)
                    _downloadedTracks.value = (_downloadedTracks.value + downloadedTrack).distinctBy {
                        "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}"
                    }
                    updateProgress(1f)
                    showDownloadComplete(track)

                    try {
                        withContext(Dispatchers.Main) {
                            repository.addCustomPlaylist(
                                repository.playlists.value.find { it.id == "local_songs" }?.let { playlist ->
                                    playlist.copy(
                                        trackCount = playlist.trackCount + 1,
                                        tracks = playlist.tracks + downloadedTrack
                                    )
                                } ?: return@withContext
                            )
                            repository.loadLocalTracks()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Downloaded track could not be added to the local playlist", e)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Download failed for ${track.title}", e)
                temporaryAudioFile?.delete()
                showDownloadFailure(track)
            } finally {
                _downloadProgress.value = _downloadProgress.value - track.id
                activeDownloadIds.remove(track.id)
            }
        }
    }

    private fun showDownloadProgress(track: Track, percent: Int, indeterminate: Boolean) {
        postDownloadNotification(
            track,
            NotificationCompat.Builder(context, MasterWaveApp.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.download_notification_progress_title, track.title))
                .setContentText(context.getString(R.string.download_notification_progress_text, percent, track.artist))
                .setProgress(100, percent, indeterminate)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        )
    }

    private fun showDownloadComplete(track: Track) {
        postDownloadNotification(
            track,
            NotificationCompat.Builder(context, MasterWaveApp.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.download_notification_complete_title))
                .setContentText(context.getString(R.string.download_notification_complete_text, track.title))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
        )
    }

    private fun showDownloadFailure(track: Track) {
        postDownloadNotification(
            track,
            NotificationCompat.Builder(context, MasterWaveApp.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.download_notification_failed_title))
                .setContentText(context.getString(R.string.download_notification_failed_text, track.title))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
        )
    }

    private fun postDownloadNotification(track: Track, notification: android.app.Notification) {
        if (!notificationManager.areNotificationsEnabled()) {
            if (!notificationPermissionWarningShown) {
                Log.w(TAG, "Notifications are disabled; download status is not visible")
                notificationPermissionWarningShown = true
            }
            return
        }
        try {
            notificationManager.notify(downloadNotificationId(track.id), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission is unavailable for download ${track.title}", e)
        }
    }

    private fun downloadNotificationId(trackId: String): Int =
        DOWNLOAD_NOTIFICATION_ID_BASE + (trackId.hashCode() and 0x0FFFFFFF)

    fun deleteDownload(track: Track) {
        scope.launch {
            try {
                val stream = track.streamUrl ?: return@launch
                val file = File(stream)
                if (file.exists() && !file.delete()) {
                    throw IOException("Could not delete downloaded file ${file.name}")
                }
                val sourceTrackId = downloadPreferences.getString(trackIdPreferenceKey(file), null)
                val artFile = File(stream.substringBeforeLast(".") + ".jpg")
                if (artFile.exists()) {
                    if (!artFile.delete()) Log.w(TAG, "Could not delete artwork for ${file.name}")
                }
                unregisterDownload(file)
                _downloadedIds.value = _downloadedIds.value - setOfNotNull(
                    track.id,
                    sourceTrackId,
                    file.nameWithoutExtension,
                    file.nameWithoutExtension.lowercase()
                )
                _downloadedTracks.value = _downloadedTracks.value.filterNot {
                    it.streamUrl == stream
                }
                scanDownloadedFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Could not delete downloaded track ${track.title}", e)
            }
        }
    }

    companion object {
        private const val TAG = "MusicDownloadManager"
        private const val DOWNLOADS_PREFERENCES = "masterwave_downloads"
        private const val KEY_COMPLETED_DOWNLOADS = "completed_download_paths"
        private const val KEY_TRACK_ID_PREFIX = "source_track_id:"
        private const val DOWNLOAD_NOTIFICATION_ID_BASE = 10_000

        private fun trackIdPreferenceKey(file: File) = KEY_TRACK_ID_PREFIX + file.absolutePath

        @Volatile
        private var INSTANCE: MusicDownloadManager? = null

        fun getInstance(context: Context, okHttpClient: OkHttpClient, repository: MusicRepository): MusicDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicDownloadManager(context.applicationContext, okHttpClient, repository).also { INSTANCE = it }
            }
        }
    }
}
