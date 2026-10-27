package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import android.widget.PopupMenu
import org.jellyfin.androidtv.util.addHomeShortcutMenu
import org.jellyfin.androidtv.preference.HomeShortcut
import org.jellyfin.androidtv.preference.HomeShortcutPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
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
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
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
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.CardShape
import org.jellyfin.androidtv.preference.constant.HomeStyle
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import java.io.IOException
import org.jellyfin.androidtv.util.KeyProcessor
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.api.LibraryChangedMessage
import org.jellyfin.sdk.model.api.UserDataChangedMessage
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/** Card height in dp for the top bar style's landscape Continue Watching row (other landscape rows use 150). */
private const val TOP_BAR_LARGE_CARD_HEIGHT = 168
/** Card height in dp for portrait Home rows. */
private const val PORTRAIT_CARD_HEIGHT = 168

class HomeRowsFragment : RowsSupportFragment(), AudioEventListener, View.OnKeyListener {
	private val userPreferences by inject<UserPreferences>()
	private val api by inject<ApiClient>()
	private val sessionRepository by inject<SessionRepository>()
	private var homeLoadGeneration = 0L
	private var homeErrorShown = false
	private var homeErrorDialog: LumaFinMenuDialog? = null
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
	/**
	 * Space between rows in dp. Each row keeps Leanback's padding (re-applied on every selection) and
	 * captioned cards reserve two lines, so the classic Home overlaps that empty space.
	 */
	var rowSpacingDp = -20
		set(value) {
			field = value
			applyRowSpacing()
		}

	private fun applyRowSpacing() {
		verticalGridView?.verticalSpacing = (rowSpacingDp * resources.displayMetrics.density).toInt()
	}

	/** The focused card's item, shown by the new Home's preview area. */
	val focusedItem = MutableStateFlow<BaseItemDto?>(null)
	/** Position of the focused card within its row, or -1; Left leaves the rows only from the first card. */
	var focusedItemIndex = -1
		private set
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
	private var loadedHomeStyle = HomeStyle.SIDE_MENU
	private var loadedContinueWatchingShape = CardShape.LANDSCAPE
	private var loadedHomeRowsShape = CardShape.LANDSCAPE

	private fun homePreferencesChanged() = watchLaterPreferences.items != loadedWatchLater || shortcutPreferences.shortcuts != loadedShortcuts || recommendationPreferences.hiddenRows != loadedHiddenRecommendations || userSettingPreferences.activeHomesections != loadedHomeSections ||
		userSettingPreferences[UserSettingPreferences.latestMoviesItemLimit] != loadedLatestMoviesLimit ||
		userSettingPreferences[UserSettingPreferences.latestShowsItemLimit] != loadedLatestShowsLimit ||
		userSettingPreferences[UserSettingPreferences.recentlyAddedMoviesItemLimit] != loadedRecentlyAddedMoviesLimit ||
		userSettingPreferences[UserSettingPreferences.recentlyAddedShowsItemLimit] != loadedRecentlyAddedShowsLimit ||
		userSettingPreferences[UserSettingPreferences.hideWatchedLatestMovies] != loadedHideWatchedLatestMovies ||
		userSettingPreferences[UserSettingPreferences.hideWatchedRecentlyAddedMovies] != loadedHideWatchedRecentlyAddedMovies ||
		userPreferences[UserPreferences.homeStyle] != loadedHomeStyle ||
		userPreferences[UserPreferences.continueWatchingCardShape] != loadedContinueWatchingShape ||
		userPreferences[UserPreferences.homeRowsCardShape] != loadedHomeRowsShape

	// Special rows
	private val notificationsRow by lazy { NotificationsHomeFragmentRow(lifecycleScope, notificationsRepository) }
	private val nowPlaying by lazy { HomeFragmentNowPlayingRow(lifecycleScope, playbackManager, mediaManager) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = RecoveringHomeRowsAdapter(lifecycleScope, lifecycle, ::onHomeLoadFailure)

		// Session publication and preference restoration can finish after this fragment
		// is created. Restart for the ready session instead of abandoning a stale load.
		combine(sessionRepository.currentSession, sessionRepository.state) { session, state ->
			session.takeIf { state == SessionRepositoryState.READY }
		}.distinctUntilChanged().onEach { session ->
			if (session != null) {
				loadHomeRows()
			} else {
				homeLoadGeneration++
				homeLoadJob?.cancel()
				(adapter as RecoveringHomeRowsAdapter).cancelRequests()
				cancelRowPages()
				homeErrorDialog?.dismiss()
			}
		}.launchIn(lifecycleScope)

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
		val session = sessionRepository.currentSession.value ?: return
		if (sessionRepository.state.value != SessionRepositoryState.READY) return
		homeLoadJob?.cancel()
		cancelRowPages()
		val generation = ++homeLoadGeneration
		val current = {
			generation == homeLoadGeneration && session == sessionRepository.currentSession.value &&
				sessionRepository.state.value == SessionRepositoryState.READY
		}
		homeErrorShown = false
		homeErrorDialog?.dismiss()
		homeErrorDialog = null
		(adapter as RecoveringHomeRowsAdapter).beginLoad(current)
		homeLoadJob = lifecycleScope.launch(Dispatchers.IO) {
			try {
				recoverHomeLoad(current) { buildHomeRows(current) }
			} catch (cancelled: CancellationException) {
				throw cancelled
			} catch (failure: Exception) {
				withContext(Dispatchers.Main) { if (current()) onHomeLoadFailure(failure) }
			}
		}
	}

