package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.FilterOptions
import org.jellyfin.androidtv.util.RuntimeCandidate
import org.jellyfin.androidtv.util.RuntimeCandidatePage
import org.jellyfin.androidtv.util.RuntimeLibraryChanged
import org.jellyfin.androidtv.util.RuntimeScanLimit
import org.jellyfin.androidtv.util.scanMovieRuntimes
import org.jellyfin.androidtv.util.runtimePageIds
import org.jellyfin.androidtv.util.orderRuntimePage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import java.util.UUID

/** View-owned result source. Its cursor is the matching-ID index, not a server offset. */
class RuntimeFilterController(
	private val context: Context,
	private val owner: LifecycleOwner,
	private val adapter: ItemRowAdapter,
	private val api: ApiClient,
	private val onRestored: () -> Unit,
	private val onOpenFilters: () -> Unit,
) {
	private data class Plan(val query: GetItemsRequest, val minutes: Int, val identity: String?,
		val ids: List<UUID>, val firstPage: List<BaseItemDto>)
	private data class Snapshot(val query: GetItemsRequest, val options: FilterOptions,
		val rows: List<Any>, val total: Int, val loaded: Int)
	private var plan: Plan? = null
	private var prepared = false
	private var snapshot: Snapshot? = null
	private var job: Job? = null
	private var generation = 0

	init {
		owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_DESTROY) clear()
		})
	}

	fun clear() {
		generation++
		job?.cancel()
		plan = null
		prepared = false
		snapshot = null
	}

	fun applyDraft(options: FilterOptions, apply: () -> Unit, cancelled: () -> Unit) {
		val minutes = options.runtimeMinutes
		if (minutes == null) { clear(); adapter.setFilters(options); apply(); return }
		val query = setCombinedItemsFilter(adapter.itemsRequest, options, adapter.filterBaseRequest)
		startCheck(cancelled, { error -> showFailure(error) { applyDraft(options, apply, cancelled) } }) { progress, identity ->
			val checked = prepare(query, minutes, identity, progress)
			plan = checked
			prepared = true
			adapter.setFilters(options)
			apply()
		}
	}

	fun retrieve(query: GetItemsRequest, offset: Int, batchSize: Int) {
		val minutes = adapter.filters.runtimeMinutes ?: return
		val key = query.copy(startIndex = null, limit = null)
		val cached = plan?.takeIf { it.query == key && it.minutes == minutes && it.identity == api.accessToken }
		val needsCheck = cached == null || (offset == 0 && !prepared)
		prepared = false
		adapter.beginRuntimeRetrieve()
		val cancel = { restore(); onOpenFilters() }
		val failure: (Exception) -> Unit = { error ->
			restore()
			showFailure(error) { adapter.Retrieve() }
		}
		startCheck(cancel, failure, needsCheck) { progress, identity ->
			val current = if (needsCheck) prepare(query, minutes, identity, progress) else cached!!
			val items = if (offset == 0 && batchSize == adapter.runtimePageSize) current.firstPage
				else readPage(current.query, current.ids, offset, batchSize, minutes, identity)
			if (adapter.itemsRequest != query || adapter.filters.runtimeMinutes != minutes) throw CancellationException()
			plan = current
			adapter.totalItems = current.ids.size
			adapter.setItems(items) { item, _ -> BaseItemDtoBaseRowItem(item, adapter.preferParentThumb, adapter.isStaticHeight) }
			snapshot = Snapshot(query, adapter.filters.copy(), (0 until adapter.size()).mapNotNull { adapter.get(it) },
				adapter.totalItems, adapter.itemsLoaded)
			adapter.notifyRetrieveFinished()
		}
	}

	/** Alphabet probes must include the client-side duration, rather than matching long movies. */
	suspend fun hasRuntimeMatches(query: GetItemsRequest): Boolean {
		val minutes = adapter.filters.runtimeMinutes ?: return false
		val identity = api.accessToken
		val result = scanMovieRuntimes(minutes) { offset, limit ->
			checkIdentity(identity)
			val response = withContext(Dispatchers.IO) {
				api.itemsApi.getItems(query.copy(startIndex = offset, limit = limit, enableImages = false,
					enableUserData = false, fields = emptyList(), enableTotalRecordCount = true)).content
			}
			checkIdentity(identity)
			RuntimeCandidatePage(response.items.map { RuntimeCandidate(it.id, it.runTimeTicks) }, response.totalRecordCount)
		}
		return result.matchingIds.isNotEmpty()
	}

	private fun restore() {
		val old = snapshot
		if (old != null) {
			adapter.restoreRuntimeState(old.query, old.options.copy(), old.rows, old.total, old.loaded)
			onRestored()
			adapter.notifyRetrieveFinished()
		} else adapter.notifyRetrieveFinished(CancellationException("Runtime check cancelled"))
	}

	private fun startCheck(
		onCancel: () -> Unit,
		onFailure: (Exception) -> Unit,
		showProgress: Boolean = true,
		block: suspend (progress: (Int, Int) -> Unit, identity: String?) -> Unit,
	) {
		val current = ++generation
		job?.cancel()
		val identity = api.accessToken
		var finished = false
		val dialog = if (showProgress) org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog(context, owner,
			context.getString(R.string.runtime_filter_title), context.getString(R.string.runtime_check_starting)).apply {
			button(context.getString(android.R.string.cancel)) { dismiss() }
		} else null
		job = owner.lifecycleScope.launch {
			try {
				// Saved filters are retrieved from onViewCreated, before the view is
				// STARTED. Wait for it instead of cancelling that initial retrieval.
				owner.lifecycle.withStarted { }
				checkIdentity(identity)
				dialog?.show()
				withTimeout(180_000L) {
					block({ checked, total -> dialog?.description(context.getString(R.string.runtime_check_progress, checked, total)) }, identity)
				}
			} catch (timeout: TimeoutCancellationException) {
				if (current == generation && active(identity)) onFailure(RuntimeScanLimit())
			} catch (cancelled: CancellationException) {
				throw cancelled
			} catch (error: Exception) {
				if (current == generation && active(identity)) onFailure(error)
			} finally {
				finished = true
				dialog?.dismiss()
			}
		}
		dialog?.onDismiss {
			if (!finished && current == generation) {
				generation++
				job?.cancel()
				if (active(identity)) onCancel()
			}
		}
	}

	private fun active(identity: String?) = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && api.accessToken == identity
	private fun checkIdentity(identity: String?) { if (!active(identity)) throw CancellationException("Library no longer active") }

	private suspend fun prepare(query: GetItemsRequest, minutes: Int, identity: String?, progress: (Int, Int) -> Unit): Plan {
		val key = query.copy(startIndex = null, limit = null)
		var attempt = 0
		while (true) {
			try {
				val result = scanMovieRuntimes(minutes, onProgress = progress) { offset, limit ->
					checkIdentity(identity)
					val response = withContext(Dispatchers.IO) {
						api.itemsApi.getItems(key.copy(startIndex = offset, limit = limit,
							enableImages = false, enableUserData = false, fields = emptyList(), enableTotalRecordCount = true)).content
					}
					checkIdentity(identity)
					RuntimeCandidatePage(response.items.map { RuntimeCandidate(it.id, it.runTimeTicks) }, response.totalRecordCount)
				}
				val first = readPage(key, result.matchingIds, 0, adapter.runtimePageSize, minutes, identity)
				return Plan(key, minutes, identity, result.matchingIds, first)
			} catch (changed: RuntimeLibraryChanged) {
				if (attempt++ > 0) throw changed
			}
		}
	}

	private suspend fun readPage(query: GetItemsRequest, ids: List<UUID>, offset: Int, limit: Int, minutes: Int, identity: String?): List<BaseItemDto> {
		val selected = runtimePageIds(ids, offset, limit)
		if (selected.isEmpty()) return emptyList()
		checkIdentity(identity)
		val response = withContext(Dispatchers.IO) {
			api.itemsApi.getItems(query.copy(ids = selected, startIndex = 0, limit = selected.size, enableTotalRecordCount = false)).content.items
		}
		checkIdentity(identity)
		val ordered = orderRuntimePage(selected, response) { it.id }
		val threshold = minutes.toLong() * 60 * 10_000_000
		return ordered.map { movie -> movie.also {
			if (it.runTimeTicks == null || it.runTimeTicks!! <= 0 || it.runTimeTicks!! >= threshold) throw RuntimeLibraryChanged()
		} }
	}

	private fun showFailure(error: Exception, retry: () -> Unit) {
		val message = when (error) {
			is RuntimeScanLimit -> R.string.runtime_check_limit
			is RuntimeLibraryChanged -> R.string.runtime_check_changed
			else -> R.string.runtime_check_failed
		}
		val identity = api.accessToken
		val dialog = org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog(context, owner,
			context.getString(R.string.runtime_filter_title), context.getString(message))
		var retrying = false
		dialog.button(context.getString(android.R.string.cancel)) { dialog.dismiss() }
		dialog.button(context.getString(R.string.runtime_filter_retry), true) {
			retrying = true; dialog.dismiss(); if (active(identity)) retry()
		}
		dialog.onDismiss { if (!retrying && active(identity)) onOpenFilters() }
		dialog.show()
	}
}
