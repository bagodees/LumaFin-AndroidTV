package org.jellyfin.androidtv.constant

sealed interface CustomMessage {
	data object RefreshCurrentItem : CustomMessage
	data object HomeShortcutsChanged : CustomMessage
	/** A show was hidden from or returned to Next Up (#97). */
	data object NextUpChanged : CustomMessage
	data object ActionComplete : CustomMessage
}
