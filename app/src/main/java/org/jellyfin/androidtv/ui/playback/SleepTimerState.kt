package org.jellyfin.androidtv.ui.playback

import java.util.UUID

internal class SleepTimerState {
	var minutes: Int = 0
		private set
	var deadline: Long? = null
		private set
	private var episodeId: UUID? = null

	fun set(minutes: Int, now: Long, episodeId: UUID?) {
		this.minutes = minutes
		deadline = if (minutes > 0) now + minutes * 60_000L else null
		this.episodeId = if (minutes == -1) episodeId else null
	}
	fun expired(now: Long) = deadline?.let { now >= it } ?: false
	fun endsWithEpisode(id: UUID) = minutes == -1 && episodeId == id
	fun cancel() = set(0, 0, null)
}
