package org.jellyfin.androidtv.util

import android.view.View
import android.view.ViewTreeObserver

/**
 * Listens for focus changes in the window only while this view is in it. A view's tree observer is
 * the window's while the view is attached and a private one after it leaves, so removing a listener
 * once the page has closed missed the window's observer, which then kept the whole page in memory.
 * Returns a callback that stops listening.
 */
fun View.listenForFocusWhileAttached(listener: ViewTreeObserver.OnGlobalFocusChangeListener): Runnable {
	val attachListener = object : View.OnAttachStateChangeListener {
		override fun onViewAttachedToWindow(view: View) = view.viewTreeObserver.addOnGlobalFocusChangeListener(listener)
		override fun onViewDetachedFromWindow(view: View) = view.viewTreeObserver.removeOnGlobalFocusChangeListener(listener)
	}
	addOnAttachStateChangeListener(attachListener)
	if (isAttachedToWindow) viewTreeObserver.addOnGlobalFocusChangeListener(listener)

	return Runnable {
		removeOnAttachStateChangeListener(attachListener)
		viewTreeObserver.removeOnGlobalFocusChangeListener(listener)
	}
}
