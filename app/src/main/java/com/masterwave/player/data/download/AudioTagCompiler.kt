package com.masterwave.player.data.download

import android.util.Log
import com.masterwave.player.data.model.Track
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.AndroidArtwork
import org.jaudiotagger.tag.reference.PictureTypes
import java.io.File

/**
 * Compiles and physically embeds track metadata and cover artwork
 * directly into downloaded audio containers (M4A/MP4, MP3, FLAC, Ogg/Opus).
 *
 * This ensures that external music players (Poweramp, Samsung Music, Apple Music,
 * Musicolet, VLC), Android lockscreen/notification media sessions, and desktop
 * operating systems permanently display the embedded artwork and tags.
 */
object AudioTagCompiler {

    private const val TAG = "AudioTagCompiler"

    fun embedMetadataAndArtwork(
        audioFile: File,
        track: Track,
        artworkBytes: ByteArray?
    ): Boolean {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            Log.w(TAG, "Audio file does not exist or is empty: ${audioFile.absolutePath}")
            return false
        }

        return try {
            val audio = AudioFileIO.read(audioFile)
            var tag = audio.tag
            if (tag == null) {
                tag = audio.createDefaultTag()
                audio.tag = tag
            }

            // Set Title & Artist
            val cleanTitle = track.title.trim()
            val cleanArtist = track.artist.trim()
            if (cleanTitle.isNotBlank()) {
                tag.setField(FieldKey.TITLE, cleanTitle)
            }
            if (cleanArtist.isNotBlank() && cleanArtist != "Unknown") {
                tag.setField(FieldKey.ARTIST, cleanArtist)
            }
            if (!track.album.isNullOrBlank()) {
                tag.setField(FieldKey.ALBUM, track.album.trim())
            }

            // Embed artwork inside the audio file container
            if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                try {
                    val artwork = AndroidArtwork()
                    artwork.binaryData = artworkBytes
                    artwork.mimeType = "image/jpeg"
                    artwork.pictureType = PictureTypes.DEFAULT_ID // 3: Front Cover
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                    Log.d(TAG, "Attached ${artworkBytes.size} bytes cover art to ${audioFile.name}")
                } catch (artEx: Throwable) {
                    Log.w(TAG, "Failed to attach artwork field to ${audioFile.name}", artEx)
                }
            }

            audio.commit()
            Log.i(TAG, "Successfully compiled tags and artwork into ${audioFile.name}")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Could not compile audio tags for ${audioFile.name}", e)
            false
        }
    }

    /**
     * Feature 8: Music File Manager & ID3 Editor
     * Updates metadata tags of local or downloaded audio containers.
     */
    fun updateTags(
        audioFile: File,
        title: String,
        artist: String,
        album: String,
        year: String? = null,
        genre: String? = null
    ): Boolean {
        if (!audioFile.exists() || !audioFile.canWrite()) {
            Log.w(TAG, "File cannot be written: ${audioFile.absolutePath}")
            return false
        }
        return try {
            val audio = AudioFileIO.read(audioFile)
            var tag = audio.tag
            if (tag == null) {
                tag = audio.createDefaultTag()
                audio.tag = tag
            }
            if (title.isNotBlank()) tag.setField(FieldKey.TITLE, title.trim())
            if (artist.isNotBlank()) tag.setField(FieldKey.ARTIST, artist.trim())
            if (album.isNotBlank()) tag.setField(FieldKey.ALBUM, album.trim())
            if (!year.isNullOrBlank()) tag.setField(FieldKey.YEAR, year.trim())
            if (!genre.isNullOrBlank()) tag.setField(FieldKey.GENRE, genre.trim())
            audio.commit()
            Log.i(TAG, "Updated ID3 tags for ${audioFile.name}")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed updating tags for ${audioFile.name}", e)
            false
        }
    }
}
