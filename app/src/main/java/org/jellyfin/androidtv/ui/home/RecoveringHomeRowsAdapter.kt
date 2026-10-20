package org.jellyfin.androidtv.ui.home

import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.util.apiclient.EmptyResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Home-only recovery; a failed row does not reload healthy rows or move their focus. */
internal class RecoveringHomeRowsAdapter(
	private val ownerScope: CoroutineScope,
	private val lifecycle: Lifecycle,
	private val onFailure: (Exception) -> Unit,
) : MutableObjectAdapter<Row>(PositionableListRowPresenter()) {
	private var requests: CoroutineScope? = null
	private var isCurrent: () -> Boolean = { false }

	fun beginLoad(current: () -> Boolean) {
		requests?.cancel()
		isCurrent = current
		requests = CoroutineScope(ownerScope.coroutineContext + SupervisorJob(ownerScope.coroutineContext[Job]))
	}

	fun cancelRequests() {
		requests?.cancel()
		requests = null
	}

	fun retrieve(row: ItemRowAdapter) {
		val current = isCurrent
		requests?.launch {
			try {
				recoverHomeLoad(current) {
					// Retry only while Home is visible, and recheck the account after waiting.
					lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
					if (!current()) throw CancellationException("Home account changed")
					suspendCancellableCoroutine<Unit> { continuation ->
						row.setRetrieveFinishedListener(object : EmptyResponse(lifecycle) {
							override fun onResponse() {
								if (continuation.isActive) continuation.resume(Unit)
							}
							override fun onError(exception: Exception) {
								if (continuation.isActive) continuation.resumeWithException(exception)
							}
						})
						row.Retrieve()
					}
				}
			} catch (cancelled: CancellationException) {
				throw cancelled
			} catch (failure: Exception) {
				if (current()) onFailure(failure)
			}
		}
	}
}

internal fun MutableObjectAdapter<Row>.retrieveHomeRow(row: ItemRowAdapter) {
	if (this is RecoveringHomeRowsAdapter) retrieve(row) else row.Retrieve()
}
