package org.jellyfin.androidtv.util

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID
import android.app.AlertDialog
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

fun canRemoveFromContinueWatching(item: BaseItemDto): Boolean =
	item.type in setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO) &&
		(item.userData?.playbackPositionTicks ?: 0) > 0

internal suspend fun removeContinueWatching(item: BaseItemDto, mutations: ItemMutationRepository, refresh: DataRefreshService, messages: CustomMessageRepository) {
	mutations.setPlayed(item.id, false)
	refresh.playbackReportFinished(item)
	messages.pushMessage(CustomMessage.RefreshCurrentItem)
}

fun FragmentActivity.confirmRemoveFromContinueWatching(item: BaseItemDto) {
	if (!canRemoveFromContinueWatching(item)) return
	AlertDialog.Builder(this)
		.setTitle(R.string.remove_continue_watching)
		.setMessage(getString(R.string.remove_continue_watching_confirmation, item.name.orEmpty()))
		.setNegativeButton(R.string.lbl_cancel, null)
		.setPositiveButton(R.string.remove_continue_watching_confirm) { _, _ ->
			val mutations by inject<ItemMutationRepository>()
			val refresh by inject<DataRefreshService>()
			val messages by inject<CustomMessageRepository>()
			lifecycleScope.launch {
				try {
					removeContinueWatching(item, mutations, refresh, messages)
					Toast.makeText(this@confirmRemoveFromContinueWatching, R.string.remove_continue_watching_success, Toast.LENGTH_SHORT).show()
				} catch (error: CancellationException) {
					throw error
				} catch (error: Exception) {
					Timber.e(error, "Unable to clear Continue Watching progress")
					Toast.makeText(this@confirmRemoveFromContinueWatching, R.string.remove_continue_watching_failed, Toast.LENGTH_LONG).show()
				}
			}
		}
		.show()
}

fun FragmentActivity.playFirstUnwatchedItem(parentId: UUID) {
	val api by inject<ApiClient>()
	val playbackHelper by inject<PlaybackHelper>()

	lifecycleScope.launch(Dispatchers.IO) {
		runCatching {
			api.itemsApi.getItems(
				parentId = parentId,
				recursive = true,
				isMissing = false,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				sortOrder = setOf(SortOrder.ASCENDING),
				limit = 1,
				excludeItemTypes = setOf(),
				filters = setOf(ItemFilter.IS_UNPLAYED),
			)
		}.fold(
			onSuccess = { response ->
				val item = response.content.items?.firstOrNull()
				withContext(Dispatchers.Main) {
					if (item == null) {
						Toast.makeText(
							this@playFirstUnwatchedItem,
							R.string.msg_no_items,
							Toast.LENGTH_LONG
						).show()
					} else {
						playbackHelper.retrieveAndPlay(item.id, false, this@playFirstUnwatchedItem)
					}
				}
			},
			onFailure = { error ->
				Timber.e(error, "Error trying to play first unwatched")
				Toast.makeText(
					this@playFirstUnwatchedItem,
					R.string.msg_video_playback_error,
					Toast.LENGTH_LONG
				).show()
			},
		)
	}
}
