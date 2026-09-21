package org.jellyfin.androidtv.ui.playback

import java.util.UUID

/**
 * Audio/subtitle tracks picked on the item details screen, applied when that item starts playing.
 * A subtitle index of [SUBTITLES_OFF] means subtitles were explicitly turned off.
 */
object PlaybackTrackSelection {
	const val SUBTITLES_OFF = -1

	data class Selection(
		val audioIndex: Int? = null,
		val subtitleIndex: Int? = null,
	)

	private val selections = mutableMapOf<UUID, Selection>()

	fun get(itemId: UUID?): Selection? = itemId?.let { selections[it] }

	fun setAudio(itemId: UUID, index: Int) {
		selections[itemId] = (selections[itemId] ?: Selection()).copy(audioIndex = index)
	}

	fun setSubtitle(itemId: UUID, index: Int) {
		selections[itemId] = (selections[itemId] ?: Selection()).copy(subtitleIndex = index)
	}

	fun consume(itemId: UUID?): Selection? = itemId?.let { selections.remove(it) }
}
