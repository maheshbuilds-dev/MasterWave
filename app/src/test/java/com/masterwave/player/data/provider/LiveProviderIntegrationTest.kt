package com.masterwave.player.data.provider

import com.masterwave.player.data.model.TrackAvailability
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class LiveProviderIntegrationTest {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Test
    fun testAudiusLiveSearchAndStreamResolution() = runBlocking {
        val adapter = AudiusSourceAdapter(okHttpClient)
        val tracks = adapter.search("chill", limit = 3)

        println("Audius live search returned ${tracks.size} tracks")
        assertTrue("Audius should return at least 1 track", tracks.isNotEmpty())

        val firstTrack = tracks.first()
        println("Testing track: ${firstTrack.title} by ${firstTrack.artist} (id: ${firstTrack.id})")
        assertEquals(TrackAvailability.FULL_TRACK, firstTrack.availability)
        assertEquals("audius", firstTrack.providerId)
        assertTrue(firstTrack.isPlayable)

        val streamUrl = adapter.resolveStreamUrl(firstTrack)
        println("Resolved stream URL: $streamUrl")
        assertNotNull(streamUrl)
        assertTrue(streamUrl!!.startsWith("https://") || streamUrl.startsWith("http://"))
    }

    @Test
    fun testInternetArchiveLiveSearchAndStreamResolution() = runBlocking {
        val adapter = InternetArchiveSourceAdapter(okHttpClient)
        val tracks = adapter.search("Hindustani", limit = 3)

        println("Internet Archive live search returned ${tracks.size} tracks")
        assertTrue("Internet Archive should return at least 1 track for 'Hindustani'", tracks.isNotEmpty())

        val firstTrack = tracks.first()
        println("Testing IA track: ${firstTrack.title} by ${firstTrack.artist} (id: ${firstTrack.id})")
        assertEquals(TrackAvailability.FULL_TRACK, firstTrack.availability)
        assertEquals("internet_archive", firstTrack.providerId)
        assertTrue(firstTrack.isPlayable)

        val streamUrl = adapter.resolveStreamUrl(firstTrack)
        println("Resolved IA stream URL: $streamUrl")
        // Note: Some archive items may be audio collections where files metadata resolves to direct MP3
        if (streamUrl != null) {
            assertTrue(streamUrl.contains("archive.org/download/"))
        }
    }
}
