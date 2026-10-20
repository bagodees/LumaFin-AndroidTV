package org.jellyfin.androidtv.ui.home

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/** Retry temporary startup failures, but never turn cancellation into another request. */
internal suspend fun <T> recoverHomeLoad(
	isCurrent: () -> Boolean,
	wait: suspend (Long) -> Unit = { delay(it) },
	load: suspend () -> T,
): T {
	val delays = listOf(2_000L, 5_000L, 10_000L)
	var attempt = 0
	while (true) {
		currentCoroutineContext().ensureActive()
		if (!isCurrent()) throw CancellationException("Home load replaced")
		try {
			val result = load()
			currentCoroutineContext().ensureActive()
			if (!isCurrent()) throw CancellationException("Home load replaced")
			return result
		} catch (cancelled: CancellationException) {
			throw cancelled
		} catch (failure: Exception) {
			if (attempt == delays.size) throw failure
			wait(delays[attempt++])
		}
	}
}
