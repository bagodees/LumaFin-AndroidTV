package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Adds one random landscape image per genre, stable for this Home row's lifetime.
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
			}.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyList())
				.mapNotNull { it.name }

			val requests = Semaphore(4)
			val candidates = coroutineScope {
				genres.map { genre ->
					async { requests.withPermit {
						val items = runCatching {
							api.itemsApi.getItems(
								includeItemTypes = setOf(BaseItemKind.MOVIE),
								genres = setOf(genre),
								recursive = true,
								sortBy = setOf(ItemSortBy.RANDOM),
								imageTypeLimit = 1,
								limit = 8,
							).content.items
						}.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyList())

						genre to items.mapNotNull { item ->
							imageHelper.getLandscapeImageUrl(item, fillWidth = 640, fillHeight = 360)?.let { item.id to it }
						}
					} }
				}.awaitAll()
			}
			val used = mutableSetOf<UUID>()
			candidates.map { (genre, images) ->
				val selected = images.firstOrNull { it.first !in used } ?: images.firstOrNull()
				selected?.first?.let(used::add)
				GenreTile(genre, selected?.second)
			}
		}

		if (tiles.isEmpty()) return

		val adapter = ArrayObjectAdapter(GenreTilePresenter())
		for (tile in tiles) adapter.add(tile)

		val row = ListRow(HeaderItem(context.getString(R.string.home_section_genres)), adapter)
		rowsAdapter.add(row)
	}

}
