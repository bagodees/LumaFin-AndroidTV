package org.jellyfin.androidtv.ui.navigation.focus

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusRequester

@Stable
class FocusPersistManager {
	private val requesters = mutableMapOf<String, FocusRequester>()
	private var lastFocusedKey: String? = null
	private var initialFocusRequester: FocusRequester? = null
	private var initialFocusAllowed = true
	private var restoredFocusKey: String? = null
	// A row in a lazy list may not exist yet when its page comes back; it gets focus once placed, briefly
	private var pendingRestoreKey: String? = null
	/** False while the page is no longer the top one, e.g. sliding out. */
	var recording = true
	private var pendingRestoreUntil = 0L

	fun register(key: String, requester: FocusRequester, initialFocus: Boolean) {
		val previousRequester = requesters.put(key, requester)
		if (initialFocusRequester === previousRequester) initialFocusRequester = null
		if (initialFocus && initialFocusAllowed) initialFocusRequester = requester
	}

	fun unregister(key: String) {
		val requester = requesters.remove(key)
		if (initialFocusRequester === requester) initialFocusRequester = null
	}

	fun onFocusChanged(key: String, isFocused: Boolean) {
		if (!isFocused || !recording) return

		lastFocusedKey = key
		if (initialFocusRequester == null || requesters[key] === initialFocusRequester) {
			initialFocusAllowed = false
		}
	}

	fun requestInitialFocus(requester: FocusRequester) {
		if (!initialFocusAllowed || restoredFocusKey != null || initialFocusRequester !== requester) return

		if (requester.requestFocus()) initialFocusAllowed = false
	}

	fun save(): String? = lastFocusedKey

	fun restore(key: String?) {
		restoredFocusKey = key
		if (key == null) return

		initialFocusAllowed = false
		if (requesters[key]?.requestFocus() != true) {
			pendingRestoreKey = key
			pendingRestoreUntil = android.os.SystemClock.uptimeMillis() + PENDING_RESTORE_MS
		}
	}

	fun onPlaced(key: String, requester: FocusRequester) {
		if (key != pendingRestoreKey) return
		if (android.os.SystemClock.uptimeMillis() > pendingRestoreUntil || requester.requestFocus()) pendingRestoreKey = null
	}

	private companion object {
		const val PENDING_RESTORE_MS = 1_000L
	}
}

val LocalFocusPersistManager = compositionLocalOf<FocusPersistManager> {
	error("No FocusPersistManager provided")
}
