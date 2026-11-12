package com.masterwave.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@Serializable
data class LyricsResult(
    val trackId: String,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val plainLyrics: String? = null
)
