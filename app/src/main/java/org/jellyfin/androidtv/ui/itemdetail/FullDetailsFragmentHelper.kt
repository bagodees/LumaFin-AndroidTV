package org.jellyfin.androidtv.ui.itemdetail

import android.content.ActivityNotFoundException
import android.view.View
import android.widget.Toast
import org.jellyfin.androidtv.util.confirmRemoveFromContinueWatching
import org.jellyfin.androidtv.util.listenForFocusWhileAttached
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.util.sdk.TrailerUtils.getExternalTrailerIntent
import org.jellyfin.androidtv.util.sdk.compat.canResume
import org.jellyfin.androidtv.util.sdk.compat.copyWithUserData
import androidx.lifecycle.Lifecycle
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUID
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun FullDetailsFragment.showDeleteConfirmation(onConfirmed: Runnable) {
	val selectedId = mBaseItem.id
	val root = requireView()
	val anchor = root.findFocus()
	val menu = LumaFinMenuDialog(requireContext(), viewLifecycleOwner,
		getString(R.string.item_delete_confirm_title), getString(R.string.item_delete_confirm_message), fitContent = true)
	menu.button(getString(R.string.lbl_no)) { menu.dismiss() }
	menu.button(getString(R.string.lbl_delete), primary = true) {
		menu.dismiss()
		if (isAdded && view === root && mBaseItem.id == selectedId &&
			viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) onConfirmed.run()
	}
	menu.onDismiss { if (anchor?.isAttachedToWindow == true) anchor.requestFocus() }
	menu.show()
}

/** Selected-row callbacks also run while data binds; only actual focus should hide header chrome. */
fun FullDetailsFragment.installDetailsHeaderFocus(root: View) {
	val header = root.findViewById<View>(R.id.detailsHeader)
	header.visibility = View.VISIBLE
	val listener = android.view.ViewTreeObserver.OnGlobalFocusChangeListener { _, focused ->
		if (focused == null || !generateSequence(focused) { it.parent as? View }.any { it === root })
			return@OnGlobalFocusChangeListener
		val grid = root.findViewById<View>(R.id.rowsFragment)
		val rows = (childFragmentManager.findFragmentById(grid.id) as? androidx.leanback.app.RowsSupportFragment)?.verticalGridView
		val holder = rows?.findContainingViewHolder(focused) as? androidx.leanback.widget.ItemBridgeAdapter.ViewHolder
		header.visibility = if (holder?.item is androidx.leanback.widget.ListRow) View.INVISIBLE else View.VISIBLE
	}
	root.listenForFocusWhileAttached(listener)
}

fun FullDetailsFragment.deleteItem(
	api: ApiClient,
	item: BaseItemDto,
	dataRefreshService: DataRefreshService,
	navigationRepository: NavigationRepository,
) = lifecycleScope.launch {
	Timber.i("Deleting item ${item.name} (id=${item.id})")

	try {
		withContext(Dispatchers.IO) {
			api.libraryApi.deleteItem(item.id)
		}
	} catch (error: ApiClientException) {
		Timber.e(error, "Failed to delete item ${item.name} (id=${item.id})")
		Toast.makeText(
			context,
			getString(R.string.item_deletion_failed, item.name),
			Toast.LENGTH_LONG
		).show()
		return@launch
	}

	dataRefreshService.lastDeletedItemId = item.id

	if (navigationRepository.canGoBack) navigationRepository.goBack()
	else navigationRepository.navigate(Destinations.home)

	Toast.makeText(context, getString(R.string.item_deleted, item.name), Toast.LENGTH_LONG).show()
}