	private suspend fun buildHomeRows(current: () -> Boolean) {
		val currentUser = withTimeoutOrNull(30.seconds) {
			userRepository.currentUser.filterNotNull().first()
		} ?: throw IOException("Home user not ready")

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
		// The top bar Home style shows larger Continue Watching cards and Latest Movies as posters
		loadedHomeStyle = userPreferences[UserPreferences.homeStyle]
		val topBar = userPreferences[UserPreferences.newHomeEnabled] && loadedHomeStyle == HomeStyle.TOP_BAR
		// Card style settings (#123): Continue Watching and Up Next follow one choice, every other row the other
		loadedContinueWatchingShape = userPreferences[UserPreferences.continueWatchingCardShape]
		loadedHomeRowsShape = userPreferences[UserPreferences.homeRowsCardShape]
		fun presenterFor(shape: CardShape, continueWatching: Boolean) = when {
			shape == CardShape.PORTRAIT -> CardPresenter.homeRows(PORTRAIT_CARD_HEIGHT, portrait = true)
			// The top bar style shows larger landscape Continue Watching cards
			continueWatching && topBar -> CardPresenter.homeRows(TOP_BAR_LARGE_CARD_HEIGHT, portrait = false)
			else -> null
		}
		val continueWatchingPresenter = presenterFor(loadedContinueWatchingShape, continueWatching = true)
		val otherRowsPresenter = presenterFor(loadedHomeRowsShape, continueWatching = false)
		val continueWatchingRows = mutableSetOf<HomeFragmentRow>()
		fun MutableList<HomeFragmentRow>.addContinueWatching(row: HomeFragmentRow) { add(row); continueWatchingRows += row }

		// Check for coroutine cancellation
		if (!current()) throw CancellationException("Home load replaced")

		// Actually add the sections
		for (section in homesections) when (section) {
			HomeSectionType.LATEST_MEDIA -> rows.add(helper.loadRecentlyAdded(userViewsRepository.views.first(), loadedRecentlyAddedMoviesLimit, loadedRecentlyAddedShowsLimit, loadedHideWatchedRecentlyAddedMovies))
			HomeSectionType.LIBRARY_TILES_SMALL -> rows.add(HomeFragmentViewsRow(small = false))
			HomeSectionType.LIBRARY_BUTTONS -> rows.add(HomeFragmentViewsRow(small = true))
			HomeSectionType.RESUME -> rows.addContinueWatching(helper.loadResumeVideo())
			HomeSectionType.RESUME_AUDIO -> rows.add(helper.loadResumeAudio())
			HomeSectionType.RESUME_BOOK -> Unit // Books are not (yet) supported
			HomeSectionType.ACTIVE_RECORDINGS -> rows.add(helper.loadLatestLiveTvRecordings())
			HomeSectionType.NEXT_UP -> rows.addContinueWatching(helper.loadNextUp())
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
			HomeSectionType.CONTINUE_WATCHING_NEXT_UP -> rows.addContinueWatching(HomeFragmentContinueWatchingNextUpRow(api))
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
			if (!current() || shortcutScope != shortcutPreferences.scope) throw CancellationException("Home account changed")
			@Suppress("UNCHECKED_CAST")
			val rowsAdapter = adapter as MutableObjectAdapter<Row>
			val cardPresenter = CardPresenter.homeRows()

			(rowsAdapter as RecoveringHomeRowsAdapter).beginLoad(current)

			// Clear any rows from a previous load (e.g. rebuilding after a home section preference change)
			rowsAdapter.clear()
			currentItem = null
			currentRow = null

			// Add rows in order
			notificationsRow.addToRowsAdapter(requireContext(), cardPresenter, rowsAdapter)
			nowPlaying.addToRowsAdapter(requireContext(), cardPresenter, rowsAdapter)
			for (row in rows) {
				// The shortcut, library and Live TV button rows keep their own cards
				val styled = row !is HomeFragmentShortcutsRow && row !is HomeFragmentViewsRow && row !is HomeFragmentLiveTVRow
				val presenter = if (row in continueWatchingRows) continueWatchingPresenter else if (styled) otherRowsPresenter else null
				row.addToRowsAdapter(requireContext(), presenter ?: cardPresenter, rowsAdapter)
			}

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

	private fun onHomeLoadFailure(failure: Exception) {
		Timber.w(failure, "Home loading failed after recovery attempts")
		val generation = homeLoadGeneration
		val session = sessionRepository.currentSession.value
		lifecycleScope.launch {
			lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
			if (generation != homeLoadGeneration || session != sessionRepository.currentSession.value || homeErrorShown || !isAdded) return@launch
			homeErrorShown = true
			homeErrorDialog = LumaFinMenuDialog(requireContext(), viewLifecycleOwner,
                getString(R.string.home_load_failed_title), getString(R.string.home_load_failed_message), fitContent = true).apply {
                button(getString(android.R.string.cancel)) { dismiss() }
                button(getString(R.string.home_shortcut_retry), primary = true) {
                    dismiss()
                    if (isAdded && generation == homeLoadGeneration && session == sessionRepository.currentSession.value) loadHomeRows()
                }
                onDismiss { homeErrorDialog = null }
                show()
            }
		}
	}

	override fun onStop() {
		homeErrorDialog?.dismiss()
		super.onStop()
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
				LumaFinMenuDialog(requireContext(), viewLifecycleOwner, getString(R.string.because_you_watched, header.seedName),
                    getString(R.string.recommendation_explanation, header.seedName), fitContent = true).apply {
                    button(getString(android.R.string.ok), primary = true) { dismiss() }
                    show()
                }
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

	private fun restoreFocusedBackdrop() {
		val grid = verticalGridView ?: return
		val focused = grid.findFocus() ?: return
		val rowHolder = grid.findContainingViewHolder(focused) as? ItemBridgeAdapter.ViewHolder ?: return
		val row = rowHolder.item as? ListRow ?: return
		val cards = (rowHolder.viewHolder as? ListRowPresenter.ViewHolder)?.gridView ?: return
		val cardHolder = cards.findContainingViewHolder(focused) as? ItemBridgeAdapter.ViewHolder ?: return
		val item = cardHolder.item as? BaseRowItem ?: return
		currentItem = item
		currentRow = row
		backgroundService.setBackground(item.baseItem)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		applyRowSpacing()
		fun View?.insideRows() = this != null && generateSequence(this) { it.parent as? View }.any { it === view }
		val listener = android.view.ViewTreeObserver.OnGlobalFocusChangeListener { old, focused ->
			if (focused.insideRows() && !old.insideRows()) restoreFocusedBackdrop()
		}
		view.viewTreeObserver.addOnGlobalFocusChangeListener(listener)
		viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
			override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
				if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnGlobalFocusChangeListener(listener)
			}
		})
	}

