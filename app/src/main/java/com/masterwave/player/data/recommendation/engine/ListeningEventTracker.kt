package com.masterwave.player.data.recommendation.engine

import com.masterwave.player.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Real-time event analyzer observing player playback and user interactions.
 */
class ListeningEventTracker(
    private val tasteProfileStore: TasteProfileStore,
    private val scope: CoroutineScope
) {
    private var activeTrack: Track? = null
    private var trackStartEpochMs: Long = 0L
    private var maxPositionMs: Long = 0L
    private var trackDurationMs: Long = 0L
    private var lastCompletedTrackId: String? = null
    private var lastCompletedEpochMs: Long = 0L

    fun onTrackStarted(track: Track) {
        val now = System.currentTimeMillis()

        // Check if previous track ended without an explicit skip event
        activeTrack?.let { prev ->
            finalizeTrackPlayback(prev, wasCompleted = false)
        }

        activeTrack = track
        trackStartEpochMs = now
        maxPositionMs = 0L
        trackDurationMs = track.durationSeconds * 1000L

        // Check for replay within 2 hours
        val isReplay = track.id == lastCompletedTrackId && (now - lastCompletedEpochMs) < 2 * 60 * 60 * 1000L
        val eventType = if (isReplay) ListeningEventType.REPLAY else ListeningEventType.SONG_START

        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = eventType,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 0f,
                    timestampMs = now
                )
            )
        }
    }

    fun onProgressUpdate(positionMs: Long, durationMs: Long) {
        if (positionMs > maxPositionMs) {
            maxPositionMs = positionMs
        }
        if (durationMs > 0L) {
            trackDurationMs = durationMs
        }
    }

    fun onTrackEndedNaturally(track: Track) {
        finalizeTrackPlayback(track, wasCompleted = true)
        lastCompletedTrackId = track.id
        lastCompletedEpochMs = System.currentTimeMillis()
        activeTrack = null
    }

    fun onTrackSkipped(track: Track) {
        finalizeTrackPlayback(track, wasCompleted = false)
        activeTrack = null
    }

    private fun finalizeTrackPlayback(track: Track, wasCompleted: Boolean) {
        val duration = if (trackDurationMs > 0L) trackDurationMs else (track.durationSeconds * 1000L)
        val listenedSec = (maxPositionMs / 1000L).coerceAtLeast(0L)
        val ratio = if (duration > 0L) (maxPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

        val eventType = when {
            wasCompleted || ratio >= 0.88f -> ListeningEventType.SONG_COMPLETE
            listenedSec < 15L || ratio < 0.20f -> ListeningEventType.EARLY_SKIP
            ratio > 0.75f -> ListeningEventType.LATE_SKIP
            else -> ListeningEventType.MID_SKIP
        }

        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = eventType,
                    durationSeconds = duration / 1000L,
                    listenedSeconds = listenedSec,
                    completionRatio = ratio
                )
            )
        }
    }

    fun onTrackLiked(track: Track) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.LIKE,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 1.0f
                )
            )
        }
    }

    fun onTrackUnliked(track: Track) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.UNLIKE,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 0f
                )
            )
        }
    }

    fun onManualQueueAdd(track: Track) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.MANUAL_QUEUE_ADD,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 0f
                )
            )
        }
    }

    fun onQueueTrackRemoved(track: Track) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.QUEUE_REMOVE,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 0f
                )
            )
        }
    }

    fun onPlaylistAdd(track: Track) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.PLAYLIST_ADD,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 0f
                )
            )
        }
    }

    fun onSearchConversion(track: Track, query: String) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.SEARCH_CONVERSION,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 1.0f
                )
            )
        }
    }

    fun onDislikeArtist(artist: String) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = "",
                    title = "",
                    artist = artist,
                    album = "",
                    eventType = ListeningEventType.DISLIKE_ARTIST,
                    durationSeconds = 0L,
                    listenedSeconds = 0L,
                    completionRatio = 0f
                )
            )
        }
    }

    fun onHideTrack(track: Track) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    eventType = ListeningEventType.HIDE_TRACK,
                    durationSeconds = track.durationSeconds,
                    listenedSeconds = 0L,
                    completionRatio = 0f
                )
            )
        }
    }

    fun onArtistFollowChanged(artistName: String, isFollowing: Boolean) {
        scope.launch(Dispatchers.IO) {
            tasteProfileStore.recordEvent(
                ListeningEvent(
                    trackId = "artist_follow_$artistName",
                    title = "",
                    artist = artistName,
                    album = "",
                    eventType = if (isFollowing) ListeningEventType.LIKE else ListeningEventType.UNLIKE,
                    durationSeconds = 180L,
                    listenedSeconds = 180L,
                    completionRatio = 1.0f
                )
            )
        }
    }
}
