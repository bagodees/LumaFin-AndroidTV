package org.jellyfin.androidtv.ui.browsing

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter

fun showBrowsePageFailure(fragment: Fragment, adapter: ItemRowAdapter, error: Exception) {
	if (error is CancellationException || !fragment.isAdded || fragment.view == null) return
	val lifecycle = fragment.viewLifecycleOwner.lifecycle
	if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
	val dialog = AlertDialog.Builder(fragment.requireContext()).setTitle(R.string.browse_page_failed)
		.setMessage(R.string.browse_page_failed_help)
		.setPositiveButton(R.string.home_shortcut_retry) { _, _ ->
			if (adapter.pageRequest.canRetry) adapter.pageRequest.retry() else adapter.Retrieve()
		}
		.setNegativeButton(android.R.string.cancel, null).create()
	val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_DESTROY) dialog.dismiss() }
	lifecycle.addObserver(observer)
	dialog.setOnDismissListener { lifecycle.removeObserver(observer) }
	dialog.show()
}
