package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val query: String = "",
    val songs: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList()
)
