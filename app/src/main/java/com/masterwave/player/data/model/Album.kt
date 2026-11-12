package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val thumbnailUrl: String? = null,
    val trackCount: Int = 0,
    val tracks: List<Track> = emptyList()
)
