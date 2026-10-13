package org.jellyfin.androidtv.ui.itemhandling

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.apiclient.EmptyResponse
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.core.context.GlobalContext
import java.util.UUID

internal fun seriesFocusIndex(items: List<BaseItemDto>, savedId: UUID?, savedIndex: Int, played: BaseItemDto?): Int? {
	if (items.isEmpty()) return null
	val playedId = if (items.first().type == BaseItemKind.SEASON) played?.seasonId else played?.id
	items.indexOfFirst { it.id == playedId }.takeIf { it >= 0 }?.let { return it }
	items.indexOfFirst { it.id == savedId }.takeIf { it >= 0 }?.let { return it }
	if (savedId != null) return savedIndex.coerceIn(items.indices)
	return items.indexOfFirst { it.userData?.played == false }.takeIf { it >= 0 } ?: 0
}

/** Restore one relevant series/season row; never change another series or steal focus from its controls. */
class SeriesBrowseFocus(
	private val fragment: Fragment,
	private val rowsFragment: () -> RowsSupportFragment?,
	private val rows: () -> MutableObjectAdapter<Row>?,
	private val folder: () -> BaseItemDto?,
) {
	private var savedId: UUID? = null
	private var savedIndex = 0
	private var pending = true
	private var restoring = false
	private var playbackBeforeLeaving: UUID? = null
	private var playbackOnReturn: BaseItemDto? = null

	fun save(state: Bundle) {
		state.putString("series_focus_id", savedId?.toString())
		state.putInt("series_focus_index", savedIndex)
	}
	fun read(state: Bundle?) {
		savedId = state?.getString("series_focus_id")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
		savedIndex = state?.getInt("series_focus_index") ?: 0
	}
	fun leaving() {
		playbackBeforeLeaving = GlobalContext.get().get<DataRefreshService>().lastPlayedItem?.id
	}
	fun returning() {
		val scope = folder() ?: return
		if (scope.type !in setOf(BaseItemKind.SERIES, BaseItemKind.SEASON)) return
		val played = GlobalContext.get().get<DataRefreshService>().lastPlayedItem
		playbackOnReturn = played?.takeIf {
			it.id != playbackBeforeLeaving && it.type == BaseItemKind.EPISODE &&
				(if (scope.type == BaseItemKind.SERIES) it.seriesId == scope.id else it.seasonId == scope.id)
		}
		pending = true
		restore()
	}
	fun selected(item: BaseRowItem?, row: ListRow?) {
		if (pending || restoring || item?.baseItem?.type !in setOf(BaseItemKind.SEASON, BaseItemKind.EPISODE)) return
		savedId = item?.baseItem?.id
		savedIndex = row?.adapter?.let { adapter -> (0 until adapter.size()).firstOrNull { adapter[it] == item } } ?: 0
	}
	fun observe(adapter: ItemRowAdapter) {
		adapter.setRetrieveFinishedListener(object : EmptyResponse(fragment.lifecycle) {
			override fun onResponse() {
				val currentRows = rows()
				val position = rowsFragment()?.selectedPosition ?: -1
				val selectedRow = if (currentRows != null && position in 0 until currentRows.size()) currentRows[position] as? ListRow else null
				if (pending || (savedId != null && selectedRow?.adapter === adapter)) {
					pending = true
					restore()
				}
			}
		})
	}
	private fun restore() {
		if (!fragment.isResumed || !pending) return
		val scope = folder() ?: return
		val expected = when (scope.type) {
			BaseItemKind.SERIES -> BaseItemKind.SEASON
			BaseItemKind.SEASON -> BaseItemKind.EPISODE
			else -> return
		}
		val adapter = rows() ?: return
		for (rowIndex in 0 until adapter.size()) {
			val row = adapter[rowIndex] as? ListRow ?: continue
			val items = (0 until row.adapter.size()).mapNotNull { (row.adapter[it] as? BaseRowItem)?.baseItem }
			if (items.isEmpty() || items.first().type != expected) continue
			val index = seriesFocusIndex(items, savedId, savedIndex, playbackOnReturn) ?: continue
			savedId = items[index].id
			savedIndex = index
			pending = false
			restoring = true
			rowsFragment()?.setSelectedPosition(rowIndex, false, ListRowPresenter.SelectItemViewHolderTask(index))
			rowsFragment()?.view?.post { restoring = false }
			return
		}
	}
}
