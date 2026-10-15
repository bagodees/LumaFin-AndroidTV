package org.jellyfin.androidtv.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.data.model.FilterOptions
import java.util.UUID

class RuntimeResultPageTests : FunSpec({
	test("index pages remain bounded, complete and nonoverlapping including the final page") {
		val ids = (0L until 889).map { UUID(0, it) }
		val pages = (ids.indices step 40).map { runtimePageIds(ids, it, 40) }
		pages.flatten() shouldBe ids
		pages.last().size shouldBe 9
		runtimePageIds(ids, 0, 1000).size shouldBe 100
		runtimePageIds(ids, ids.size, 40) shouldBe emptyList()
	}
	test("server response ordering cannot alter the checked sort order") {
		val ids = listOf(UUID(0, 9), UUID(0, 2), UUID(0, 7))
		orderRuntimePage(ids, ids.reversed()) { it } shouldBe ids
	}
	test("missing, duplicate or unexpected IDs invalidate the page rather than skipping movies") {
		val ids = listOf(UUID(0, 1), UUID(0, 2))
		shouldThrow<RuntimeLibraryChanged> { orderRuntimePage(ids, listOf(ids.first())) { it } }
		shouldThrow<RuntimeLibraryChanged> { orderRuntimePage(ids, listOf(ids.first(), ids.first())) { it } }
		shouldThrow<RuntimeLibraryChanged> { orderRuntimePage(ids, listOf(ids.first(), UUID(0, 3))) { it } }
	}
	test("editing or cancelling a draft does not mutate the applied preferences") {
		val applied = FilterOptions().apply { genre = "Drama"; runtimeMinutes = 90; isFavoriteOnly = true }
		val draft = applied.copy().apply { genre = "Comedy"; runtimeMinutes = 30; isFavoriteOnly = false }
		applied.genre shouldBe "Drama"
		applied.runtimeMinutes shouldBe 90
		applied.isFavoriteOnly shouldBe true
		draft.runtimeMinutes shouldBe 30
	}
})
