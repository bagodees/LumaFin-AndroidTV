package org.jellyfin.androidtv.ui.home

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest

class HomeFragmentHelper(
	private val context: Context,
	private val userRepository: UserRepository,
) {
	fun loadRecentlyAdded(userViews: Collection<BaseItemDto>): HomeFragmentRow {
		return HomeFragmentLatestRow(userRepository, userViews)
	}

	fun loadResume(title: String, includeMediaTypes: Collection<MediaType>): HomeFragmentRow {
		val query = GetResumeItemsRequest(
			limit = ITEM_LIMIT_RESUME,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
			mediaTypes = includeMediaTypes,
			excludeItemTypes = setOf(BaseItemKind.AUDIO_BOOK),
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, 0, true, true, arrayOf(ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback)))
	}

	fun loadResumeVideo(): HomeFragmentRow {
		return loadResume(context.getString(R.string.lbl_continue_watching), listOf(MediaType.VIDEO))
	}

	fun loadResumeAudio(): HomeFragmentRow {
		return loadResume(context.getString(R.string.continue_listening), listOf(MediaType.AUDIO))
	}

	fun loadLatestLiveTvRecordings(): HomeFragmentRow {
		val query = GetRecordingsRequest(
			fields = ItemRepository.itemFields,
			enableImages = true,
			limit = ITEM_LIMIT_RECORDINGS
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_recordings), query))
	}

	fun loadNextUp(): HomeFragmentRow {
		val query = GetNextUpRequest(
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_NEXT_UP,
			enableResumable = false,
			fields = ItemRepository.browseFields
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_next_up), query, arrayOf(ChangeTriggerType.TvPlayback)))
	}

	fun loadLatestMovies(): HomeFragmentRow {
		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(BaseItemKind.MOVIE),
			recursive = true,
			sortBy = setOf(ItemSortBy.PREMIERE_DATE),
			sortOrder = setOf(SortOrder.DESCENDING),
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_LATEST_BY_RELEASE,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.home_section_latest_movies), query, ITEM_LIMIT_LATEST_BY_RELEASE, false, true, arrayOf(ChangeTriggerType.LibraryUpdated)))
	}

	fun loadLatestShows(): HomeFragmentRow {
		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(BaseItemKind.SERIES),
			recursive = true,
			sortBy = setOf(ItemSortBy.PREMIERE_DATE),
			sortOrder = setOf(SortOrder.DESCENDING),
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_LATEST_BY_RELEASE,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.home_section_latest_shows), query, ITEM_LIMIT_LATEST_BY_RELEASE, false, true, arrayOf(ChangeTriggerType.LibraryUpdated)))
	}

	fun loadWatchAgain(): HomeFragmentRow {
		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			filters = setOf(ItemFilter.IS_PLAYED),
			sortBy = setOf(ItemSortBy.DATE_PLAYED),
			sortOrder = setOf(SortOrder.DESCENDING),
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_WATCH_AGAIN,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.home_section_watch_again), query, ITEM_LIMIT_WATCH_AGAIN, false, true, arrayOf(ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback)))
	}

	fun loadFavorites(): HomeFragmentRow {
		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.EPISODE, BaseItemKind.MUSIC_ALBUM, BaseItemKind.MUSIC_ARTIST),
			recursive = true,
			filters = setOf(ItemFilter.IS_FAVORITE),
			sortBy = setOf(ItemSortBy.SORT_NAME),
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_FAVORITES,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.home_section_favorites), query, ITEM_LIMIT_FAVORITES, false, true, arrayOf(ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate)))
	}

	fun loadRecentlyAddedMovies(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.MOVIE, context.getString(R.string.home_section_recently_added_movies))
	fun loadRecentlyAddedShows(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.SERIES, context.getString(R.string.home_section_recently_added_shows))
	fun loadRecentlyAddedAlbums(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.MUSIC_ALBUM, context.getString(R.string.home_section_recently_added_albums))
	fun loadRecentlyAddedArtists(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.MUSIC_ARTIST, context.getString(R.string.home_section_recently_added_artists))
	fun loadRecentlyAddedMusicVideos(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.MUSIC_VIDEO, context.getString(R.string.home_section_recently_added_music_videos))
	fun loadRecentlyAddedBooks(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.BOOK, context.getString(R.string.home_section_recently_added_books))
	fun loadRecentlyAddedAudiobooks(): HomeFragmentRow = loadRecentlyAddedByType(BaseItemKind.AUDIO_BOOK, context.getString(R.string.home_section_recently_added_audiobooks))

	fun loadLatestAlbums(): HomeFragmentRow = loadLatestByType(BaseItemKind.MUSIC_ALBUM, context.getString(R.string.home_section_latest_albums))
	fun loadLatestMusicVideos(): HomeFragmentRow = loadLatestByType(BaseItemKind.MUSIC_VIDEO, context.getString(R.string.home_section_latest_music_videos))
	fun loadLatestBooks(): HomeFragmentRow = loadLatestByType(BaseItemKind.BOOK, context.getString(R.string.home_section_latest_books))
	fun loadLatestAudiobooks(): HomeFragmentRow = loadLatestByType(BaseItemKind.AUDIO_BOOK, context.getString(R.string.home_section_latest_audiobooks))

	// "Latest X" sorts by release date; "Recently added X" sorts by date added to the library
	private fun loadLatestByType(itemType: BaseItemKind, title: String): HomeFragmentRow {
		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(itemType),
			recursive = true,
			sortBy = setOf(ItemSortBy.PREMIERE_DATE),
			sortOrder = setOf(SortOrder.DESCENDING),
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_LATEST_BY_RELEASE,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, ITEM_LIMIT_LATEST_BY_RELEASE, false, true, arrayOf(ChangeTriggerType.LibraryUpdated)))
	}

	private fun loadRecentlyAddedByType(itemType: BaseItemKind, title: String): HomeFragmentRow {
		val query = GetItemsRequest(
			fields = ItemRepository.browseFields,
			includeItemTypes = setOf(itemType),
			recursive = true,
			sortBy = setOf(ItemSortBy.DATE_CREATED),
			sortOrder = setOf(SortOrder.DESCENDING),
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_RECENTLY_ADDED,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, ITEM_LIMIT_RECENTLY_ADDED, false, true, arrayOf(ChangeTriggerType.LibraryUpdated)))
	}

	fun loadOnNow(): HomeFragmentRow {
		val query = GetRecommendedProgramsRequest(
			isAiring = true,
			fields = ItemRepository.itemFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
			limit = ITEM_LIMIT_ON_NOW
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_on_now), query))
	}

	companion object {
		// Maximum amount of items loaded for a row
		private const val ITEM_LIMIT_RESUME = 50
		private const val ITEM_LIMIT_RECORDINGS = 40
		private const val ITEM_LIMIT_NEXT_UP = 50
		private const val ITEM_LIMIT_ON_NOW = 20
		private const val ITEM_LIMIT_LATEST_BY_RELEASE = 50
		private const val ITEM_LIMIT_WATCH_AGAIN = 50
		private const val ITEM_LIMIT_RECENTLY_ADDED = 50
		private const val ITEM_LIMIT_FAVORITES = 50
	}
}
