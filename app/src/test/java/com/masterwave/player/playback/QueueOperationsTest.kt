package com.masterwave.player.playback

import com.masterwave.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueOperationsTest {
    private fun track(id: String) = Track(id = id, title = id, artist = "Test")

    @Test
    fun addDoesNotDuplicateTracks() {
        val a = track("a")
        val b = track("b")

        val once = QueueOperations.add(listOf(a), b)
        val twice = QueueOperations.add(once, b)

        assertEquals(listOf(a, b), twice)
    }

    @Test
    fun removeUpdatesCurrentIndexWhenBeforeCurrentTrack() {
        val queue = listOf(track("a"), track("b"), track("c"))

        val (updated, index) = QueueOperations.remove(queue, 2, "a")

        assertEquals(listOf(track("b"), track("c")), updated)
        assertEquals(1, index)
    }

    @Test
    fun moveUpdatesCurrentIndexAndTrackOrder() {
        val queue = listOf(track("a"), track("b"), track("c"))

        val (updated, index) = QueueOperations.move(queue, 1, 1, 2)

        assertEquals(listOf(track("a"), track("c"), track("b")), updated)
        assertEquals(2, index)
    }

    @Test
    fun playNextInsertsDirectlyAfterCurrentTrack() {
        val queue = listOf(track("a"), track("b"), track("c"))
        val d = track("d")

        val (updated, index) = QueueOperations.playNext(queue, 0, d)

        assertEquals(listOf(track("a"), track("d"), track("b"), track("c")), updated)
        assertEquals(0, index)
    }

    @Test
    fun playNextRepositionsExistingTrackWithoutDuplicates() {
        val queue = listOf(track("a"), track("b"), track("c"), track("d"))

        val (updated, index) = QueueOperations.playNext(queue, 0, track("c"))

        assertEquals(listOf(track("a"), track("c"), track("b"), track("d")), updated)
        assertEquals(0, index)
    }
}
