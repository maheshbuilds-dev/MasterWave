package com.masterwave.player.data.recommendation.engine

import com.masterwave.player.data.model.Track

/**
 * Types of user listening events tracked for real-time personalization.
 */
enum class ListeningEventType(val weight: Float) {
    SONG_START(0.5f),            // User initiated playback
    SONG_COMPLETE(3.0f),         // User listened to >= 90% or played to end
    LATE_SKIP(1.0f),             // User listened to > 80% before skipping (mild positive)
    EARLY_SKIP(-2.5f),           // User skipped within < 15s or < 20% (negative signal)
    MID_SKIP(0.0f),              // User skipped between 20% and 80% (neutral)
    REPLAY(5.0f),                // User played the same song again within 2 hours
    LIKE(5.0f),                  // User explicitly liked the track
    UNLIKE(-3.0f),               // User removed from favorites
    PLAYLIST_ADD(4.0f),          // User added track to custom playlist
    MANUAL_QUEUE_ADD(3.5f),      // User explicitly added track to queue (high intentionality)
    QUEUE_REMOVE(-1.5f),         // User removed track from queue
    SEARCH_CONVERSION(4.5f),     // User searched for query and selected this track
    DISLIKE_ARTIST(-100f),       // Explicit "Don't recommend this artist"
    HIDE_TRACK(-100f)            // Explicit "Hide this track / Not interested"
}

/**
 * Atomic listening interaction record.
 */
data class ListeningEvent(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val eventType: ListeningEventType,
    val durationSeconds: Long,
    val listenedSeconds: Long,
    val completionRatio: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
