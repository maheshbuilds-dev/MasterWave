package com.masterwave.player.data.recommendation.engine

import android.content.Context
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.provider.YouTubeMusicProvider
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * High-level Recommendation Coordinator powering Home, Queue, and Now Playing.
 */
class RecommendationManager private constructor(
    context: Context,
    private val ytProvider: YouTubeMusicProvider,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val tasteProfileStore = TasteProfileStore(context)
    val eventTracker = ListeningEventTracker(tasteProfileStore, scope)
    private val candidateGenerator = CandidateGenerator(ytProvider, repository)
    private val ranker = PersonalizedRanker()

    val tasteProfile: StateFlow<TasteProfile> = tasteProfileStore.tasteProfile

    /**
     * "Made For You": Hybrid recommendation shelf tailored to the user's decaying taste profile.
     */
    suspend fun getMadeForYou(limit: Int = 12): List<Track> = withContext(Dispatchers.IO) {
        val profile = tasteProfile.value
        val topArtists = profile.artistAffinities.entries
            .sortedByDescending { it.value.getDecayedScore() }
            .map { it.key }
            .take(4)

        val artistCandidates = candidateGenerator.getCandidatesFromTopArtists(topArtists)
        val discoveryCandidates = candidateGenerator.getDiscoveryCandidates()
        val allCandidates = (artistCandidates + discoveryCandidates + repository.favorites.value).distinctBy { it.id }

        ranker.rankCandidates(allCandidates, profile, maxResults = limit, maxPerArtist = 2)
    }

    /**
     * "Daily Mix": Blend of established favorites with closely-related candidates.
     */
    suspend fun getDailyMix(limit: Int = 15): List<Track> = withContext(Dispatchers.IO) {
        val profile = tasteProfile.value
        val favorites = repository.favorites.value
        val recents = repository.recentTracks.value

        val seedArtist = favorites.firstOrNull()?.artist ?: recents.firstOrNull()?.artist ?: "Sai Abhyankkar"
        val relatedCandidates = candidateGenerator.getCandidatesForSeed(
            favorites.firstOrNull() ?: Track(id = "seed", title = "Hit", artist = seedArtist, durationSeconds = 180),
            limit = 20
        )

        val pool = (favorites.shuffled().take(6) + relatedCandidates).distinctBy { it.id }
        ranker.rankCandidates(pool, profile, maxResults = limit, maxPerArtist = 3)
    }

    /**
     * "Because You Listened To [Artist]": Contextual shelf tied to a specific artist.
     */
    suspend fun getBecauseYouListenedTo(seedTrack: Track, limit: Int = 8): Pair<String, List<Track>> = withContext(Dispatchers.IO) {
        val candidates = candidateGenerator.getCandidatesForSeed(seedTrack, limit = 16)
        val ranked = ranker.rankCandidates(candidates, tasteProfile.value, seedTrack = seedTrack, maxResults = limit, maxPerArtist = 3)
        Pair("Because you like ${seedTrack.artist}", ranked)
    }

    /**
     * "Up Next Recommendations": Tailored queue extensions with high relevance to the active track.
     */
    suspend fun getQueueRecommendations(currentTrack: Track, limit: Int = 8): List<Track> = withContext(Dispatchers.IO) {
        val candidates = candidateGenerator.getCandidatesForSeed(currentTrack, limit = 15)
        ranker.rankCandidates(candidates, tasteProfile.value, seedTrack = currentTrack, maxResults = limit, maxPerArtist = 2)
    }

    /**
     * "Rediscover": Tracks user previously loved that haven't been played in > 7 days.
     */
    fun getRediscoverTracks(limit: Int = 10): List<Track> {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
        val recentIds = tasteProfile.value.recentTrackIds.toSet()

        return repository.favorites.value
            .filter { !recentIds.contains(it.id) }
            .take(limit)
    }

    // --- User Feedback API ---
    fun onUserLikedTrack(track: Track) {
        eventTracker.onTrackLiked(track)
    }

    fun onUserUnlikedTrack(track: Track) {
        eventTracker.onTrackUnliked(track)
    }

    fun onMoreLikeThis(track: Track) {
        eventTracker.onTrackLiked(track)
    }

    fun onNotInterested(track: Track) {
        eventTracker.onHideTrack(track)
    }

    fun onDontRecommendArtist(artistName: String) {
        eventTracker.onDislikeArtist(artistName)
    }

    fun onHideTrack(track: Track) {
        eventTracker.onHideTrack(track)
    }

    companion object {
        @Volatile
        private var instance: RecommendationManager? = null

        fun getInstance(
            context: Context,
            ytProvider: YouTubeMusicProvider,
            repository: MusicRepository
        ): RecommendationManager {
            return instance ?: synchronized(this) {
                instance ?: RecommendationManager(
                    context.applicationContext,
                    ytProvider,
                    repository
                ).also { instance = it }
            }
        }
    }
}
