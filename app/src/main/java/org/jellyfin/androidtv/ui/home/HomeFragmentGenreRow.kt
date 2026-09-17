package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.GenreTilePresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Adds a single "Genres" row containing one collage tile per genre (a 2x2 grid of that
 * genre's top-rated movies) for every genre present in the user's movie libraries.
 * Selecting a tile opens a full grid of that genre.
 */
class HomeFragmentGenreRow(
	private val api: ApiClient,
) : HomeFragmentRow, KoinComponent {
	private val imageHelper by inject<ImageHelper>()

	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val tiles = withContext(Dispatchers.IO) {
			val genres = runCatching {
				api.genresApi.getGenres(
					includeItemTypes = setOf(BaseItemKind.MOVIE),
					sortBy = setOf(ItemSortBy.SORT_NAME),
				).content.items
			}.getOrDefault(emptyList())
				.mapNotNull { it.name }

			coroutineScope {
				genres.map { genre ->
					async {
						val items = runCatching {
							api.itemsApi.getItems(
								includeItemTypes = setOf(BaseItemKind.MOVIE),
								genres = setOf(genre),
								recursive = true,
								sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
								sortOrder = setOf(SortOrder.DESCENDING),
								imageTypeLimit = 1,
								limit = TILE_IMAGE_COUNT,
							).content.items
						}.getOrDefault(emptyList())

						val imageUrls = items.mapNotNull { item ->
							imageHelper.getPrimaryImageUrl(item, width = TILE_IMAGE_SIZE, height = TILE_IMAGE_SIZE)
						}

						if (imageUrls.isEmpty()) null else GenreTile(genre, imageUrls)
					}
				}.awaitAll().filterNotNull()
			}
		}

		if (tiles.isEmpty()) return

		val adapter = ArrayObjectAdapter(GenreTilePresenter())
		for (tile in tiles) adapter.add(tile)

		val row = ListRow(HeaderItem(context.getString(R.string.home_section_genres)), adapter)
		rowsAdapter.add(row)
	}

	companion object {
		private const val TILE_IMAGE_COUNT = 4
		private const val TILE_IMAGE_SIZE = 300
	}
}
