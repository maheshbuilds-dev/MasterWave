package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

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
    val isFavorite: Boolean = false
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