fun FullDetailsFragment.showDetailsMenu(
	view: View,
	baseItemDto: BaseItemDto,
) = DetailsMenu(view, getString(R.string.details_more), baseItemDto.name).apply {
	if (org.jellyfin.androidtv.ui.browsing.canSelectWatchTitles(view)) {
		item(getString(R.string.bulk_watch_title), navigates = true) {
			view.post { org.jellyfin.androidtv.ui.browsing.showBulkWatchSelection(view, null) }
		}
	}
	// Hidden actions remain available in More.
    if (mWatchedToggleButton?.isVisible == false) {
        item(getString(if (baseItemDto.userData?.played == true) R.string.details_mark_unwatched else R.string.details_mark_watched)) { togglePlayed() }
    }
	if (queueButton?.isVisible == false) {
		item(getString(R.string.lbl_add_to_queue)) { addItemToQueue() }
	}

	if (shuffleButton?.isVisible == false) {
		item(getString(R.string.lbl_shuffle_all)) { shufflePlay() }
	}

	if (hasWatchLaterAction()) {
        val watchLater = org.koin.core.context.GlobalContext.get().get<org.jellyfin.androidtv.preference.WatchLaterPreferences>()
        item(getString(if (watchLater.contains(baseItemDto.id)) R.string.watch_later_remove else R.string.watch_later_add)) {
            org.jellyfin.androidtv.util.toggleWatchLater(requireContext(), baseItemDto)
            watchLaterButton?.setLabel(getString(if (watchLater.contains(baseItemDto.id)) R.string.watch_later_remove else R.string.watch_later_add))
        }
    }

    if (extrasButton?.isVisible == false) {
		item(getString(R.string.extras_title), navigates = true) { showExtras(view) }
	}

	if (trailerButton?.isVisible == false) {
		item(getString(R.string.lbl_play_trailers)) { playTrailers() }
	}

	if (favButton?.isVisible == false) {
		val favoriteStringRes = when (baseItemDto.userData?.isFavorite) {
			true -> R.string.lbl_remove_favorite
			else -> R.string.lbl_add_favorite
		}

		item(getString(favoriteStringRes)) { toggleFavorite() }
	}

	// The same actions as a card's menu (#84), so nothing is long-press only
	if (org.jellyfin.androidtv.util.canPlayExternally(baseItemDto)) {
		item(getString(R.string.play_in_external_player)) {
			org.koin.core.context.GlobalContext.get().get<org.jellyfin.androidtv.util.PlaybackHelper>()
				.retrieveAndPlay(baseItemDto.id, false, requireContext(), external = true)
		}
	}
	if (org.jellyfin.androidtv.util.canRemoveFromContinueWatching(baseItemDto)) {
		item(getString(R.string.remove_continue_watching)) { requireActivity().confirmRemoveFromContinueWatching(baseItemDto) }
	}
	val shortcuts = org.koin.core.context.GlobalContext.get().get<org.jellyfin.androidtv.preference.HomeShortcutPreferences>()
	if (shortcuts.canPin(baseItemDto)) {
		val pinned = shortcuts.isPinned(baseItemDto.id)
		item(getString(if (pinned) R.string.home_shortcut_unpin else R.string.home_shortcut_pin)) {
			if (pinned) shortcuts.unpin(baseItemDto.id.toString()) else shortcuts.pin(baseItemDto)
			org.koin.core.context.GlobalContext.get().get<org.jellyfin.androidtv.data.repository.CustomMessageRepository>()
				.pushMessage(org.jellyfin.androidtv.constant.CustomMessage.HomeShortcutsChanged)
			Toast.makeText(requireContext(), if (pinned) R.string.home_shortcut_unpinned else R.string.home_shortcut_pinned, Toast.LENGTH_SHORT).show()
		}
	}

	if (goToSeriesButton?.isVisible == false) {
		item(getString(R.string.lbl_goto_series), navigates = true) { gotoSeries() }
	}
	if (hasDeleteAction()) {
		item(getString(R.string.lbl_delete)) { deleteButton?.performClick() }
	}
}.showIfNotEmpty().let { Unit }

fun FullDetailsFragment.createFakeSeriesTimerBaseItemDto(timer: SeriesTimerInfoDto) = BaseItemDto(
	id = requireNotNull(timer.id).toUUID(),
	type = BaseItemKind.FOLDER,
	mediaType = MediaType.UNKNOWN,
	seriesTimerId = timer.id,
	name = timer.name,
	overview = timer.getSeriesOverview(requireContext()),
)

fun FullDetailsFragment.toggleFavorite() {
	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setFavorite(
			item = mBaseItem.id,
			favorite = !(mBaseItem.userData?.isFavorite ?: false)
		)
		mBaseItem = mBaseItem.copyWithUserData(userData)
		favButton.isActivated = userData.isFavorite
		dataRefreshService.lastFavoriteUpdate = Instant.now()
	}
}

