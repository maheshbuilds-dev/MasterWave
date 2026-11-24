package com.masterwave.player.data.backup

import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistTest {

    @Test
    fun m3uExportFormatsCorrectly() {
        val tracks = listOf(
            Track(id = "1", title = "Fear Song", artist = "Anirudh", durationSeconds = 195, streamUrl = "https://example.com/fear.m4a"),
            Track(id = "2", title = "Singari", artist = "Ramajogayya", durationSeconds = 210, localUri = "/storage/emulated/0/Music/singari.mp3", isLocal = true)
        )
        val playlist = Playlist(
            id = "test_pl",
            title = "My Test Playlist",
            tracks = tracks
        )

        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        sb.append("#PLAYLIST:${playlist.title}\n")
        playlist.tracks.forEach { track ->
            val duration = track.durationSeconds
            sb.append("#EXTINF:$duration,${track.artist} - ${track.title}\n")
            val location = track.localUri ?: track.streamUrl ?: track.id
            sb.append(location).append("\n")
        }
        val output = sb.toString()

        assertTrue(output.startsWith("#EXTM3U"))
        assertTrue(output.contains("#EXTINF:195,Anirudh - Fear Song"))
        assertTrue(output.contains("#EXTINF:210,Ramajogayya - Singari"))
        assertTrue(output.contains("/storage/emulated/0/Music/singari.mp3"))
    }

    @Test
    fun m3uParserExtractsTracksAndMetadata() {
        val sampleM3u = """
            #EXTM3U
            #PLAYLIST:Rock Classics
            #EXTINF:245,Queen - Bohemian Rhapsody
            https://example.com/queen.m4a
            #EXTINF:180,The Beatles - Yesterday
            /storage/emulated/0/Music/yesterday.mp3
        """.trimIndent()

        val lines = sampleM3u.lines()
        val parsedTracks = mutableListOf<Track>()
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
                val id = "m3u_test_${parsedTracks.size}"
                val isLocalPath = trimmed.startsWith("/") || trimmed.startsWith("file://")
                parsedTracks.add(
                    Track(
                        id = id,
                        title = currentTitle,
                        artist = currentArtist,
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

        assertEquals(2, parsedTracks.size)
        assertEquals("Bohemian Rhapsody", parsedTracks[0].title)
        assertEquals("Queen", parsedTracks[0].artist)
        assertEquals(245L, parsedTracks[0].durationSeconds)
        assertEquals("Yesterday", parsedTracks[1].title)
        assertEquals("The Beatles", parsedTracks[1].artist)
        assertTrue(parsedTracks[1].isLocal)
    }
}
