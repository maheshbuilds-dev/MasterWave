package com.masterwave.player.data.repository

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.model.TrackAvailability
import com.masterwave.player.data.provider.MusicSourceAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicCatalogRepositoryTest {

    private class FakeAdapter(
        override val providerId: String,
        override val displayName: String = providerId,
        override val isAuthorizedStream: Boolean = true,
        private val cannedTracks: List<Track> = emptyList(),
        private val streamUrlToReturn: String? = null
    ) : MusicSourceAdapter {
        override suspend fun search(query: String, limit: Int): List<Track> = cannedTracks
        override suspend fun getTrending(limit: Int): List<Track> = cannedTracks
        override suspend fun resolveStreamUrl(track: Track): String? = streamUrlToReturn
    }

    private fun createRepository(
        local: MusicSourceAdapter = FakeAdapter("local"),
        audius: MusicSourceAdapter = FakeAdapter("audius"),
        ia: MusicSourceAdapter = FakeAdapter("internet_archive"),
        jamendo: MusicSourceAdapter = FakeAdapter("jamendo"),
        yt: MusicSourceAdapter = FakeAdapter("youtube", isAuthorizedStream = false)
    ): MusicCatalogRepository {
        return MusicCatalogRepository(
            localAdapter = local,
            audiusAdapter = audius,
            internetArchiveAdapter = ia,
            jamendoAdapter = jamendo,
            youtubeAdapter = yt
        )
    }

    @Test
    fun testDeduplicationPrefersAuthorizedSources() {
        val ytTrack = Track(
            id = "yt_123",
            title = "Aane Wala Star (Official Audio)",
            artist = "Sai Abhyankkar",
            providerId = "youtube"
        )
        val audiusTrack = Track(
            id = "audius_456",
            title = "Aane Wala Star",
            artist = "Sai Abhyankkar",
            providerId = "audius"
        )
        val localTrack = Track(
            id = "local_789",
            title = "Aane Wala Star",
            artist = "Sai Abhyankkar",
            providerId = "local",
            isLocal = true,
            localUri = "/storage/music/aane_wala.mp3"
        )

        val repo = createRepository()

        // Pass in YouTube first, then Audius, then Local
        val deduplicated = repo.deduplicateTracks(listOf(ytTrack, audiusTrack, localTrack))

        assertEquals(1, deduplicated.size)
        // Local should win priority over audius and youtube
        assertEquals("local", deduplicated.first().providerId)
    }

    @Test
    fun testDeduplicationCleanTitles() {
        val t1 = Track(id = "1", title = "Kesariya [Lyric Video]", artist = "Arijit Singh", providerId = "youtube")
        val t2 = Track(id = "2", title = "Kesariya (From \"Brahmastra\")", artist = "Arijit Singh", providerId = "audius")

        val repo = createRepository()
        val result = repo.deduplicateTracks(listOf(t1, t2))
        assertEquals(1, result.size)
        assertEquals("audius", result.first().providerId)
    }

    @Test
    fun testMultiSourceSearchAggregation() = runBlocking {
        val audiusSong = Track(id = "aud_1", title = "Song A", artist = "Artist 1", providerId = "audius")
        val iaSong = Track(id = "ia_1", title = "Song B", artist = "Artist 2", providerId = "internet_archive")

        val repo = createRepository(
            audius = FakeAdapter("audius", cannedTracks = listOf(audiusSong)),
            ia = FakeAdapter("internet_archive", cannedTracks = listOf(iaSong))
        )

        val results = repo.searchAll("Song")
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == "aud_1" })
        assertTrue(results.any { it.id == "ia_1" })
    }

    @Test
    fun testStreamResolutionWithFallback() = runBlocking {
        val track = Track(id = "yt_test", title = "Unique Song", artist = "Unique Artist", providerId = "youtube")

        // Primary adapter returns null, but Audius adapter can resolve by search
        val resolvedTrackOnAudius = Track(id = "aud_match", title = "Unique Song", artist = "Unique Artist", providerId = "audius")
        val repo = createRepository(
            audius = FakeAdapter("audius", cannedTracks = listOf(resolvedTrackOnAudius), streamUrlToReturn = "https://stream.audius.co/123"),
            yt = FakeAdapter("youtube", streamUrlToReturn = null)
        )

        val streamUrl = repo.resolveStreamUrl(track)
        assertNotNull(streamUrl)
        assertEquals("https://stream.audius.co/123", streamUrl)
    }
}
