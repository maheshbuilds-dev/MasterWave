package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscribers: String? = null,
    val description: String? = null,
    val isFavorite: Boolean = false,
    val followedAtMs: Long = 0L
)
