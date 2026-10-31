package org.jellyfin.androidtv.ui.search

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.home.SeerrCardPresenter
import org.jellyfin.androidtv.ui.home.SeerrItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

/** Seerr cards at the same size as the library cards above them. */
private const val SEERR_CARD_HEIGHT = 130

class SearchFragmentDelegate(
	private val context: Context,
	private val backgroundService: BackgroundService,
	private val itemLauncher: ItemLauncher,
	private val navigationRepository: NavigationRepository,
) {
	val rowsAdapter = MutableObjectAdapter<Row>(CustomListRowPresenter())
	var onSelectionChanged: (Int, Int) -> Unit = { _, _ -> }

	fun showResults(searchResultGroups: Collection<SearchResultGroup>, seerr: List<SeerrItem> = emptyList()) {
		rowsAdapter.clear()
		val adapters = mutableListOf<ItemRowAdapter>()
		for ((labelRes, baseItems) in searchResultGroups) {
			val adapter = ItemRowAdapter(
				context,
				baseItems.toList(),
				CardPresenter(),
				rowsAdapter,
				QueryType.Search
			).apply {
				setRow(ListRow(HeaderItem(context.getString(labelRes)), this))
			}
			adapters.add(adapter)
		}
		for (adapter in adapters) adapter.Retrieve()
		// Titles that aren't here yet, last, so they can be requested (#100)
		if (seerr.isNotEmpty()) {
			val adapter = ArrayObjectAdapter(SeerrCardPresenter(cardHeight = SEERR_CARD_HEIGHT)).apply { addAll(0, seerr) }
			rowsAdapter.add(ListRow(HeaderItem(context.getString(R.string.search_not_in_library)), adapter))
		}
	}

	fun clearBackground() = backgroundService.clearBackgrounds()

	val onItemViewClickedListener = OnItemViewClickedListener { _, item, _, row ->
		if (item is SeerrItem) {
			navigationRepository.navigate(Destinations.seerrDetails(item.mediaType, item.mediaId))
			return@OnItemViewClickedListener
		}
		if (item !is BaseRowItem) return@OnItemViewClickedListener
		row as ListRow
		val adapter = row.adapter as ItemRowAdapter
		itemLauncher.launch(item as BaseRowItem?, adapter, context)
	}

	val onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, row ->
		if (row is ListRow && item != null) {
			val column = (row.adapter as? ItemRowAdapter)?.indexOf(item) ?: (row.adapter as? ArrayObjectAdapter)?.indexOf(item)
			if (column != null) onSelectionChanged(rowsAdapter.indexOf(row).coerceAtLeast(0), column.coerceAtLeast(0))
		}
		if (item is SeerrItem) {
			backgroundService.setBackgroundUrl(item.imageUrl)
			return@OnItemViewSelectedListener
		}
		val baseItem = (item as? BaseRowItem)?.baseItem
		if (baseItem != null) {
			backgroundService.setBackground(baseItem)
		} else {
			backgroundService.clearBackgrounds()
		}
	}
}
