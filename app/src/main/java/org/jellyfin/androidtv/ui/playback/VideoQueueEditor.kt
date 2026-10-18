package org.jellyfin.androidtv.ui.playback

import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.queue.metadata
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.BaseItemDto

data class UpcomingVideo(val index: Int, val title: String)
data class VideoQueueView(
	val currentTitle: String?,
	val upcoming: List<UpcomingVideo>,
	val hasMore: Boolean,
	val editable: Boolean = true,
	val edit: (Int, Int?) -> Boolean,
)

fun interface VideoQueueEditor {
	suspend fun snapshot(limit: Int): VideoQueueView
}

private fun BaseItemDto.queueTitle(): String = buildString {
	seriesName?.let { append(it).append(" · ") }
	if (parentIndexNumber != null && indexNumber != null) append("S$parentIndexNumber E$indexNumber · ")
	append(name.orEmpty())
}

fun legacyVideoQueueEditor(controller: PlaybackController) = VideoQueueEditor { limit ->
	val items = controller.videoQueueSnapshot
	val current = controller.currentQueueIndex
	VideoQueueView(items.getOrNull(current)?.queueTitle(),
		items.indices.drop(current + 1).take(limit).map { UpcomingVideo(it, items[it].queueTitle()) },
		items.size - current - 1 > limit,
		edit = { from, destination -> controller.editUpcomingQueue(items, current, from, destination) })
}

fun playerVideoQueueEditor(manager: PlaybackManager) = VideoQueueEditor { limit ->
	val queue = manager.queue
	queue.peekNext(limit, usePlaybackOrder = false, useRepeatMode = false)
	val items = queue.entries.value
	val currentIndex = queue.entryIndex.value
	val current = queue.entry.value
	fun title(index: Int): String = items[index].baseItem?.queueTitle() ?: items[index].metadata.title.orEmpty()
	VideoQueueView(items.getOrNull(currentIndex)?.let { title(currentIndex) },
		items.indices.drop(currentIndex + 1).take(limit).map { UpcomingVideo(it, title(it)) },
		queue.estimatedSize - currentIndex - 1 > limit,
		editable = manager.state.playbackOrder.value == PlaybackOrder.DEFAULT,
		edit = { from, destination -> current != null && queue.editUpcoming(items, current, from, destination) })
}
