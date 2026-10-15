package org.jellyfin.androidtv.ui.browsing

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.util.RuntimeCandidate
import org.jellyfin.androidtv.util.RuntimeCandidatePage
import org.jellyfin.androidtv.util.RuntimeLibraryChanged
import org.jellyfin.androidtv.util.RuntimeScanLimit
import org.jellyfin.androidtv.util.scanMovieRuntimes
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.koin.core.context.GlobalContext
import java.util.Locale

/** Debug-only measurement gate; does not apply filters or persist a matching index. */
fun BrowseGridFragment.showRuntimeCheckChoices(adapter: ItemRowAdapter) {
	if (!org.jellyfin.androidtv.BuildConfig.DEBUG || !isAdded) return
	val minutes = listOf(30, 60, 90, 120)
	AlertDialog.Builder(requireContext()).setTitle(R.string.runtime_check_title)
		.setItems(minutes.map { getString(R.string.runtime_check_under, it) }.toTypedArray()) { _, index ->
			runRuntimeCheck(adapter, minutes[index])
		}.setNegativeButton(android.R.string.cancel, null).show()
}

private fun BrowseGridFragment.runRuntimeCheck(adapter: ItemRowAdapter, minutes: Int) {
	val query = adapter.itemsRequest ?: return
	val owner = viewLifecycleOwner
	val context = requireContext()
	val api = GlobalContext.get().get<ApiClient>()
	val identity = api.accessToken
	var finished = false
	val dialog = AlertDialog.Builder(context).setTitle(R.string.runtime_check_title)
		.setMessage(R.string.runtime_check_starting)
		.setNegativeButton(android.R.string.cancel, null).create()
	dialog.show()
	val job = owner.lifecycleScope.launch {
		try {
			var requests = 0
			val started = System.nanoTime()
			val result = withTimeout(180_000L) {
				scanMovieRuntimes(minutes, onProgress = { checked, total ->
					dialog.setMessage(context.getString(R.string.runtime_check_progress, checked, total))
				}) { offset, limit ->
					if (api.accessToken != identity) throw CancellationException("Account changed")
					val response = withContext(Dispatchers.IO) {
						api.itemsApi.getItems(query.copy(
							startIndex = offset, limit = limit, enableTotalRecordCount = true,
							enableImages = false, enableUserData = false, fields = emptyList(),
						)).content
					}
					if (api.accessToken != identity) throw CancellationException("Account changed")
					requests++
					RuntimeCandidatePage(response.items.map { RuntimeCandidate(it.id, it.runTimeTicks) }, response.totalRecordCount)
				}
			}
			if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
			val elapsed = String.format(Locale.getDefault(), "%.1f", (System.nanoTime() - started) / 1_000_000_000.0)
			finished = true
			dialog.dismiss()
			AlertDialog.Builder(context).setTitle(R.string.runtime_check_title)
				.setMessage(context.getString(R.string.runtime_check_result,
					result.matchingIds.size, minutes, result.checked, elapsed, requests, result.unknown))
				.setPositiveButton(android.R.string.ok, null).show()
		} catch (timeout: TimeoutCancellationException) {
			showRuntimeCheckFailure(R.string.runtime_check_limit)
		} catch (cancelled: CancellationException) {
			throw cancelled
		} catch (error: Exception) {
			val message = when (error) {
				is RuntimeLibraryChanged -> R.string.runtime_check_changed
				is RuntimeScanLimit -> R.string.runtime_check_limit
				else -> R.string.runtime_check_failed
			}
			showRuntimeCheckFailure(message)
		} finally {
			finished = true
			dialog.dismiss()
		}
	}
	dialog.setOnDismissListener { if (!finished) job.cancel() }
}

private fun BrowseGridFragment.showRuntimeCheckFailure(message: Int) {
	if (!isAdded || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
	AlertDialog.Builder(requireContext()).setTitle(R.string.runtime_check_title)
		.setMessage(message).setPositiveButton(android.R.string.ok, null).show()
}
