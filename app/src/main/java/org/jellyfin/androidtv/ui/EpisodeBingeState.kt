package org.jellyfin.androidtv.ui

internal class EpisodeBingeState {
	private var completed = 0
	fun completeEpisode() { completed++ }
	fun reset() { completed = 0 }
	fun shouldAsk(limit: Int): Boolean = limit > 0 && completed >= limit
}
