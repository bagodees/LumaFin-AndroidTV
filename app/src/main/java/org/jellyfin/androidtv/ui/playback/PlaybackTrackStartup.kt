package org.jellyfin.androidtv.ui.playback

import java.util.UUID

/** Keeps a details-screen choice until playback is prepared, including startup retries. */
class PlaybackTrackStartup {
	private var itemId: UUID? = null
	private var selection: PlaybackTrackSelection.Selection? = null

	fun get(id: UUID): PlaybackTrackSelection.Selection? {
		if (itemId != id) {
			itemId = id
			selection = null
		}
		PlaybackTrackSelection.consume(id)?.let { selection = it }
		return selection
	}

	fun prepared() {
		selection = null
	}
}
