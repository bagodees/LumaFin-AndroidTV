package org.jellyfin.androidtv.data.model

data class AppNotification(
	val message: String,
	val dismiss: () -> Unit,
	val public: Boolean,
	/** Shown as a full-width banner instead of a regular card */
	val highlight: Boolean = false,
)
