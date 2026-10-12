package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID

class DataRefreshService {
	private val _playbackUpdates = MutableStateFlow(0L)
	val playbackUpdates = _playbackUpdates.asStateFlow()

	/** Retain completion for Home even when it is behind the player. */
	fun playbackReportFinished(item: BaseItemDto, now: Instant = Instant.now()) {
		lastPlayback = now
		when (item.type) {
			BaseItemKind.MOVIE -> lastMoviePlayback = now
			BaseItemKind.EPISODE -> lastTvPlayback = now
			else -> Unit
		}
		_playbackUpdates.value += 1
	}

	var lastDeletedItemId: UUID? = null
	var lastPlayback: Instant? = null
	var lastMoviePlayback: Instant? = null
	var lastTvPlayback: Instant? = null
	var lastLibraryChange: Instant? = null
	var lastFavoriteUpdate: Instant? = null
	var lastPlayedItem: BaseItemDto? = null
}
