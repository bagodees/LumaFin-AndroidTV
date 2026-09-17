package org.jellyfin.androidtv.ui.home

/**
 * A single "genre" tile shown in the Genres home row: a collage of a genre's top movie images.
 */
data class GenreTile(
	val name: String,
	val imageUrls: List<String>,
)