fun FullDetailsFragment.togglePlayed() {
	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setPlayed(
			item = mBaseItem.id,
			played = !(mBaseItem.userData?.played ?: false)
		)
		mBaseItem = mBaseItem.copyWithUserData(userData)
		mWatchedToggleButton.isActivated = userData.played
		updateSpoilerSummary()

		// Adjust resume
		mResumeButton?.apply {
			isVisible = mBaseItem.canResume
		}

		// Force lists to re-fetch
		dataRefreshService.lastPlayback = Instant.now()
		when (mBaseItem.type) {
			BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
			BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
			else -> Unit
		}

		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.playTrailers() {
	val localTrailerCount = mBaseItem.localTrailerCount ?: 0

	// External trailer
	if (localTrailerCount < 1) try {
		val intent = getExternalTrailerIntent(requireContext(), mBaseItem)
		if (intent != null) startActivity(intent)
	} catch (exception: ActivityNotFoundException) {
		Timber.w(exception, "Unable to open external trailer")
		Toast.makeText(
			requireContext(),
			getString(R.string.no_player_message),
			Toast.LENGTH_LONG
		).show()
	} else lifecycleScope.launch {
		val api by inject<ApiClient>()

		try {
			val trailers = withContext(Dispatchers.IO) {
				api.userLibraryApi.getLocalTrailers(mBaseItem.id).content
			}
			play(trailers, 0, false)
		} catch (exception: ApiClientException) {
			Timber.e(exception, "Error retrieving trailers for playback")
			Toast.makeText(
				requireContext(),
				getString(R.string.msg_video_playback_error),
				Toast.LENGTH_LONG
			).show()
		}
	}
}

fun FullDetailsFragment.getItem(id: UUID, callback: (item: BaseItemDto?) -> Unit) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val response = try {
			withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(id).content
			}
		} catch (err: ApiClientException) {
			Timber.w(err, "Failed to get item $id")
			null
		}

		callback(response)
	}
}

fun FullDetailsFragment.populatePreviousButton() {
	if (mBaseItem.type != BaseItemKind.EPISODE) return

	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val siblings = withContext(Dispatchers.IO) {
			api.tvShowsApi.getEpisodes(
				seriesId = requireNotNull(mBaseItem.seriesId),
				adjacentTo = mBaseItem.id,
			).content
		}

		// adjacentTo returns [previous, current, next] without missing neighbors, so on the
		// first episode it's [current, next]; only an item before the current one counts
		val currentIndex = siblings.items.indexOfFirst { it.id == mBaseItem.id }
		val previousItem = siblings.items.getOrNull(currentIndex - 1)?.id

		mPrevItemId = previousItem
		mPrevButton.isVisible = previousItem != null

		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.getNextUpEpisode(callback: (BaseItemDto?) -> Unit) {
	lifecycleScope.launch {
		val nextUpEpisode = getNextUpEpisode()
		callback(nextUpEpisode)
	}
}

suspend fun FullDetailsFragment.getNextUpEpisode(): BaseItemDto? {
	val api by inject<ApiClient>()

	try {
		val episodes = withContext(Dispatchers.IO) {
			api.tvShowsApi.getNextUp(
				seriesId = mBaseItem.seriesId ?: mBaseItem.id,
				fields = ItemRepository.itemFields,
				limit = 1,
			).content
		}
		return episodes.items.firstOrNull()
	} catch (err: ApiClientException) {
		Timber.w(err, "Failed to get next up items")
		return null
	}
}

fun FullDetailsFragment.resumePlayback(v: View) {
	if (mBaseItem.type != BaseItemKind.SERIES) {
		val pos = (mBaseItem.userData?.playbackPositionTicks?.ticks
			?: Duration.ZERO) - resumePreroll.milliseconds
		play(mBaseItem, pos.inWholeMilliseconds.toInt(), false)
		return
	}

	lifecycleScope.launch {
		val nextUpEpisode = getNextUpEpisode()
		if (nextUpEpisode == null) {
			Toast.makeText(
				requireContext(),
				getString(R.string.msg_video_playback_error),
				Toast.LENGTH_LONG
			).show()
		} else if (nextUpEpisode.userData?.playbackPositionTicks == 0L) {
			play(nextUpEpisode, 0, false)
		} else {
			showResumeMenu(v, nextUpEpisode)
		}
	}
}

fun FullDetailsFragment.showResumeMenu(
	view: View,
	nextUpEpisode: BaseItemDto
) = DetailsMenu(view, nextUpEpisode.name.orEmpty()).apply {
	val pos = (nextUpEpisode.userData?.playbackPositionTicks?.ticks
		?: Duration.ZERO) - resumePreroll.milliseconds
	item(
		getString(
			R.string.lbl_resume_from,
			TimeUtils.formatMillis(pos.inWholeMilliseconds)
		)
	) {
		play(nextUpEpisode, pos.inWholeMilliseconds.toInt(), false)
	}
	item(getString(R.string.lbl_from_beginning)) {
		play(nextUpEpisode, 0, false)
	}
}.showIfNotEmpty().let { Unit }

fun FullDetailsFragment.getLiveTvSeriesTimer(
	id: String,
	callback: (timer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getSeriesTimer(id).content
			}
		}.onSuccess { timer ->
			callback(timer)
		}
	}
}

