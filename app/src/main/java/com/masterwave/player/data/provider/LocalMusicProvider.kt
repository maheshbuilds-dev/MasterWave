package com.masterwave.player.data.provider

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.masterwave.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalMusicProvider(private val context: Context) {

    suspend fun loadLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            val coversDir = File(context.filesDir, "local_covers").apply { mkdirs() }

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val durationMs = it.getLong(durationCol)
                    val albumId = it.getLong(albumIdCol)
                    val filePath = it.getString(dataCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val coverFile = File(coversDir, "local_$id.jpg")
                    var artworkUri: String? = null

                    if (coverFile.exists() && coverFile.length() > 0) {
                        artworkUri = "file://${coverFile.absolutePath}"
                    } else {
                        // 1. Try MediaStore albumart URI
                        val msArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                        val msSuccess = runCatching {
                            context.contentResolver.openInputStream(msArtUri)?.use { input ->
                                coverFile.outputStream().use { out -> input.copyTo(out) }
                                true
                            } ?: false
                        }.getOrDefault(false)

                        if (msSuccess && coverFile.exists() && coverFile.length() > 0) {
                            artworkUri = "file://${coverFile.absolutePath}"
                        } else {
                            // 2. Extract embedded picture with MediaMetadataRetriever
                            try {
                                val retriever = MediaMetadataRetriever()
                                if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                                    retriever.setDataSource(filePath)
                                } else {
                                    retriever.setDataSource(context, Uri.parse(contentUri))
                                }
                                val pic = retriever.embeddedPicture
                                retriever.release()
                                if (pic != null && pic.isNotEmpty()) {
                                    coverFile.writeBytes(pic)
                                    artworkUri = "file://${coverFile.absolutePath}"
                                }
                            } catch (_: Exception) {}
                        }

                        if (artworkUri == null) {
                            artworkUri = msArtUri.toString()
                        }
                    }

                    tracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationSeconds = durationMs / 1000L,
                            artworkUrl = artworkUri,
                            streamUrl = if (!filePath.isNullOrBlank() && File(filePath).exists()) filePath else contentUri,
                            isLocal = true,
                            localUri = if (!filePath.isNullOrBlank() && File(filePath).exists()) filePath else contentUri,
                            codec = "FLAC/MP3/M4A",
                            bitrateKbps = 320
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }
}
