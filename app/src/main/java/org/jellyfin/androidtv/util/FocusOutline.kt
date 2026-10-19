package org.jellyfin.androidtv.util

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.platform.AbstractComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.koin.android.ext.android.inject

/**
 * Stronger focus outline (#23) for classic views, drawn as a foreground so layouts do not move.
 * Compose content draws its own outline and is skipped here.
 */
fun FragmentActivity.installFocusOutline() {
	val preferences by inject<UserPreferences>()
	val density = resources.displayMetrics.density
	val listener = ViewTreeObserver.OnGlobalFocusChangeListener { old, new ->
		old?.clearFocusOutline()
		if (new != null && preferences[UserPreferences.strongFocus] && !new.isInCompose()) new.showFocusOutline(density)
	}
	val tree = window.decorView.viewTreeObserver
	tree.addOnGlobalFocusChangeListener(listener)
	lifecycle.addObserver(object : DefaultLifecycleObserver {
		override fun onDestroy(owner: LifecycleOwner) {
			window.decorView.viewTreeObserver.takeIf { it.isAlive }?.removeOnGlobalFocusChangeListener(listener)
		}
	})
}

/**
 * True when focus is inside Compose content, which reports its host as the focused view. Classic views
 * embedded in Compose (the whole main screen is hosted that way) are still outlined here.
 */
private fun View.isInCompose() = this is AbstractComposeView || parent is AbstractComposeView

private object NoForeground

private fun View.showFocusOutline(density: Float) {
	if (getTag(R.id.focus_outline_previous) != null) return
	setTag(R.id.focus_outline_previous, foreground ?: NoForeground)
	foreground = GradientDrawable().apply {
		setStroke((3 * density).toInt(), android.graphics.Color.WHITE)
		cornerRadius = 6 * density
	}
}

private fun View.clearFocusOutline() {
	val previous = getTag(R.id.focus_outline_previous) ?: return
	foreground = previous as? Drawable
	setTag(R.id.focus_outline_previous, null)
}
