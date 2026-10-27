package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.ItemFilter


class FilterOptions {
	var isFavoriteOnly = false
	var isUnwatchedOnly = false
	var genre: String? = null
	var year: Int? = null
	var minimumRating: Int? = null
	var resolution = LibraryResolution.ANY
	var runtimeMinutes: Int? = null
	var mediaType = LibraryMediaType.ALL

	fun copy() = FilterOptions().also {
		it.isFavoriteOnly = isFavoriteOnly; it.isUnwatchedOnly = isUnwatchedOnly
		it.genre = genre; it.year = year; it.minimumRating = minimumRating
		it.resolution = resolution; it.runtimeMinutes = runtimeMinutes; it.mediaType = mediaType
	}

	val filters: Set<ItemFilter>
		get() = buildSet {
			if (isFavoriteOnly) add(ItemFilter.IS_FAVORITE)
			if (isUnwatchedOnly) add(ItemFilter.IS_UNPLAYED)
		}
}
