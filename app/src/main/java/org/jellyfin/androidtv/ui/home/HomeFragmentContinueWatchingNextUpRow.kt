package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest

/**
 * Adds a single row merging Continue Watching and Next Up, matching the webui plugin's
 * "Continue Watching / Next Up" section. Since these come from two different endpoints they're
 * fetched concurrently and merged client-side into one static-list row.
 */
class HomeFragmentContinueWatchingNextUpRow(
	private val api: ApiClient,
) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val items = withContext(Dispatchers.IO) {
			coroutineScope {
				val resumeItems = async {
					runCatching {
						api.itemsApi.getResumeItems(
							GetResumeItemsRequest(
								limit = ITEM_LIMIT,
								fields = ItemRepository.browseFields,
								imageTypeLimit = 1,
								enableTotalRecordCount = false,
								excludeItemTypes = setOf(BaseItemKind.AUDIO_BOOK),
							)
						).content.items
					}.getOrDefault(emptyList())
				}

				val nextUpItems = async {
					runCatching {
						api.tvShowsApi.getNextUp(
							GetNextUpRequest(
								imageTypeLimit = 1,
								limit = ITEM_LIMIT,
								enableResumable = false,
								fields = ItemRepository.browseFields,
							)
						).content.items
					}.getOrDefault(emptyList())
				}

				(resumeItems.await() + nextUpItems.await()).distinctBy { it.id }
			}
		}

		if (items.isEmpty()) return

		val header = HeaderItem(context.getString(R.string.home_section_continue_watching_next_up))
		val rowAdapter = ItemRowAdapter(context, items, cardPresenter, rowsAdapter, true)
		val row = ListRow(header, rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}

	companion object {
		private const val ITEM_LIMIT = 50
	}
}
