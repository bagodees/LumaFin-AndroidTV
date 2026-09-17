package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest

/**
 * Shows a single grid of movies for the genre named by [folder]'s name, opened from a
 * [org.jellyfin.androidtv.ui.home.GenreTile] on the home screen.
 */
class GenreItemsFragment : BrowseFolderFragment() {
	override suspend fun setupQueries(rowLoader: RowLoader) {
		val genreName = folder?.name ?: return

		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(BaseItemKind.MOVIE),
			genres = setOf(genreName),
			recursive = true,
			sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
			sortOrder = setOf(SortOrder.DESCENDING),
			imageTypeLimit = 1,
		)

		rows.add(BrowseRowDef("", query, GENRE_ITEMS_LIMIT))
		rowLoader.loadRows(rows)
	}

	companion object {
		private const val GENRE_ITEMS_LIMIT = 100
	}
}
