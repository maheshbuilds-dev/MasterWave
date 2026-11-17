package com.masterwave.player.data.recommendation.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Thread-safe persistent storage and updates for User Taste Profile.
 */
class TasteProfileStore(private val context: Context) {

    private val mutex = Mutex()
    private val profileFile: File by lazy {
        File(context.filesDir, "masterwave_taste_profile.json")
    }

    private val _tasteProfile = MutableStateFlow(loadFromDisk())
    val tasteProfile: StateFlow<TasteProfile> = _tasteProfile.asStateFlow()

    init {
        // If cold start, seed baseline popular artists
        if (_tasteProfile.value.artistAffinities.isEmpty()) {
            seedColdStartProfile()
        }
    }

    private fun seedColdStartProfile() {
        val defaultArtists = listOf(
            "Sai Abhyankkar", "Anirudh Ravichander", "Arijit Singh",
            "Sid Sriram", "A.R. Rahman", "Pritam", "Shreya Ghoshal", "The Weeknd"
        )
        val initialAffinities = defaultArtists.mapIndexed { idx, name ->
            name to ArtistAffinity(
                artistName = name,
                playCount = 1,
                rawScore = (defaultArtists.size - idx).toFloat() * 1.5f
            )
        }.toMap()

        val defaultLangs = mapOf(
            "Tamil" to 2.5f,
            "Hindi" to 2.5f,
            "English" to 2.0f,
            "Telugu" to 1.5f
        )

        val seeded = TasteProfile(
            artistAffinities = initialAffinities,
            languageAffinities = defaultLangs
        )
        _tasteProfile.value = seeded
        saveToDisk(seeded)
    }

    suspend fun recordEvent(event: ListeningEvent) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val current = _tasteProfile.value
            val now = System.currentTimeMillis()

            // Dislike artist or hide track check
            if (event.eventType == ListeningEventType.DISLIKE_ARTIST) {
                val updatedDislikedArtists = current.dislikedArtists + event.artist
                val updatedAffinities = current.artistAffinities.toMutableMap().apply { remove(event.artist) }
                val newProfile = current.copy(
                    dislikedArtists = updatedDislikedArtists,
                    artistAffinities = updatedAffinities,
                    lastUpdatedEpochMs = now
                )
                _tasteProfile.value = newProfile
                saveToDisk(newProfile)
                return@withContext
            }

            if (event.eventType == ListeningEventType.HIDE_TRACK) {
                val updatedDislikedTracks = current.dislikedTracks + event.trackId
                val updatedTrackAffinities = current.trackAffinities.toMutableMap().apply { remove(event.trackId) }
                val newProfile = current.copy(
                    dislikedTracks = updatedDislikedTracks,
                    trackAffinities = updatedTrackAffinities,
                    lastUpdatedEpochMs = now
                )
                _tasteProfile.value = newProfile
                saveToDisk(newProfile)
                return@withContext
            }

            // Update Artist Affinity
            val artistMap = current.artistAffinities.toMutableMap()
            val existingArtist = artistMap[event.artist] ?: ArtistAffinity(artistName = event.artist)
            val eventWeight = event.eventType.weight

            val newPlayCount = if (event.eventType == ListeningEventType.SONG_START) existingArtist.playCount + 1 else existingArtist.playCount
            val newSkipCount = if (event.eventType == ListeningEventType.EARLY_SKIP) existingArtist.skipCount + 1 else existingArtist.skipCount
            val newCompCount = if (event.eventType == ListeningEventType.SONG_COMPLETE) existingArtist.completionCount + 1 else existingArtist.completionCount

            val updatedArtist = existingArtist.copy(
                playCount = newPlayCount,
                skipCount = newSkipCount,
                completionCount = newCompCount,
                lastPlayedEpochMs = now,
                rawScore = (existingArtist.rawScore + eventWeight).coerceAtLeast(0f)
            )
            artistMap[event.artist] = updatedArtist

            // Update Track Affinity
            val trackMap = current.trackAffinities.toMutableMap()
            val existingTrack = trackMap[event.trackId] ?: TrackAffinity(
                trackId = event.trackId,
                title = event.title,
                artist = event.artist,
                album = event.album
            )
            val updatedTrack = existingTrack.copy(
                playCount = if (event.eventType == ListeningEventType.SONG_START) existingTrack.playCount + 1 else existingTrack.playCount,
                skipCount = if (event.eventType == ListeningEventType.EARLY_SKIP) existingTrack.skipCount + 1 else existingTrack.skipCount,
                completionCount = if (event.eventType == ListeningEventType.SONG_COMPLETE) existingTrack.completionCount + 1 else existingTrack.completionCount,
                lastPlayedEpochMs = now,
                rawScore = (existingTrack.rawScore + eventWeight).coerceAtLeast(0f),
                isFavorite = if (event.eventType == ListeningEventType.LIKE) true else if (event.eventType == ListeningEventType.UNLIKE) false else existingTrack.isFavorite
            )
            trackMap[event.trackId] = updatedTrack

            // Update Recent Track History
            val recents = current.recentTrackIds.toMutableList()
            recents.remove(event.trackId)
            recents.add(0, event.trackId)
            val trimmedRecents = recents.take(50)

