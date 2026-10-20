package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter

fun showBrowsePageFailure(fragment: Fragment, adapter: ItemRowAdapter, error: Exception) {
	if (error is CancellationException || !fragment.isAdded || fragment.view == null) return
	val lifecycle = fragment.viewLifecycleOwner.lifecycle
	if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
	val anchor = fragment.requireView().findFocus()
	val dialog = LumaFinMenuDialog(fragment.requireContext(), fragment.viewLifecycleOwner,
		fragment.getString(R.string.browse_page_failed), fragment.getString(R.string.browse_page_failed_help), fitContent = true)
	dialog.button(fragment.getString(android.R.string.cancel)) { dialog.dismiss() }
	dialog.button(fragment.getString(R.string.home_shortcut_retry), primary = true) {
		dialog.dismiss()
		if (fragment.isAdded && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
			if (adapter.pageRequest.canRetry) adapter.pageRequest.retry() else adapter.Retrieve()
		}
	}
	dialog.onDismiss { if (anchor?.isAttachedToWindow == true) anchor.requestFocus() }
	dialog.show()
}
