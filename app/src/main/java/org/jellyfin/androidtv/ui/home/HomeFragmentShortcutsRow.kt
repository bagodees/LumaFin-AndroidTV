package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.HomeShortcut
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

class HomeShortcutRowItem(item: BaseItemDto, val available: Boolean, val watchLater: Boolean = false) : BaseItemDtoBaseRowItem(item) {
	override fun getSubText(context: Context): String? = if (available) null else context.getString(R.string.home_shortcut_unavailable)
}

class HomeFragmentShortcutsRow(private val items: List<HomeShortcutRowItem>, private val title: Int = R.string.home_shortcuts) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		if (items.isEmpty()) return
		val adapter = MutableObjectAdapter<BaseRowItem>(cardPresenter)
		items.forEach(adapter::add)
		rowsAdapter.add(ListRow(HeaderItem(context.getString(title)), adapter))
	}
}

// Resolve every target for the signed-in user rather than persisting artwork or permissions.
suspend fun loadHomeShortcuts(api: ApiClient, shortcuts: List<HomeShortcut>, libraries: Collection<BaseItemDto>, watchLater: Boolean = false): List<HomeShortcutRowItem> =
	withContext(Dispatchers.IO) {
		coroutineScope {
			shortcuts.map { shortcut -> async {
				val id = UUID.fromString(shortcut.id)
				val item = libraries.firstOrNull { it.id == id } ?: try {
					api.userLibraryApi.getItem(itemId = id).content
				} catch (error: CancellationException) {
					throw error
				} catch (_: Exception) {
					null
				}
				HomeShortcutRowItem(item ?: BaseItemDto(id = id, name = shortcut.name, type = shortcut.type), item != null, watchLater)
			} }.awaitAll()
		}
	}
