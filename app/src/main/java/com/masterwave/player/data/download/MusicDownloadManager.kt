package com.masterwave.player.data.download

import android.content.Context
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

    private val musicDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MasterWave")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    init {
        scanDownloadedFiles()
    }

    private fun scanDownloadedFiles() {
        val files = musicDir.listFiles { _, name -> name.endsWith(".opus") || name.endsWith(".m4a") || name.endsWith(".mp3") }
        val ids = files?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()
        _downloadedIds.value = ids
    }

    fun isDownloaded(trackId: String): Boolean {
        return _downloadedIds.value.contains(trackId)
    }

    /**
     * Silently downloads a track in the background without pop-up notifications.
     */
    fun downloadTrack(track: Track) {
        if (isDownloaded(track.id)) return

        scope.launch {
            try {
                _downloadProgress.value = _downloadProgress.value + (track.id to 0.05f)
                val streamUrl = repository.resolveStreamUrl(track)

                val request = Request.Builder().url(streamUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body ?: return@launch
                    val contentLength = body.contentLength().coerceAtLeast(1L)
                    val ext = if (track.codec.contains("AAC", ignoreCase = true)) "m4a" else "opus"
                    val outputFile = File(musicDir, "${track.id}.$ext")

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(outputFile)
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

                    // Mark as downloaded
                    _downloadedIds.value = _downloadedIds.value + track.id
                    _downloadProgress.value = _downloadProgress.value - track.id

                    // Refresh local tracks
                    withContext(Dispatchers.Main) {
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
