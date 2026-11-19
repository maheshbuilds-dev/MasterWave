package com.masterwave.player.playback

import com.masterwave.player.data.model.Track

internal object QueueOperations {
    fun add(queue: List<Track>, track: Track): List<Track> {
        if (queue.any { it.id == track.id }) return queue
        return queue + track
    }

    fun remove(queue: List<Track>, currentIndex: Int, trackId: String): Pair<List<Track>, Int> {
        val removedIndex = queue.indexOfFirst { it.id == trackId }
        if (removedIndex < 0) return queue to currentIndex

        val updated = queue.toMutableList().apply { removeAt(removedIndex) }
        val updatedIndex = when {
            removedIndex < currentIndex -> currentIndex - 1
            removedIndex == currentIndex && updated.isNotEmpty() -> removedIndex.coerceAtMost(updated.lastIndex)
            updated.isEmpty() -> -1
            else -> currentIndex
        }
        return updated to updatedIndex
    }

    fun move(queue: List<Track>, currentIndex: Int, fromIndex: Int, toIndex: Int): Pair<List<Track>, Int> {
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) {
            return queue to currentIndex
        }

        val updated = queue.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        val updatedIndex = when {
            currentIndex == fromIndex -> toIndex
            fromIndex < toIndex && currentIndex > fromIndex && currentIndex <= toIndex -> currentIndex - 1
            fromIndex > toIndex && currentIndex >= toIndex && currentIndex < fromIndex -> currentIndex + 1
            else -> currentIndex
        }
        return updated to updatedIndex
    }
}
