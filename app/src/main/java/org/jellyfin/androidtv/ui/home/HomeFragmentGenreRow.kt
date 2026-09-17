package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest

/**
 * Adds a handful of genre-based rows (e.g. "Action", "Comedy"), picked at random from the
 * genres actually present in the user's movie libraries, each showing that genre's
 * highest-rated movies.
 */
class HomeFragmentGenreRow(
	private val api: ApiClient,
) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val genres = withContext(Dispatchers.IO) {
			runCatching {
				api.genresApi.getGenres(
					includeItemTypes = setOf(BaseItemKind.MOVIE),
					sortBy = setOf(ItemSortBy.SORT_NAME),
				).content.items
			}.getOrDefault(emptyList())
				.mapNotNull { it.name }
				.shuffled()
				.take(GENRE_ROW_COUNT)
		}

		for (genre in genres) {
			val query = GetItemsRequest(
				fields = ItemRepository.browseFields,
				includeItemTypes = setOf(BaseItemKind.MOVIE),
				genres = setOf(genre),
				recursive = true,
				sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
				sortOrder = setOf(SortOrder.DESCENDING),
				imageTypeLimit = 1,
				limit = ITEMS_PER_GENRE,
			)

			val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(genre, query, ITEMS_PER_GENRE, false, true, arrayOf(ChangeTriggerType.LibraryUpdated)))
			row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
		}
	}

	companion object {
		private const val GENRE_ROW_COUNT = 6
		private const val ITEMS_PER_GENRE = 50
	}
}
