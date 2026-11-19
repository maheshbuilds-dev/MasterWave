package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrackAvailability {
    FULL_TRACK,
    PREVIEW_ONLY,
    METADATA_ONLY,
    UNAVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    ERROR
}

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Long = 0L,
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val isLocal: Boolean = false,
    val localUri: String? = null,
    val codec: String = "Opus",
    val bitrateKbps: Int = 160,
    val isFavorite: Boolean = false,
    val providerId: String = if (isLocal) "local" else "youtube",
    val releaseYear: Int? = null,
    val language: String? = null,
    val genre: String? = null,
    val externalSourceUrl: String? = null,
    val license: String? = null,
    val attribution: String? = null,
    val availability: TrackAvailability = TrackAvailability.FULL_TRACK,
    val isExplicit: Boolean = false
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val isPlayable: Boolean
        get() = availability == TrackAvailability.FULL_TRACK || availability == TrackAvailability.PREVIEW_ONLY
}