fun FullDetailsFragment.getLiveTvProgram(
	id: UUID,
	callback: (program: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getProgram(id.toString()).content
			}
		}.onSuccess { program ->
			callback(program)
		}
	}
}

fun FullDetailsFragment.createLiveTvSeriesTimer(
	seriesTimer: SeriesTimerInfoDto,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.createSeriesTimer(seriesTimer)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvDefaultTimer(
	id: UUID,
	callback: (seriesTimer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getDefaultTimer(id.toString()).content
			}
		}.onSuccess { seriesTimer ->
			callback(seriesTimer)
		}
	}
}

fun FullDetailsFragment.cancelLiveTvSeriesTimer(
	timerId: String,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.cancelTimer(timerId)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}

fun FullDetailsFragment.showExtras(anchor: View) {
    val selectedItem = mBaseItem
    val owner = viewLifecycleOwner
    fun current() = isAdded && mBaseItem.id == selectedItem.id && anchor.isAttachedToWindow &&
        owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    var loading = true
    val dialog = LumaFinMenuDialog(requireContext(), owner, getString(R.string.extras_title), getString(R.string.loading), fitContent = true)
    dialog.button(getString(android.R.string.cancel)) { dialog.dismiss() }
    val api by inject<ApiClient>()
    val job = owner.lifecycleScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
        try {
            val entries = withContext(Dispatchers.IO) {
                val features = if ((selectedItem.specialFeatureCount ?: 0) > 0)
                    api.userLibraryApi.getSpecialFeatures(selectedItem.id).content else emptyList()
                val trailers = if ((selectedItem.localTrailerCount ?: 0) > 0)
                    api.userLibraryApi.getLocalTrailers(selectedItem.id).content else emptyList()
                org.jellyfin.androidtv.util.combineExtras(features, trailers)
            }
            if (!dialog.isShowing || !current()) return@launch
            loading = false
            dialog.dismiss()
            if (entries.isEmpty()) {
                val empty = LumaFinMenuDialog(requireContext(), owner, getString(R.string.extras_title), selectedItem.name, fitContent = true)
                empty.emptyState(getString(R.string.extras_empty))
                empty.button(getString(android.R.string.ok), primary = true) { empty.dismiss() }
                empty.onDismiss { if (anchor.isAttachedToWindow) anchor.requestFocus() }
                empty.show()
                return@launch
            }
            val menu = LumaFinMenuDialog(requireContext(), owner, getString(R.string.extras_title), selectedItem.name, fitContent = true)
            entries.forEach { extra ->
                val type = if (extra.type == BaseItemKind.TRAILER) R.string.extra_trailer
                    else org.jellyfin.androidtv.util.extraTypeLabel(extra.extraType)
                val runtime = extra.runTimeTicks?.takeIf { it >= 10_000_000L }
                    ?.let { TimeUtils.formatMillis(it / 10_000L) }
                val details = listOfNotNull(getString(type), runtime).joinToString(" · ")
                menu.row(extra.name?.takeIf { it.isNotBlank() } ?: getString(type), details, navigates = false) {
                    menu.dismiss()
                    if (current()) {
                        returnToExtras = true
                        play(listOf(extra), 0, false)
                    }
                }
            }
            menu.button(getString(android.R.string.cancel)) { menu.dismiss() }
            menu.onDismiss { if (anchor.isAttachedToWindow) anchor.requestFocus() }
            menu.show()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: ApiClientException) {
            Timber.w(error, "Unable to load extras")
            if (dialog.isShowing && current()) dialog.description(getString(R.string.extras_error))
        }
    }
    dialog.onDismiss {
        if (loading && job.isActive) job.cancel()
        if (anchor.isAttachedToWindow) anchor.requestFocus()
    }
    dialog.show()
    job.start()
}