	override fun onResume() {
		super.onResume()
		view?.post { if (isResumed) restoreFocusedBackdrop() }

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
						// Refresh rows in place so the cursor stays where it was.
						// Playback and watch-status changes rebuild rows through playbackUpdates and the
						// websocket, which also brings back rows that were empty.
						refreshRows(delayed = false)
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
		homeLoadGeneration++
		homeLoadJob?.cancel()
		(adapter as? RecoveringHomeRowsAdapter)?.cancelRequests()
		homeErrorDialog?.dismiss()
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
			// Adapter rebuilding/restored focus can report a temporary null selection.
			// Keep the backdrop until an actual replacement item is selected.
			if (item == null) return
			if (item !is BaseRowItem) {
				currentItem = null
				focusedItem.value = null
				focusedItemIndex = (row as? ListRow)?.adapter?.positionOf(item) ?: -1
				//fill in default background
				backgroundService.clearBackgrounds()
			} else {
				currentItem = item
				focusedItem.value = item.baseItem
				currentRow = row as ListRow
				focusedItemIndex = row.adapter.positionOf(item)

				val itemRowAdapter = row.adapter as? ItemRowAdapter
				itemRowAdapter?.loadMoreItemsIfNeeded(itemRowAdapter.indexOf(item))

				backgroundService.setBackground(item.baseItem)
			}
		}
	}
}

private fun ObjectAdapter.positionOf(item: Any): Int = (0 until size()).firstOrNull { get(it) == item } ?: -1