            val newProfile = current.copy(
                artistAffinities = artistMap,
                trackAffinities = trackMap,
                recentTrackIds = trimmedRecents,
                totalListeningSeconds = current.totalListeningSeconds + event.listenedSeconds,
                lastUpdatedEpochMs = now
            )

            _tasteProfile.value = newProfile
            saveToDisk(newProfile)
        }
    }

    private fun saveToDisk(profile: TasteProfile) {
        try {
            val root = JSONObject()
            root.put("lastUpdatedEpochMs", profile.lastUpdatedEpochMs)
            root.put("totalListeningSeconds", profile.totalListeningSeconds)

            // Artists
            val artistsObj = JSONObject()
            profile.artistAffinities.forEach { (name, aff) ->
                val o = JSONObject().apply {
                    put("playCount", aff.playCount)
                    put("skipCount", aff.skipCount)
                    put("completionCount", aff.completionCount)
                    put("lastPlayedEpochMs", aff.lastPlayedEpochMs)
                    put("rawScore", aff.rawScore.toDouble())
                }
                artistsObj.put(name, o)
            }
            root.put("artistAffinities", artistsObj)

            // Tracks
            val tracksObj = JSONObject()
            profile.trackAffinities.forEach { (id, aff) ->
                val o = JSONObject().apply {
                    put("title", aff.title)
                    put("artist", aff.artist)
                    put("album", aff.album)
                    put("artworkUrl", aff.artworkUrl ?: "")
                    put("playCount", aff.playCount)
                    put("skipCount", aff.skipCount)
                    put("completionCount", aff.completionCount)
                    put("lastPlayedEpochMs", aff.lastPlayedEpochMs)
                    put("rawScore", aff.rawScore.toDouble())
                    put("isFavorite", aff.isFavorite)
                }
                tracksObj.put(id, o)
            }
            root.put("trackAffinities", tracksObj)

            // Disliked
            val dislikedArtistsArr = JSONArray(profile.dislikedArtists)
            root.put("dislikedArtists", dislikedArtistsArr)

            val dislikedTracksArr = JSONArray(profile.dislikedTracks)
            root.put("dislikedTracks", dislikedTracksArr)

            val recentsArr = JSONArray(profile.recentTrackIds)
            root.put("recentTrackIds", recentsArr)

            profileFile.writeText(root.toString())
        } catch (_: Exception) {}
    }

    private fun loadFromDisk(): TasteProfile {
        return try {
            if (!profileFile.exists()) return TasteProfile()
            val text = profileFile.readText()
            if (text.isBlank()) return TasteProfile()

            val root = JSONObject(text)
            val artists = mutableMapOf<String, ArtistAffinity>()
            val artistsObj = root.optJSONObject("artistAffinities")
            if (artistsObj != null) {
                val keys = artistsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val o = artistsObj.getJSONObject(k)
                    artists[k] = ArtistAffinity(
                        artistName = k,
                        playCount = o.optInt("playCount"),
                        skipCount = o.optInt("skipCount"),
                        completionCount = o.optInt("completionCount"),
                        lastPlayedEpochMs = o.optLong("lastPlayedEpochMs"),
                        rawScore = o.optDouble("rawScore", 0.0).toFloat()
                    )
                }
            }

            val tracks = mutableMapOf<String, TrackAffinity>()
            val tracksObj = root.optJSONObject("trackAffinities")
            if (tracksObj != null) {
                val keys = tracksObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val o = tracksObj.getJSONObject(k)
                    tracks[k] = TrackAffinity(
                        trackId = k,
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        album = o.optString("album"),
                        artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() },
                        playCount = o.optInt("playCount"),
                        skipCount = o.optInt("skipCount"),
                        completionCount = o.optInt("completionCount"),
                        lastPlayedEpochMs = o.optLong("lastPlayedEpochMs"),
                        rawScore = o.optDouble("rawScore", 0.0).toFloat(),
                        isFavorite = o.optBoolean("isFavorite", false)
                    )
                }
            }

            val dislikedArtists = mutableSetOf<String>()
            val da = root.optJSONArray("dislikedArtists")
            if (da != null) {
                for (i in 0 until da.length()) dislikedArtists.add(da.getString(i))
            }

            val dislikedTracks = mutableSetOf<String>()
            val dt = root.optJSONArray("dislikedTracks")
            if (dt != null) {
                for (i in 0 until dt.length()) dislikedTracks.add(dt.getString(i))
            }

            val recents = mutableListOf<String>()
            val rc = root.optJSONArray("recentTrackIds")
            if (rc != null) {
                for (i in 0 until rc.length()) recents.add(rc.getString(i))
            }

            TasteProfile(
                artistAffinities = artists,
                trackAffinities = tracks,
                dislikedArtists = dislikedArtists,
                dislikedTracks = dislikedTracks,
                recentTrackIds = recents,
                totalListeningSeconds = root.optLong("totalListeningSeconds"),
                lastUpdatedEpochMs = root.optLong("lastUpdatedEpochMs", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            TasteProfile()
        }
    }
}
