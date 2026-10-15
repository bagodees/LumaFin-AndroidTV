package org.jellyfin.androidtv.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import java.util.UUID

class RuntimeFilterScanTests : FunSpec({
	fun movie(index: Long, seconds: Long?) = RuntimeCandidate(UUID(0, index), seconds?.times(10_000_000L))

	test("duration is strict and missing or invalid runtimes do not count as short movies") {
		val items = listOf(movie(1, 1799), movie(2, 1800), movie(3, 1801), movie(4, null), movie(5, 0), movie(6, -1))
		val result = scanMovieRuntimes(30) { _, _ -> RuntimeCandidatePage(items, items.size) }
		result.matchingIds shouldBe listOf(items.first().id)
		result.unknown shouldBe 3
		result.checked shouldBe 6
	}
	test("a page with no matches does not stop scanning later pages and result order is preserved") {
		val items = listOf(movie(1, 8000), movie(2, 9000), movie(3, 60), movie(4, 100), movie(5, 10))
		val offsets = mutableListOf<Int>()
		val progress = mutableListOf<Int>()
		val result = scanMovieRuntimes(30, pageSize = 2, onProgress = { checked, _ -> progress += checked }) { offset, limit ->
			offsets += offset
			RuntimeCandidatePage(items.drop(offset).take(limit), items.size)
		}
		offsets shouldBe listOf(0, 2, 4)
		progress shouldBe listOf(2, 4, 5)
		result.matchingIds shouldBe items.drop(2).map { it.id }
		result.pages shouldBe 3
	}
	test("exact page boundaries and empty libraries do not require an extra request") {
		val items = (1L..4).map { movie(it, 60) }
		var requests = 0
		val result = scanMovieRuntimes(30, pageSize = 2) { offset, limit ->
			requests++
			RuntimeCandidatePage(items.drop(offset).take(limit), items.size)
		}
		requests shouldBe 2
		result.checked shouldBe 4
		scanMovieRuntimes(30) { _, _ -> RuntimeCandidatePage(emptyList(), 0) }.matchingIds shouldBe emptyList()
	}
	test("changed totals and duplicate IDs fail instead of publishing incomplete results") {
		shouldThrow<RuntimeLibraryChanged> {
			scanMovieRuntimes(30, pageSize = 1) { offset, _ ->
				RuntimeCandidatePage(listOf(movie(offset.toLong(), 60)), if (offset == 0) 2 else 3)
			}
		}
		shouldThrow<RuntimeLibraryChanged> {
			scanMovieRuntimes(30, pageSize = 1) { _, _ -> RuntimeCandidatePage(listOf(movie(1, 60)), 2) }
		}
	}
	test("an early empty page and candidate budget failure are not successful zero-match results") {
		shouldThrow<RuntimeLibraryChanged> {
			scanMovieRuntimes(30) { _, _ -> RuntimeCandidatePage(emptyList(), 10) }
		}
		shouldThrow<RuntimeScanLimit> {
			scanMovieRuntimes(30, maxCandidates = 2) { _, _ -> RuntimeCandidatePage(listOf(movie(1, 60)), 3) }
		}
	}
	test("cancellation stops further requests and publishes no progress or result") {
		var requests = 0
		var progress = 0
		shouldThrow<CancellationException> {
			scanMovieRuntimes(30, onProgress = { _, _ -> progress++ }) { _, _ ->
				requests++
				throw CancellationException("User cancelled")
			}
		}
		requests shouldBe 1
		progress shouldBe 0
	}
	test("a large synthetic library remains paginated with no lost or duplicated matches") {
		val result = scanMovieRuntimes(120) { offset, limit ->
			RuntimeCandidatePage((offset until minOf(offset + limit, 100_000)).map {
				movie(it.toLong(), if (it % 2 == 0) 60 else 8000)
			}, 100_000)
		}
		result.checked shouldBe 100_000
		result.matchingIds.size shouldBe 50_000
		result.matchingIds.distinct().size shouldBe 50_000
		result.pages shouldBe 200
	}
})
