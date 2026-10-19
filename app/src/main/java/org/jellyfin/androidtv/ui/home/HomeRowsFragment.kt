package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import android.widget.PopupMenu
import org.jellyfin.androidtv.util.addHomeShortcutMenu
import org.jellyfin.androidtv.preference.HomeShortcut
import org.jellyfin.androidtv.preference.HomeShortcutPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.constant.LiveTvOption
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.RecommendationPreferences
import androidx.appcompat.app.AlertDialog
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.browsing.CompositeClickedListener
import org.jellyfin.androidtv.ui.browsing.CompositeSelectedListener
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.itemhandling.refreshItem
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.util.KeyProcessor
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.api.LibraryChangedMessage
import org.jellyfin.sdk.model.api.UserDataChangedMessage
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class HomeRowsFragment : RowsSupportFragment(), AudioEventListener, View.OnKeyListener {
	private val api by inject<ApiClient>()
	private val backgroundService by inject<BackgroundService>()
	private val playbackManager by inject<PlaybackManager>()
	private val mediaManager by inject<MediaManager>()
	private val notificationsRepository by inject<NotificationsRepository>()
	private val userRepository by inject<UserRepository>()
	private val userSettingPreferences by inject<UserSettingPreferences>()
	private val recommendationPreferences by inject<RecommendationPreferences>()
	private val shortcutPreferences by inject<HomeShortcutPreferences>()
	private val watchLaterPreferences by inject<org.jellyfin.androidtv.preference.WatchLaterPreferences>()
	private var loadedWatchLater: List<HomeShortcut> = emptyList()
	private var homeLoadJob: Job? = null
	private var loadedShortcuts: List<HomeShortcut> = emptyList()
	private val userViewsRepository by inject<UserViewsRepository>()
	private val dataRefreshService by inject<DataRefreshService>()
	private val customMessageRepository by inject<CustomMessageRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val itemLauncher by inject<ItemLauncher>()
	private val keyProcessor by inject<KeyProcessor>()

	private val helper by lazy { HomeFragmentHelper(requireContext(), userRepository) }

	// Data
	private var currentItem: BaseRowItem? = null
	private var currentRow: ListRow? = null
	private var justLoaded = true
	private var handledPlaybackRevision = 0L
	private var loadedHomeSections: List<HomeSectionType> = emptyList()
	private var loadedLatestMoviesLimit = 50
	private var loadedLatestShowsLimit = 50
	private var loadedRecentlyAddedMoviesLimit = 50
	private var loadedRecentlyAddedShowsLimit = 50
	private var loadedHiddenRecommendations: Map<String, String> = emptyMap()
	private var loadedHideWatchedLatestMovies = false
	private var loadedHideWatchedRecentlyAddedMovies = false

	private fun homePreferencesChanged() = watchLaterPreferences.items != loadedWatchLater || shortcutPreferences.shortcuts != loadedShortcuts || recommendationPreferences.hiddenRows != loadedHiddenRecommendations || userSettingPreferences.activeHomesections != loadedHomeSections ||
		userSettingPreferences[UserSettingPreferences.latestMoviesItemLimit] != loadedLatestMoviesLimit ||
		userSettingPreferences[UserSettingPreferences.latestShowsItemLimit] != loadedLatestShowsLimit ||
		userSettingPreferences[UserSettingPreferences.recentlyAddedMoviesItemLimit] != loadedRecentlyAddedMoviesLimit ||
		userSettingPreferences[UserSettingPreferences.recentlyAddedShowsItemLimit] != loadedRecentlyAddedShowsLimit ||
		userSettingPreferences[UserSettingPreferences.hideWatchedLatestMovies] != loadedHideWatchedLatestMovies ||
		userSettingPreferences[UserSettingPreferences.hideWatchedRecentlyAddedMovies] != loadedHideWatchedRecentlyAddedMovies

	// Special rows
	private val notificationsRow by lazy { NotificationsHomeFragmentRow(lifecycleScope, notificationsRepository) }
	private val nowPlaying by lazy { HomeFragmentNowPlayingRow(lifecycleScope, playbackManager, mediaManager) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = MutableObjectAdapter<Row>(PositionableListRowPresenter())

		loadHomeRows()

		onItemViewClickedListener = CompositeClickedListener().apply {
			registerListener(ItemViewClickedListener())
			registerListener(notificationsRow::onItemClicked)
		}

		onItemViewSelectedListener = CompositeSelectedListener().apply {
			registerListener(ItemViewSelectedListener())
		}

		customMessageRepository.message
			.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { message ->
				when (message) {
					CustomMessage.RefreshCurrentItem -> refreshCurrentItem()
					CustomMessage.HomeShortcutsChanged -> loadHomeRows()
					else -> Unit
				}
			}.launchIn(lifecycleScope)

		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				// Receive completion both after returning and if reporting finishes late.
				dataRefreshService.playbackUpdates
					.onEach { revision ->
						if (revision > handledPlaybackRevision) {
							handledPlaybackRevision = revision
							// Empty rows are removed from the visible adapter, so refreshing
							// existing rows cannot bring Next up back. Recreate all configured
							// rows in their saved order after the server finishes the report.
							loadHomeRows()
						}
					}
					.launchIn(this)

				api.webSocket.subscribe<UserDataChangedMessage>()
					.onEach { refreshRows(force = true, delayed = false) }
					.launchIn(this)

				api.webSocket.subscribe<LibraryChangedMessage>()
					.onEach { refreshRows(force = true, delayed = false) }
					.launchIn(this)
			}
		}

		// Subscribe to Audio messages
		mediaManager.addAudioEventListener(this)
	}

	/**
	 * Rebuilds the home rows if the home section preferences (order/selection) have changed
	 * since they were last loaded. Called when the settings dialog is dismissed, since that
	 * dialog is an overlay within this same fragment's lifecycle - onResume() never fires for it.
	 */
	fun reloadHomeRowsIfSectionsChanged() {
		lifecycleScope.launch(Dispatchers.IO) {
			val sectionsChanged = homePreferencesChanged()
			if (sectionsChanged) withContext(Dispatchers.Main) { loadHomeRows() }
		}
	}

	private fun loadHomeRows() {
		homeLoadJob?.cancel()
		cancelRowPages()
		homeLoadJob = lifecycleScope.launch(Dispatchers.IO) {
			val currentUser = withTimeout(30.seconds) {
				userRepository.currentUser.filterNotNull().first()
			}

			val shortcutScope = shortcutPreferences.scope
			loadedShortcuts = shortcutPreferences.shortcuts
			loadedWatchLater = watchLaterPreferences.items
			val shortcuts = if (loadedShortcuts.isEmpty()) emptyList() else loadHomeShortcuts(api, loadedShortcuts, userViewsRepository.views.first())

			// Start out with default sections
			val homesections = userSettingPreferences.activeHomesections
			loadedHomeSections = homesections
			loadedHiddenRecommendations = recommendationPreferences.hiddenRows
			loadedLatestMoviesLimit = userSettingPreferences[UserSettingPreferences.latestMoviesItemLimit]
			loadedLatestShowsLimit = userSettingPreferences[UserSettingPreferences.latestShowsItemLimit]
			loadedRecentlyAddedMoviesLimit = userSettingPreferences[UserSettingPreferences.recentlyAddedMoviesItemLimit]
			loadedRecentlyAddedShowsLimit = userSettingPreferences[UserSettingPreferences.recentlyAddedShowsItemLimit]
			loadedHideWatchedLatestMovies = userSettingPreferences[UserSettingPreferences.hideWatchedLatestMovies]
			loadedHideWatchedRecentlyAddedMovies = userSettingPreferences[UserSettingPreferences.hideWatchedRecentlyAddedMovies]

			// Make sure the rows are empty
			val rows = mutableListOf<HomeFragmentRow>(HomeFragmentShortcutsRow(shortcuts))

			// Check for coroutine cancellation
			if (!isActive) return@launch

			// Actually add the sections
			for (section in homesections) when (section) {
				HomeSectionType.LATEST_MEDIA -> rows.add(helper.loadRecentlyAdded(userViewsRepository.views.first(), loadedRecentlyAddedMoviesLimit, loadedRecentlyAddedShowsLimit, loadedHideWatchedRecentlyAddedMovies))
				HomeSectionType.LIBRARY_TILES_SMALL -> rows.add(HomeFragmentViewsRow(small = false))
				HomeSectionType.LIBRARY_BUTTONS -> rows.add(HomeFragmentViewsRow(small = true))
				HomeSectionType.RESUME -> rows.add(helper.loadResumeVideo())
				HomeSectionType.RESUME_AUDIO -> rows.add(helper.loadResumeAudio())
				HomeSectionType.RESUME_BOOK -> Unit // Books are not (yet) supported
				HomeSectionType.ACTIVE_RECORDINGS -> rows.add(helper.loadLatestLiveTvRecordings())
				HomeSectionType.NEXT_UP -> rows.add(helper.loadNextUp())
				HomeSectionType.LIVE_TV -> if (currentUser.policy?.enableLiveTvAccess == true) {
					rows.add(HomeFragmentLiveTVRow(requireActivity(), userRepository))
					rows.add(helper.loadOnNow())
				}

				HomeSectionType.LATEST_MOVIES -> rows.add(helper.loadLatestMovies(loadedLatestMoviesLimit, loadedHideWatchedLatestMovies))
				HomeSectionType.LATEST_SHOWS -> rows.add(helper.loadLatestShows(loadedLatestShowsLimit))
				HomeSectionType.BECAUSE_YOU_WATCHED -> rows.add(HomeFragmentBecauseYouWatchedRow(api, loadedHiddenRecommendations))
				HomeSectionType.COLLECTIONS -> rows.add(HomeFragmentCollectionsRow(api, userViewsRepository.views.first()))
				HomeSectionType.WATCH_AGAIN -> rows.add(helper.loadWatchAgain())
				HomeSectionType.GENRES -> rows.add(HomeFragmentGenreRow(api))
				HomeSectionType.CONTINUE_WATCHING_NEXT_UP -> rows.add(HomeFragmentContinueWatchingNextUpRow(api))
				HomeSectionType.WATCH_LATER -> rows.add(HomeFragmentShortcutsRow(loadHomeShortcuts(api, loadedWatchLater, emptyList(), true), R.string.watch_later))
				HomeSectionType.FAVORITES -> rows.add(helper.loadFavorites())
				HomeSectionType.RECENTLY_ADDED_MOVIES -> rows.add(helper.loadRecentlyAddedMovies(loadedRecentlyAddedMoviesLimit, loadedHideWatchedRecentlyAddedMovies))
				HomeSectionType.RECENTLY_ADDED_SHOWS -> rows.add(helper.loadRecentlyAddedShows(loadedRecentlyAddedShowsLimit))
				HomeSectionType.RECENTLY_ADDED_ALBUMS -> rows.add(helper.loadRecentlyAddedAlbums())
				HomeSectionType.RECENTLY_ADDED_ARTISTS -> rows.add(helper.loadRecentlyAddedArtists())
				HomeSectionType.RECENTLY_ADDED_MUSIC_VIDEOS -> rows.add(helper.loadRecentlyAddedMusicVideos())
				HomeSectionType.RECENTLY_ADDED_BOOKS -> rows.add(helper.loadRecentlyAddedBooks())
				HomeSectionType.RECENTLY_ADDED_AUDIOBOOKS -> rows.add(helper.loadRecentlyAddedAudiobooks())
				HomeSectionType.LATEST_ALBUMS -> rows.add(helper.loadLatestAlbums())
				HomeSectionType.LATEST_MUSIC_VIDEOS -> rows.add(helper.loadLatestMusicVideos())
				HomeSectionType.LATEST_BOOKS -> rows.add(helper.loadLatestBooks())
				HomeSectionType.LATEST_AUDIOBOOKS -> rows.add(helper.loadLatestAudiobooks())

				HomeSectionType.NONE -> Unit
			}

			// Add sections to layout
			withContext(Dispatchers.Main) {
				if (shortcutScope != shortcutPreferences.scope) return@withContext
				@Suppress("UNCHECKED_CAST")
				val rowsAdapter = adapter as MutableObjectAdapter<Row>
				val cardPresenter = CardPresenter.homeRows()

				// Clear any rows from a previous load (e.g. rebuilding after a home section preference change)
				rowsAdapter.clear()
				currentItem = null
				currentRow = null

				// Add rows in order
				notificationsRow.addToRowsAdapter(requireContext(), cardPresenter, rowsAdapter)
				nowPlaying.addToRowsAdapter(requireContext(), cardPresenter, rowsAdapter)
				for (row in rows) row.addToRowsAdapter(requireContext(), cardPresenter, rowsAdapter)

				// Wire up Live TV sibling rows so the On Now row removes the buttons row when empty
				for (i in 0 until rowsAdapter.size()) {
					val listRow = rowsAdapter.get(i) as? ListRow ?: continue
					val itemAdapter = listRow.adapter as? ItemRowAdapter ?: continue
					if (itemAdapter.queryType == QueryType.LiveTvProgram && i > 0) {
						val previousRow = rowsAdapter.get(i - 1)
						if (previousRow != null) itemAdapter.setSiblingRow(previousRow)
					}
				}
			}
		}
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		if (event?.action != KeyEvent.ACTION_UP) return false
		val row = currentRow
		val header = row?.headerItem as? RecommendationHeaderItem
		val item = currentItem
		if (keyCode == KeyEvent.KEYCODE_MENU && item is HomeShortcutRowItem && !item.available) {
			val menu = PopupMenu(requireContext(), v ?: requireView())
			item.baseItem?.let { if (item.watchLater) org.jellyfin.androidtv.util.addWatchLaterMenu(menu, requireActivity(), it) else addHomeShortcutMenu(menu, requireActivity(), it) }
			menu.menu.add(R.string.home_shortcut_retry).setOnMenuItemClickListener { loadHomeRows(); true }
			menu.show()
			return true
		}
		if (keyCode == KeyEvent.KEYCODE_MENU && header != null && item?.baseItem != null) {
			val menu = keyProcessor.createItemMenu(item, item.baseItem?.userData, requireActivity())
			menu.menu.add(0, 1001, 0, R.string.recommendation_why).setOnMenuItemClickListener {
				AlertDialog.Builder(requireContext())
					.setTitle(getString(R.string.because_you_watched, header.seedName))
					.setMessage(getString(R.string.recommendation_explanation, header.seedName))
					.setPositiveButton(android.R.string.ok, null)
					.show()
				true
			}
			menu.menu.add(0, 1002, 0, R.string.recommendation_hide).setOnMenuItemClickListener {
				recommendationPreferences.hide(header.seedId, header.seedName)
				loadedHiddenRecommendations = recommendationPreferences.hiddenRows
				@Suppress("UNCHECKED_CAST")
				val rows = adapter as MutableObjectAdapter<Row>
				val index = rows.indexOf(row)
				rows.remove(row)
				currentItem = null
				currentRow = null
				if (rows.size() > 0) setSelectedPosition(index.coerceIn(0, rows.size() - 1))
				android.widget.Toast.makeText(requireContext(), R.string.recommendation_hidden, android.widget.Toast.LENGTH_LONG).show()
				true
			}
			return true
		}
		return keyProcessor.handleKey(keyCode, currentItem, activity)
	}

	override fun onResume() {
		super.onResume()

		//React to deletion
		if (currentRow != null && currentItem != null && currentItem?.baseItem != null && currentItem!!.baseItem!!.id == dataRefreshService.lastDeletedItemId) {
			@Suppress("UNCHECKED_CAST")
			val rowItems = currentRow!!.adapter as? MutableObjectAdapter<Any>
			rowItems?.remove(currentItem!!)
			currentItem = null
			dataRefreshService.lastDeletedItemId = null
		}

		if (!justLoaded) {
			lifecycleScope.launch(Dispatchers.IO) {
				val sectionsChanged = homePreferencesChanged()

				withContext(Dispatchers.Main) {
					if (sectionsChanged) {
						// Home section preferences changed (e.g. reordered in Settings) - rebuild from scratch
						loadHomeRows()
					} else {
						// Removed empty rows have no adapter left to refresh. Returning
						// from details or playback must query every configured section.
						loadHomeRows()
					}
				}
			}
		} else {
			justLoaded = false
		}

		// Update audio queue
		Timber.i("Updating audio queue in HomeFragment (onResume)")
		nowPlaying.update(requireContext(), adapter as MutableObjectAdapter<Row>)
	}

	override fun onQueueStatusChanged(hasQueue: Boolean) {
		if (activity == null || requireActivity().isFinishing) return

		Timber.i("Updating audio queue in HomeFragment (onQueueStatusChanged)")
		nowPlaying.update(requireContext(), adapter as MutableObjectAdapter<Row>)
	}

	private fun refreshRows(force: Boolean = false, delayed: Boolean = true) {
		if (force) {
			// Server watch-status changes can make a previously empty row visible.
			loadHomeRows()
			return
		}
		lifecycleScope.launch {
			if (delayed) delay(1.5.seconds)

			repeat(adapter.size()) { i ->
				val rowAdapter = (adapter[i] as? ListRow)?.adapter as? ItemRowAdapter
				if (force) rowAdapter?.Retrieve()
				else rowAdapter?.ReRetrieveIfNeeded()
			}
		}
	}

	private fun refreshCurrentItem() {
		val adapter = currentRow?.adapter as? ItemRowAdapter ?: return
		val item = currentItem ?: return

		Timber.i("Refresh item ${item.getFullName(requireContext())}")
		adapter.refreshItem(api, this, item)
	}

	private fun cancelRowPages() {
		for (index in 0 until (adapter?.size() ?: 0)) {
			((adapter[index] as? ListRow)?.adapter as? ItemRowAdapter)?.pageRequest?.cancel()
		}
	}

	override fun onDestroy() {
		homeLoadJob?.cancel()
		cancelRowPages()
		super.onDestroy()

		mediaManager.removeAudioEventListener(this)
	}

	private inner class ItemViewClickedListener : OnItemViewClickedListener {
		override fun onItemClicked(
			itemViewHolder: Presenter.ViewHolder?,
			item: Any?,
			rowViewHolder: RowPresenter.ViewHolder?,
			row: Row?,
		) {
			if (item is GridButton) {
				when (item.id) {
					LiveTvOption.LIVE_TV_GUIDE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvGuide)
					LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSchedule)
					LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvRecordings)
					LiveTvOption.LIVE_TV_SERIES_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSeriesRecordings)
				}
			}

			if (item is GenreTile) {
				navigationRepository.navigate(Destinations.genreDetails(item.name))
				return
			}

			if (item is HomeShortcutRowItem && !item.available) {
				android.widget.Toast.makeText(requireContext(), if (item.watchLater) R.string.watch_later_unavailable_hint else R.string.home_shortcut_unavailable_hint, android.widget.Toast.LENGTH_LONG).show()
				return
			}
			if (item !is BaseRowItem) return
			if (row !is ListRow) return
			@Suppress("UNCHECKED_CAST")
			itemLauncher.launch(item, row.adapter as MutableObjectAdapter<Any>, requireContext())
		}
	}

	private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
		override fun onItemSelected(
			itemViewHolder: Presenter.ViewHolder?,
			item: Any?,
			rowViewHolder: RowPresenter.ViewHolder?,
			row: Row?,
		) {
			if (item !is BaseRowItem) {
				currentItem = null
				//fill in default background
				backgroundService.clearBackgrounds()
			} else {
				currentItem = item
				currentRow = row as ListRow

				val itemRowAdapter = row.adapter as? ItemRowAdapter
				itemRowAdapter?.loadMoreItemsIfNeeded(itemRowAdapter.indexOf(item))

				backgroundService.setBackground(item.baseItem)
			}
		}
	}
}
