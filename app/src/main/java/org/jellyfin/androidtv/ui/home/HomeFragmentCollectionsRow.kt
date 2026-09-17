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
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemSortBy

/**
 * Adds a single "Collections" row, matching how the webui's jellyfin-plugin-home-sections
 * CollectionsSection finds them: box sets only live under a library folder whose own content
 * type is "Collections" (CollectionType.BOXSETS) - a plain recursive/global BaseItemKind.BOX_SET
 * query across regular libraries returns nothing even when collections exist elsewhere on the
 * server. If the user has no such library (or isn't granted access to it), this row is a no-op,
 * same as any other row with nothing to show.
 */
class HomeFragmentCollectionsRow(
	private val api: ApiClient,
	private val userViews: Collection<BaseItemDto>,
) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val boxSetFolders = userViews.filter { it.collectionType == CollectionType.BOXSETS }
		if (boxSetFolders.isEmpty()) return

		val items = withContext(Dispatchers.IO) {
			coroutineScope {
				boxSetFolders.map { view ->
					async {
						runCatching {
							api.itemsApi.getItems(
								parentId = view.id,
								includeItemTypes = setOf(BaseItemKind.BOX_SET),
								recursive = true,
								collapseBoxSetItems = false,
								sortBy = setOf(ItemSortBy.SORT_NAME),
								imageTypeLimit = 1,
								fields = ItemRepository.browseFields,
							).content.items
						}.getOrDefault(emptyList())
					}
				}.awaitAll().flatten().distinctBy { it.id }
			}
		}

		if (items.isEmpty()) return

		val header = HeaderItem(context.getString(R.string.home_section_collections))
		val rowAdapter = ItemRowAdapter(context, items, cardPresenter, rowsAdapter, true)
		val row = ListRow(header, rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
