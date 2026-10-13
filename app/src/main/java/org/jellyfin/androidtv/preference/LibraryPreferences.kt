package org.jellyfin.androidtv.preference

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.preference.store.DisplayPreferencesStore
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder

class LibraryPreferences(
	displayPreferencesId: String,
	api: ApiClient,
) : DisplayPreferencesStore(
	displayPreferencesId = displayPreferencesId,
	api = api,
) {
	private val browsingSaveMutex = Mutex()

	fun saveBrowsingChoices(favoritesOnly: Boolean, unwatchedOnly: Boolean, sort: ItemSortBy, order: SortOrder) {
		this[filterFavoritesOnly] = favoritesOnly
		this[filterUnwatchedOnly] = unwatchedOnly
		this[sortBy] = sort
		this[sortOrder] = order
		// Keep the write alive when the library screen closes, and serialize rapid changes.
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			browsingSaveMutex.withLock { commit() }
		}
	}

	companion object {
		val posterSize = enumPreference("PosterSize", PosterSize.MED)
		val imageType = enumPreference("ImageType", ImageType.POSTER)
		val gridDirection = enumPreference("GridDirection", GridDirection.HORIZONTAL)
		val enableSmartScreen = booleanPreference("SmartScreen", false)

		// Filters
		val filterFavoritesOnly = booleanPreference("FilterFavoritesOnly", false)
		val filterUnwatchedOnly = booleanPreference("FilterUnwatchedOnly", false)

		// Item sorting
		val sortBy = enumPreference("SortBy", ItemSortBy.SORT_NAME)
		val sortOrder = enumPreference("SortOrder", SortOrder.ASCENDING)
	}
}
