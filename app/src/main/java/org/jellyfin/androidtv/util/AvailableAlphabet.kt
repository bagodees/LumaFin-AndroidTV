package org.jellyfin.androidtv.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Bounded probes preserve alphabet order and never mistake a failed request for an empty letter. */
internal suspend fun availableAlphabet(candidates: String, hasMatch: suspend (Char) -> Boolean): String = coroutineScope {
	val semaphore = Semaphore(4)
	candidates.toList().distinct().map { letter ->
		async { semaphore.withPermit { letter.takeIf { hasMatch(it) } } }
	}.awaitAll().filterNotNull().joinToString("")
}
