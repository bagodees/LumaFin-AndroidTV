package org.jellyfin.androidtv.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.UUID

data class RuntimeCandidate(val id: UUID, val ticks: Long?)
data class RuntimeCandidatePage(val items: List<RuntimeCandidate>, val total: Int)
data class RuntimeScanResult(val matchingIds: List<UUID>, val checked: Int, val unknown: Int, val pages: Int)
class RuntimeLibraryChanged : Exception()
class RuntimeScanLimit : Exception()

/** A metadata-only, complete-result scan. A failure never returns a partial index. */
suspend fun scanMovieRuntimes(
	minutes: Int,
	pageSize: Int = 500,
	maxCandidates: Int = 100_000,
	onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> },
	fetchPage: suspend (offset: Int, limit: Int) -> RuntimeCandidatePage,
): RuntimeScanResult {
	require(minutes in 1..1440 && pageSize > 0 && maxCandidates > 0)
	val threshold = minutes.toLong() * 60L * 10_000_000L
	val seen = HashSet<UUID>()
	val matches = ArrayList<UUID>()
	var offset = 0
	var unknown = 0
	var pages = 0
	var expectedTotal: Int? = null
	while (true) {
		currentCoroutineContext().ensureActive()
		val page = fetchPage(offset, pageSize)
		currentCoroutineContext().ensureActive()
		pages++
		if (page.total < 0 || (expectedTotal != null && page.total != expectedTotal)) throw RuntimeLibraryChanged()
		expectedTotal = page.total
		if (page.total > maxCandidates) throw RuntimeScanLimit()
		if (page.items.size > pageSize || (page.items.isEmpty() && offset < page.total)) throw RuntimeLibraryChanged()
		for (item in page.items) {
			if (!seen.add(item.id)) throw RuntimeLibraryChanged()
			if (seen.size > maxCandidates) throw RuntimeScanLimit()
			val ticks = item.ticks
			if (ticks == null || ticks <= 0) unknown++
			else if (ticks < threshold) matches.add(item.id)
		}
		offset += page.items.size
		if (offset > page.total) throw RuntimeLibraryChanged()
		onProgress(offset, page.total)
		if (offset == page.total) break
	}
	return RuntimeScanResult(matches.toList(), offset, unknown, pages)
}
