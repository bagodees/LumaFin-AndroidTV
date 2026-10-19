package org.jellyfin.androidtv.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.data.repository.applyBulkWatch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class BulkWatchTests : FunSpec({
	fun item(number: Long, watched: Boolean = false, position: Long = 0, count: Int = 0) = BaseItemDto(id = UUID(0, number), type = BaseItemKind.MOVIE,
		userData = mockk<UserItemDataDto> {
			every { played } returns watched
			every { playbackPositionTicks } returns position
			every { playCount } returns count
			every { lastPlayedDate } returns null
			every { playedPercentage } returns null
		})
	test("only reviewed unique IDs change and already-watched history is preserved") {
		val a = item(1); val b = item(2, true); val writes = mutableListOf<UUID>()
		val result = applyBulkWatch(listOf(a, b, a), true, { if (it == a.id) a else b }, { id, _ -> writes += id }, { true }, { false }, {})
		writes shouldBe listOf(a.id)
		result.changed shouldBe listOf(a.id)
		result.unchanged shouldBe listOf(b.id)
	}
	test("unwatched clears resume and old play counts even when watched flag is already false") {
		val a = item(1, position = 500); val b = item(2, count = 2); val c = item(3)
		val targets = listOf(a, b, c); val writes = mutableListOf<UUID>()
		val result = applyBulkWatch(targets, false, { id -> targets.first { it.id == id } }, { id, value -> value shouldBe false; writes += id }, { true }, { false }, {})
		writes shouldBe listOf(a.id, b.id)
		result.unchanged shouldBe listOf(c.id)
	}
	test("partial read failures are reported separately while valid targets continue") {
		val targets = listOf(item(1), item(2), item(3)); val writes = mutableListOf<UUID>()
		val result = applyBulkWatch(targets, true, { id -> if (id == targets[1].id) error("offline") else targets.first { it.id == id } }, { id, _ -> writes += id }, { true }, { false }, {})
		writes shouldBe listOf(targets[0].id, targets[2].id)
		result.failed shouldBe listOf(targets[1].id)
	}
	test("an uncertain write is never retried and leaves the remaining targets untouched") {
		val targets = listOf(item(1), item(2), item(3)); var writes = 0; var refreshes = 0
		val result = applyBulkWatch(targets, true, { id -> targets.first { it.id == id } }, { _, _ -> writes++; if (writes == 2) error("unknown server result") }, { true }, { false }, { refreshes++ })
		writes shouldBe 2
		refreshes shouldBe 2
		result.changed shouldBe listOf(targets[0].id)
		result.unconfirmed shouldBe listOf(targets[1].id)
		result.notAttempted shouldBe listOf(targets[2].id)
	}
	test("stop after one request retains completion and does not start another item") {
		val targets = listOf(item(1), item(2)); var stop = false
		val result = applyBulkWatch(targets, true, { id -> targets.first { it.id == id } }, { _, _ -> stop = true }, { true }, { stop }, {})
		result.changed shouldBe listOf(targets[0].id)
		result.notAttempted shouldBe listOf(targets[1].id)
	}
	test("account change during retrieval prevents a write using stale reviewed state") {
		val target = item(1); var current = true; var writes = 0
		val result = applyBulkWatch(listOf(target), true, { current = false; target }, { _, _ -> writes++ }, { current }, { false }, {})
		writes shouldBe 0
		result.notAttempted shouldBe listOf(target.id)
	}
	test("server returning another item or a folder cannot cause a recursive mutation") {
		val target = item(1)
		for (fresh in listOf(item(2), target.copy(type = BaseItemKind.SERIES))) {
			val result = applyBulkWatch(listOf(target), true, { fresh }, { _, _ -> error("must not write") }, { true }, { false }, {})
			result.failed shouldBe listOf(target.id)
		}
	}
	test("empty oversized and recursive selections are rejected before any request") {
		for (selection in listOf(emptyList(), (1L..101L).map { item(it) }, listOf(item(1).copy(type = BaseItemKind.SEASON)))) {
			shouldThrow<IllegalArgumentException> { applyBulkWatch(selection, true, { error("must not read") }, { _, _ -> error("must not write") }, { true }, { false }, {}) }
		}
	}
	test("lifecycle cancellation propagates and invalidates a potentially completed write") {
		var refreshed = false
		shouldThrow<CancellationException> { applyBulkWatch(listOf(item(1)), true, { item(1) }, { _, _ -> throw CancellationException() }, { true }, { false }, { refreshed = true }) }
		refreshed shouldBe true
	}
	test("repeated read failures stop bounded work and leave unrequested items explicit") {
		val targets = (1L..10L).map { item(it) }
		var reads = 0
		val result = applyBulkWatch(targets, true, { reads++; error("offline") }, { _, _ -> error("must not write") }, { true }, { false }, {})
		reads shouldBe 3
		result.failed.size shouldBe 3
		result.notAttempted.size shouldBe 7
	}
})
