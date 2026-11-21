package com.masterwave.player.data.repository

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.MusicSourceAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        override suspend fun search(query: String, limit: Int): List<Track> = cannedTracks.take(limit)
        override suspend fun getTrending(limit: Int): List<Track> = cannedTracks.take(limit)
        override suspend fun resolveStreamUrl(track: Track): String? = streamUrlToReturn
    }

    private fun createRepository(
        local: MusicSourceAdapter = FakeAdapter("local"),
        youtube: MusicSourceAdapter = FakeAdapter("youtube", isAuthorizedStream = false)
    ) = MusicCatalogRepository(localAdapter = local, youtubeAdapter = youtube)

    @Test
    fun prefersLocalFilesWhenDeduplicatingWithYouTubeResults() {
        val youtubeTrack = Track(
            id = "yt_123",
            title = "Aane Wala Star (Official Audio)",
            artist = "Sai Abhyankkar",
            providerId = "youtube"
        )
        val localTrack = Track(
            id = "local_789",
            title = "Aane Wala Star",
            artist = "Sai Abhyankkar",
            providerId = "local",
            isLocal = true,
            localUri = "/storage/music/aane_wala.mp3"
        )

        val result = createRepository().deduplicateTracks(listOf(youtubeTrack, localTrack))

        assertEquals(1, result.size)
        assertEquals("local", result.first().providerId)
    }

    @Test
    fun deduplicatesEquivalentYouTubeTitles() {
        val lyricVideo = Track(
            id = "yt_1",
            title = "Kesariya [Lyric Video]",
            artist = "Arijit Singh",
            providerId = "youtube"
        )
        val soundtrack = Track(
            id = "yt_2",
            title = "Kesariya (From \"Brahmastra\")",
            artist = "Arijit Singh",
            providerId = "youtube"
        )

        val result = createRepository().deduplicateTracks(listOf(lyricVideo, soundtrack))

        assertEquals(listOf("yt_1"), result.map { it.id })
    }

    @Test
    fun searchesOnlyLocalAndYouTubeAdapters() = runBlocking {
        val localTrack = Track(id = "local_1", title = "Local Song", artist = "Artist", providerId = "local")
        val youtubeTrack = Track(id = "yt_1", title = "Stream Song", artist = "Artist", providerId = "youtube")
        val repo = createRepository(
            local = FakeAdapter("local", cannedTracks = listOf(localTrack)),
            youtube = FakeAdapter("youtube", cannedTracks = listOf(youtubeTrack))
        )

        val results = repo.searchAll("song")

        assertEquals(setOf("local_1", "yt_1"), results.map { it.id }.toSet())
        assertTrue(repo.searchAll("song", filterProviderId = "audius").isEmpty())
    }

    @Test
    fun legacyRemovedProviderUrlsAreReplacedWithYouTubeResolution() = runBlocking {
        val legacyTrack = Track(
            id = "audius_123",
            title = "Unique Song",
            artist = "Unique Artist",
            providerId = "audius",
            streamUrl = "https://stream.audius.co/123"
        )
        val youtubeMatch = Track(
            id = "yt_match",
            title = "Unique Song",
            artist = "Unique Artist",
            providerId = "youtube"
        )
        val repo = createRepository(
            youtube = FakeAdapter(
                "youtube",
                cannedTracks = listOf(youtubeMatch),
                streamUrlToReturn = "https://music.youtube.com/stream"
            )
        )

        assertEquals(
            "https://music.youtube.com/stream",
            repo.resolveStreamUrl(legacyTrack)
        )
    }
}
