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
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.androidtv.constant.ImageType as BaseRowImageType
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import java.util.UUID

/** [filter] is set for a pinned genre or studio (#96): its library and how to limit it. */
class HomeShortcutRowItem(
	item: BaseItemDto,
	val available: Boolean,
	val watchLater: Boolean = false,
	val filter: HomeShortcutFilter? = null,
	// Artwork for a pinned genre or studio, which usually has none of its own
	private val artwork: JellyfinImage? = null,
) : BaseItemDtoBaseRowItem(item) {
	override fun getImage(imageType: BaseRowImageType): JellyfinImage? = artwork ?: super.getImage(imageType)
	override fun getSubText(context: Context): String? = if (available) null else context.getString(R.string.home_shortcut_unavailable)
}

data class HomeShortcutFilter(val library: BaseItemDto, val title: String, val genre: String?, val studioId: String?, val favoritesOnly: Boolean = false)

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
				if (shortcut.libraryId != null) return@async loadFilterShortcut(api, shortcut, id, libraries)
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

/** A pinned genre or studio: available while its library is, shown with the genre's or studio's artwork. */
private suspend fun loadFilterShortcut(api: ApiClient, shortcut: HomeShortcut, id: UUID, libraries: Collection<BaseItemDto>): HomeShortcutRowItem {
	val library = libraries.firstOrNull { it.id.toString() == shortcut.libraryId }
	val entry = shortcut.entryId?.takeUnless { shortcut.favorites }?.let { entryId ->
		try {
			api.userLibraryApi.getItem(itemId = UUID.fromString(entryId)).content
		} catch (error: CancellationException) {
			throw error
		} catch (_: Exception) {
			null
		}
	}
	val item = entry?.copy(id = id, name = shortcut.name) ?: BaseItemDto(id = id, name = shortcut.name, type = shortcut.type)
	// A backdrop of its best-rated title in this library first: a genre's own server image is a collage
	// of titles from every library. A studio's own card or logo is the fallback.
	val artwork = library?.let { representativeBackdrop(api, it, shortcut) }
		?: entry?.let { it.itemImages[ImageType.THUMB] ?: it.itemImages[ImageType.PRIMARY] }
		// With nothing to show yet (no favorites, say), the library's own artwork
		?: library?.let { it.itemImages[ImageType.PRIMARY] ?: it.itemImages[ImageType.THUMB] }
	val filter = library?.let {
		val name = shortcut.filterName.orEmpty()
		if (shortcut.favorites) HomeShortcutFilter(it, shortcut.name, genre = null, studioId = null, favoritesOnly = true)
		else HomeShortcutFilter(it, name, genre = name.takeIf { shortcut.type == BaseItemKind.GENRE }, studioId = shortcut.entryId.takeIf { shortcut.type == BaseItemKind.STUDIO })
	}
	return HomeShortcutRowItem(item, available = filter != null, filter = filter, artwork = artwork)
}

private suspend fun representativeBackdrop(api: ApiClient, library: BaseItemDto, shortcut: HomeShortcut): JellyfinImage? = try {
	api.itemsApi.getItems(GetItemsRequest(
		parentId = library.id,
		includeItemTypes = setOf(if (library.collectionType == CollectionType.TVSHOWS) BaseItemKind.SERIES else BaseItemKind.MOVIE),
		recursive = true,
		genres = shortcut.filterName.takeIf { shortcut.type == BaseItemKind.GENRE }?.let(::setOf),
		isFavorite = true.takeIf { shortcut.favorites },
		studioIds = shortcut.entryId?.takeIf { shortcut.type == BaseItemKind.STUDIO }?.let { setOf(UUID.fromString(it)) },
		imageTypes = setOf(ImageType.BACKDROP),
		sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
		sortOrder = setOf(SortOrder.DESCENDING),
		enableImageTypes = setOf(ImageType.BACKDROP),
		imageTypeLimit = 1,
		limit = 1,
	)).content.items.firstOrNull()?.itemBackdropImages?.firstOrNull()
} catch (error: CancellationException) {
	throw error
} catch (_: Exception) {
	null
}
