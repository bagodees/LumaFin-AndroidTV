package org.jellyfin.androidtv.ui.itemhandling

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Main-thread owned. Replacing a query cancels its work and rejects even uncooperative late responses. */
class LatestPageRequest {
	private var generation = 0L
	private var job: Job? = null
	private var failedPage: (() -> Unit)? = null
	val canRetry get() = failedPage != null
	fun retry() { failedPage?.invoke() }
	fun cancel() {
		generation++
		job?.cancel()
		job = null
		failedPage = null
	}
	fun <T> load(
		scope: CoroutineScope,
		fetch: suspend () -> T,
		isCurrent: () -> Boolean,
		apply: (T) -> Unit,
		finished: (Exception?) -> Unit,
		started: () -> Unit = {},
	) {
		cancel()
		val request = generation
		started()
		job = scope.launch {
			try {
				if (!isCurrent()) { finished(CancellationException("Browse context changed")); return@launch }
				val result = fetch()
				if (request != generation) return@launch
				if (!isCurrent()) { finished(CancellationException("Browse context changed")); return@launch }
				apply(result)
				finished(null)
			} catch (cancelled: CancellationException) {
				if (request == generation) finished(cancelled)
				throw cancelled
			} catch (error: Exception) {
				if (request == generation) {
					if (!isCurrent()) { finished(CancellationException("Browse context changed")); return@launch }
					failedPage = { load(scope, fetch, isCurrent, apply, finished, started) }
					finished(error)
				}
			}
		}
	}
}
