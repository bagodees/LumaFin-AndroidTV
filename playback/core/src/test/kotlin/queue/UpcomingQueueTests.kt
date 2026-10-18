package org.jellyfin.playback.core.queue

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.queue.supplier.QueueSupplier

private suspend fun withQueue(size: Int = 5, block: suspend (QueueService, List<QueueEntry>, MutableStateFlow<PlaybackOrder>) -> Unit) {
	val queue = QueueService()
	val manager = mockk<PlaybackManager>(relaxed = true)
	val state = mockk<PlayerState>(relaxed = true)
	val order = MutableStateFlow(PlaybackOrder.DEFAULT)
	every { state.playbackOrder } returns order
	val job = Job()
	queue.initialize(manager, state, job)
	val items = List(size) { QueueEntry() }
	try {
		queue.addSupplier(object : QueueSupplier {
			override val size = items.size
			override suspend fun getItem(index: Int) = items.getOrNull(index)
		})
		withTimeout(2000) { while (queue.entryIndex.value != 0) delay(5) }
		block(queue, items, order)
	} finally { job.cancel() }
}

class UpcomingQueueTests : FunSpec({
	test("loading another page after edits preserves the supplier tail without duplicates") {
		withQueue(125) { queue, items, _ ->
			queue.peekNext(50, false, false).size shouldBe 50
			queue.entries.value.size shouldBe 51
			queue.editUpcoming(queue.entries.value, items[0], 50, 1) shouldBe true
			val removal = queue.entries.value.indexOf(items[10])
			queue.editUpcoming(queue.entries.value, items[0], removal, null) shouldBe true
			queue.peekNext(100, false, false).size shouldBe 100
			queue.entries.value.size shouldBe 101
			queue.entries.value.distinct().size shouldBe 101
			queue.entries.value[1] shouldBe items[50]
			queue.entries.value.last() shouldBe items[101]
			queue.entries.value.contains(items[10]) shouldBe false
			queue.estimatedSize shouldBe 124
			queue.entry.value shouldBe items[0]
		}
	}
	test("upcoming edits preserve the previous/current prefix, including duplicate values") {
		val items = listOf("previous", "current", "duplicate", "duplicate", "last")
		editUpcomingItems(items, 1, 4, 2) shouldBe listOf("previous", "current", "last", "duplicate", "duplicate")
		editUpcomingItems(items, 1, 2, 4) shouldBe listOf("previous", "current", "duplicate", "last", "duplicate")
		editUpcomingItems(items, 1, 3, null) shouldBe listOf("previous", "current", "duplicate", "last")
		items.size shouldBe 5
	}
	test("invalid indices, active items, history and empty queues cannot be edited") {
		val items = listOf(1, 2, 3)
		listOf(-1, 0, 1, 3).forEach { editUpcomingItems(items, 1, it, null) shouldBe null }
		listOf(-1, 0, 1, 2, 3).forEach { editUpcomingItems(items, 1, 2, it) shouldBe null }
		editUpcomingItems(emptyList<Int>(), -1, 0, null) shouldBe null
		editUpcomingItems(items, -1, 1, null) shouldBe null
	}
	test("reorder and remove leave the active entry unchanged and next advances in the edited order") {
		withQueue { queue, items, _ ->
			queue.peekNext(3, false, false)
			val reviewed = queue.entries.value
			queue.editUpcoming(reviewed, items[0], 3, 1) shouldBe true
			queue.entry.value shouldBe items[0]
			queue.entryIndex.value shouldBe 0
			queue.estimatedSize shouldBe 5
			queue.editUpcoming(queue.entries.value, items[0], 2, null) shouldBe true
			queue.estimatedSize shouldBe 4
			queue.next(false, false) shouldBe items[3]
			queue.next(false, false) shouldBe items[2]
			// Supplier cursor must still fetch the previously unloaded tail exactly once.
			queue.next(false, false) shouldBe items[4]
			queue.next(false, false) shouldBe null
		}
	}
	test("a changed snapshot or playback advance rejects a stale menu operation") {
		withQueue { queue, items, _ ->
			queue.peekNext(4, false, false)
			val reviewed = queue.entries.value
			queue.editUpcoming(reviewed, items[0], 2, null) shouldBe true
			queue.editUpcoming(reviewed, items[0], 3, null) shouldBe false
			val nextReview = queue.entries.value
			queue.next(false, false)
			queue.editUpcoming(nextReview, items[0], 3, null) shouldBe false
			queue.editUpcoming(nextReview, items[1], 0, null) shouldBe false
		}
	}
	test("shuffle/random edits are rejected and removing all upcoming entries leaves only the current item") {
		withQueue { queue, items, order ->
			queue.peekNext(4, false, false)
			order.value = PlaybackOrder.SHUFFLE
			queue.editUpcoming(queue.entries.value, items[0], 1, null) shouldBe false
			order.value = PlaybackOrder.RANDOM
			queue.editUpcoming(queue.entries.value, items[0], 1, 2) shouldBe false
			order.value = PlaybackOrder.DEFAULT
			repeat(4) { queue.editUpcoming(queue.entries.value, items[0], 1, null) shouldBe true }
			queue.entry.value shouldBe items[0]
			queue.entries.value shouldBe listOf(items[0])
			queue.estimatedSize shouldBe 1
			queue.peekNext(5, false, false).isEmpty() shouldBe true
			queue.clear()
			queue.estimatedSize shouldBe 0
			queue.entry.value shouldBe null
			queue.addSupplier(object : QueueSupplier {
				override val size = 3
				override suspend fun getItem(index: Int) = items.take(3).getOrNull(index)
			})
			withTimeout(2000) { while (queue.entryIndex.value != 0) delay(5) }
			queue.estimatedSize shouldBe 3
		}
	}
})
