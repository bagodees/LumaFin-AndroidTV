package org.jellyfin.androidtv.constant

sealed interface CustomMessage {
	data object RefreshCurrentItem : CustomMessage
	data object HomeShortcutsChanged : CustomMessage
	data object ActionComplete : CustomMessage
}
