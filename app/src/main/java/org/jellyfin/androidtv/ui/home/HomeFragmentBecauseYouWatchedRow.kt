package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest

/**
 * Adds a handful of "Because you watched X" rows, seeded from the most recently watched movies,
 * each populated with similar items. Mirrors [org.jellyfin.androidtv.ui.browsing.SuggestedMoviesFragment].
 */
class HomeFragmentBecauseYouWatchedRow(
	private val api: ApiClient,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
			val seeds = runCatching {
				api.itemsApi.getItems(
					includeItemTypes = setOf(BaseItemKind.MOVIE),
					sortBy = setOf(ItemSortBy.DATE_PLAYED),
					sortOrder = setOf(SortOrder.DESCENDING),
					recursive = true,
					limit = SEED_COUNT,
				).content.items
			}.getOrDefault(emptyList())

			withContext(Dispatchers.Main) {
				for (seed in seeds) {
					val similarQuery = GetSimilarItemsRequest(
						itemId = seed.id,
						fields = ItemRepository.itemFields,
						limit = SIMILAR_ITEM_LIMIT,
					)

					val title = context.getString(R.string.because_you_watched, seed.name)
					val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(title, similarQuery, QueryType.SimilarMovies))
					row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
				}
			}
		}
	}

	companion object {
		private const val SEED_COUNT = 5
		private const val SIMILAR_ITEM_LIMIT = 20
	}
}
