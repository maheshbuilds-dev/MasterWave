package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val trackCount: Int = 0,
    val tracks: List<Track> = emptyList(),
    val artworkUrl: String? = null,
    val source: String = "User", // "User", "YouTube", "Spotify", "Local", "Liked"
    val isPinned: Boolean = false,
    val createdAt: String = "Nov 12, 2026"
)
