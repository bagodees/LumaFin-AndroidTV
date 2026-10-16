package org.jellyfin.androidtv.ui.startup.fragment

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectionFailure

/** Persistent recovery with classified messages and actions owned by the originating screen. */
fun Fragment.showConnectionRecovery(
	failure: ConnectionFailure,
	onRetry: (() -> Unit)? = null,
	onReviewSignIn: (() -> Unit)? = null,
	onSwitchServer: (() -> Unit)? = null,
) {
	if (!isAdded || view == null || parentFragmentManager.isStateSaved) return
	val root = requireView()
	if ((root.getTag(R.id.connection_recovery_dialog) as? AlertDialog)?.isShowing == true) return
	val builder = AlertDialog.Builder(requireContext())
		.setTitle(failure.title)
		.setMessage(failure.message)
		.setNegativeButton(R.string.connection_switch_server) { _, _ ->
			onSwitchServer?.invoke()
			requireActivity().supportFragmentManager.commit {
				replace<StartupToolbarFragment>(R.id.content_view)
				add<SelectServerFragment>(R.id.content_view)
				addToBackStack(null)
			}
		}
		.setNeutralButton(android.R.string.cancel, null)
	when {
		failure.retryable && onRetry != null -> builder.setPositiveButton(R.string.connection_retry) { _, _ -> onRetry() }
		failure == ConnectionFailure.SIGN_IN && onReviewSignIn != null ->
			builder.setPositiveButton(R.string.connection_review_sign_in) { _, _ -> onReviewSignIn() }
	}
	val dialog = builder.create()
	root.setTag(R.id.connection_recovery_dialog, dialog)
	val lifecycle = viewLifecycleOwner.lifecycle
	val observer = object : DefaultLifecycleObserver {
		override fun onDestroy(owner: LifecycleOwner) { dialog.dismiss() }
	}
	lifecycle.addObserver(observer)
	dialog.setOnDismissListener {
		lifecycle.removeObserver(observer)
		if (root.getTag(R.id.connection_recovery_dialog) === dialog) root.setTag(R.id.connection_recovery_dialog, null)
	}
	dialog.show()
	dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.takeIf { it.visibility == android.view.View.VISIBLE }?.requestFocus()
}
