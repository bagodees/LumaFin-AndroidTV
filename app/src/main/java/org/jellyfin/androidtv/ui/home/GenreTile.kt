package org.jellyfin.androidtv.ui.home

/**
 * Artwork is chosen once when the Home row loads, never during focus or recomposition.
 */
data class GenreTile(
	val name: String,
	val imageUrl: String?,
)
