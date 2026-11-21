package com.masterwave.player.data.download

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.R
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.CancellationException
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Public Internal Storage Music & Artwork Download Manager:
 * - Downloads genuine audio streams directly to /storage/emulated/0/Music/MasterWave/
 *   via Android MediaStore and public Environment storage.
 * - Downloads high-resolution thumbnail artwork (.jpg) alongside every single track.
 * - Embeds/associates artwork and indexes via Android MediaStore & MediaScannerConnection
 *   so system file managers and other music players display the attached thumbnail.
 * - Automatically migrates any legacy app-private downloads to public storage.
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

    /**
     * Primary Public Storage Directory:
     * Internal Storage -> /Music/MasterWave/
     */
    val musicDir: File
        get() {
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "MasterWave"
            )
            if (!publicDir.exists()) {
                try {
                    publicDir.mkdirs()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not mkdirs public music dir", e)
                }
            }
            return publicDir
        }

    init {
        migrateLegacyDownloads()
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

    /**
     * Move any previously saved downloads from app-private storage
     * to public internal storage (/storage/emulated/0/Music/MasterWave/).
     */
    private fun migrateLegacyDownloads() {
        scope.launch {
            try {
                val legacyDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MasterWave")
                if (legacyDir.exists() && legacyDir.isDirectory) {
                    val legacyFiles = legacyDir.listFiles().orEmpty()
                    for (file in legacyFiles) {
                        if (file.isFile && !file.name.endsWith(".part")) {
                            try {
                                val isAudio = file.extension.equals("m4a", true) ||
                                        file.extension.equals("opus", true) ||
                                        file.extension.equals("mp3", true)
                                if (isAudio) {
                                    insertAudioToMediaStore(file.nameWithoutExtension, file.extension, file)
                                }
                                file.delete()
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not migrate legacy file ${file.name}", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Legacy migration error", e)
            }
        }
    }

    fun scanDownloadedFiles() {
        scope.launch {
            val tracks = mutableListOf<Track>()
            val ids = mutableSetOf<String>()

            // 1. Query Android MediaStore for files under Music/MasterWave
            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                )

                val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Audio.Media.DATA} LIKE ?"
                } else {
                    "${MediaStore.Audio.Media.DATA} LIKE ?"
                }

                val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf("%Music/MasterWave%", "%/Music/MasterWave/%")
                } else {
                    arrayOf("%/Music/MasterWave/%")
                }

                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val mediaId = cursor.getLong(idCol)
                        val displayName = cursor.getString(nameCol) ?: "Track"
                        val title = cursor.getString(titleCol) ?: displayName.substringBeforeLast(".")
                        val artist = cursor.getString(artistCol) ?: "MasterWave"
                        val durationMs = cursor.getLong(durationCol)
                        val filePath = cursor.getString(dataCol)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)

                        val baseName = displayName.substringBeforeLast(".")
                        ids.add(baseName)
                        ids.add(displayName)

                        // Look for companion thumbnail in public musicDir or cache
                        val companionArt = File(musicDir, "$baseName.jpg").let {
                            if (it.exists() && it.length() > 0) it else File(context.cacheDir, "art_$baseName.jpg")
                        }

                        val artworkUrl = if (companionArt.exists() && companionArt.length() > 0) {
                            "file://${companionArt.absolutePath}"
                        } else {
                            findArtworkInMediaStore(baseName)
                        }

                        val playableUri = if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                            filePath
                        } else {
                            contentUri.toString()
                        }

                        tracks.add(
                            Track(
                                id = "download_$mediaId",
                                title = title,
                                artist = artist,
                                durationSeconds = durationMs / 1000L,
                                artworkUrl = artworkUrl,
                                streamUrl = playableUri,
                                isLocal = true,
                                localUri = playableUri
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore audio query failed", e)
            }

            // 2. Direct File System Scan Fallback
            try {
                val directFiles = musicDir.listFiles { file ->
                    file.isFile && (file.extension.equals("m4a", true) ||
                            file.extension.equals("opus", true) ||
                            file.extension.equals("mp3", true) ||
                            file.extension.equals("flac", true)) &&
                            !file.name.startsWith(".pending")
                }?.toList().orEmpty()

                for (file in directFiles) {
                    val baseName = file.nameWithoutExtension
                    ids.add(baseName)
                    ids.add(file.name)

                    if (tracks.none { it.streamUrl == file.absolutePath || it.title == baseName }) {
                        val companionArt = File(musicDir, "$baseName.jpg").let {
                            if (it.exists() && it.length() > 0) it else File(context.cacheDir, "art_$baseName.jpg")
                        }
                        val artworkUrl = if (companionArt.exists() && companionArt.length() > 0) {
                            "file://${companionArt.absolutePath}"
                        } else null

                        tracks.add(
                            Track(
                                id = "download_${file.name}",
                                title = if (baseName.contains(" - ")) baseName.split(" - ", limit = 2)[0] else baseName,
                                artist = if (baseName.contains(" - ")) baseName.split(" - ", limit = 2)[1] else "MasterWave",
                                durationSeconds = 0L,
                                artworkUrl = artworkUrl,
                                streamUrl = file.absolutePath,
                                isLocal = true,
                                localUri = file.absolutePath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct disk scan fallback error", e)
            }

            val deduplicatedTracks = tracks.distinctBy {
                "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}"
            }
            val deduplicatedIds = deduplicatedTracks.map { it.id }.toSet() +
                    deduplicatedTracks.map { it.title.trim().lowercase() }.toSet() +
                    deduplicatedTracks.mapNotNull { track ->
                        track.streamUrl?.let { File(it).nameWithoutExtension }
                    }.toSet() + ids

            _downloadedIds.value = deduplicatedIds
            _downloadedTracks.value = deduplicatedTracks
        }
    }

    private fun findArtworkInMediaStore(baseName: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("$baseName.jpg", "%Music/MasterWave%")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        return _downloadedIds.value.contains(trackId)
    }

    /**
     * Downloads genuine audio stream + maximum resolution thumbnail directly
     * to device public internal storage (/Music/MasterWave/).
     */
    fun downloadTrack(track: Track) {
        if (isDownloaded(track.id) || !activeDownloadIds.add(track.id)) return

        scope.launch {
            var tempAudioFile: File? = null
            var tempArtFile: File? = null
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
                val (streamUrl, preferredExt) = try {
                    repository.resolveDownloadStream(track)
                } catch (_: Exception) {
                    Pair(repository.resolveStreamUrl(track), if (track.codec.contains("AAC", ignoreCase = true)) "m4a" else "m4a")
                }
                val ext = preferredExt

                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val safeTitle = sanitizeFilename(track.title).ifBlank { "Track" }
                val safeArtist = sanitizeFilename(track.artist)
                val baseName = if (safeArtist.isNotBlank() && safeArtist != "Unknown") {
                    "$safeTitle - $safeArtist"
                } else {
                    safeTitle
                }

                // Stage 1: Download audio stream safely into cacheDir without EPERM
                val tempAudio = File(context.cacheDir, "dl_${System.nanoTime()}.$ext")
                tempAudioFile = tempAudio

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Audio stream request failed with HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("Audio response was empty")
                    val contentLength = body.contentLength()
                    var totalRead = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempAudio).buffered().use { output ->
                            val buffer = ByteArray(16384)
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
                                updateProgress(progress * 0.90f, indeterminate = contentLength <= 0)
                            }
                        }
                    }

                    if (totalRead == 0L) {
                        throw IOException("Zero bytes received for audio stream")
                    }
                }

                // Stage 2: Download high-resolution thumbnail artwork
                val highResUrl = getHighResArtworkUrl(track.artworkUrl) ?: track.artworkUrl
                var artworkBytes: ByteArray? = null
                if (!highResUrl.isNullOrBlank()) {
                    try {
                        val artRequest = Request.Builder().url(highResUrl).build()
                        okHttpClient.newCall(artRequest).execute().use { artResponse ->
                            if (artResponse.isSuccessful) {
                                artworkBytes = artResponse.body?.bytes()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not fetch artwork for ${track.title}", e)
                    }
                }

                // Stage 2.5: Physically compile & embed metadata and artwork into the audio container!
                AudioTagCompiler.embedMetadataAndArtwork(tempAudio, track, artworkBytes)

                // Stage 3: Insert Audio with permanently compiled artwork into Public Internal Storage (/Music/MasterWave/)
                val (audioUri, publicAudioPath) = insertAudioToMediaStore(baseName, ext, tempAudio)
                tempAudio.delete()
                tempAudioFile = null

                // Stage 4: Insert Artwork into Public Internal Storage (/Music/MasterWave/$baseName.jpg)
                var localArtworkUri: String? = null
                if (artworkBytes != null && artworkBytes!!.isNotEmpty()) {
                    val artFileInPublic = File(musicDir, "$baseName.jpg")
                    try {
                        FileOutputStream(artFileInPublic).use { it.write(artworkBytes) }
                        localArtworkUri = "file://${artFileInPublic.absolutePath}"
                    } catch (_: Exception) {}

                    // Also save companion copy in cache for instant local coil rendering
                    try {
                        val cachedArt = File(context.cacheDir, "art_$baseName.jpg")
                        FileOutputStream(cachedArt).use { it.write(artworkBytes) }
                        if (localArtworkUri == null) {
                            localArtworkUri = "file://${cachedArt.absolutePath}"
                        }
                    } catch (_: Exception) {}

                    // Insert to MediaStore Images
                    insertArtworkToMediaStore(baseName, artworkBytes!!)
                }

                // Stage 5: Scan with MediaScannerConnection for external file managers and system music player indexing
                val scanPaths = mutableListOf<String>()
                if (publicAudioPath != null) scanPaths.add(publicAudioPath)
                val artPath = File(musicDir, "$baseName.jpg")
                if (artPath.exists()) scanPaths.add(artPath.absolutePath)

                if (scanPaths.isNotEmpty()) {
                    MediaScannerConnection.scanFile(
                        context,
                        scanPaths.toTypedArray(),
                        arrayOf("audio/*", "image/jpeg")
                    ) { path, uri ->
                        Log.d(TAG, "Indexed $path -> $uri")
                    }
                }

                val finalStreamPath = publicAudioPath ?: audioUri.toString()
                val downloadedTrack = track.copy(
                    id = "download_${baseName}",
                    streamUrl = finalStreamPath,
                    artworkUrl = localArtworkUri ?: track.artworkUrl,
                    isLocal = true,
                    localUri = finalStreamPath
                )

                _downloadedIds.value = _downloadedIds.value + setOf(track.id, baseName, downloadedTrack.id)
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

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Download failed for ${track.title}", e)
                tempAudioFile?.delete()
                tempArtFile?.delete()
                showDownloadFailure(track)
            } finally {
                _downloadProgress.value = _downloadProgress.value - track.id
                activeDownloadIds.remove(track.id)
            }
        }
    }

    private fun insertAudioToMediaStore(baseName: String, ext: String, sourceFile: File): Pair<Uri, String?> {
        val mime = if (ext == "m4a") "audio/mp4" else "audio/ogg"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$baseName.$ext")
            put(MediaStore.Audio.Media.TITLE, if (baseName.contains(" - ")) baseName.split(" - ", limit = 2)[0] else baseName)
            put(MediaStore.Audio.Media.ARTIST, if (baseName.contains(" - ")) baseName.split(" - ", limit = 2)[1] else "MasterWave")
            put(MediaStore.Audio.Media.ALBUM, "MasterWave")
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MasterWave/")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not insert track into MediaStore")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(sourceFile).use { input -> input.copyTo(out) }
        } ?: throw IOException("Could not open output stream for audio MediaStore entry")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        // Query physical data path
        val path = try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        } ?: File(musicDir, "$baseName.$ext").absolutePath

        return Pair(uri, path)
    }

    private fun insertArtworkToMediaStore(baseName: String, imageBytes: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Music/MasterWave/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(imageBytes)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not insert thumbnail into MediaStore Images", e)
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

                // 1. If it's a content URI, delete via ContentResolver
                if (stream.startsWith("content://")) {
                    try {
                        context.contentResolver.delete(Uri.parse(stream), null, null)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not delete MediaStore URI $stream", e)
                    }
                }

                // 2. If it's a file path, delete file directly
                val file = File(stream)
                if (file.exists()) {
                    file.delete()
                }

                // 3. Delete companion artwork
                val baseName = file.nameWithoutExtension
                val artFile = File(musicDir, "$baseName.jpg")
                if (artFile.exists()) artFile.delete()

                val cachedArt = File(context.cacheDir, "art_$baseName.jpg")
                if (cachedArt.exists()) cachedArt.delete()

                _downloadedIds.value = _downloadedIds.value - setOfNotNull(
                    track.id,
                    baseName,
                    baseName.lowercase()
                )
                _downloadedTracks.value = _downloadedTracks.value.filterNot {
                    it.streamUrl == stream || it.id == track.id
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
        private const val DOWNLOAD_NOTIFICATION_ID_BASE = 10_000

        @Volatile
        private var INSTANCE: MusicDownloadManager? = null

        fun getInstance(context: Context, okHttpClient: OkHttpClient, repository: MusicRepository): MusicDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicDownloadManager(context.applicationContext, okHttpClient, repository).also { INSTANCE = it }
            }
        }
    }
}
