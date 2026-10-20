package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.java.KoinJavaComponent

class SleepTimerAction(context: Context, glue: CustomPlaybackTransportControlGlue) : CustomAction(context, glue) {
	private var popup: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null
	init { initializeWithIcon(R.drawable.ic_sleep_timer) }
	override fun handleClickAction(playbackController: PlaybackController, videoPlayerAdapter: VideoPlayerAdapter, context: Context, view: View) {
		dismissPopup()
		val timers = KoinJavaComponent.get<PlaybackControllerContainer>(PlaybackControllerContainer::class.java)
		val options = buildList {
			add(0 to context.getString(R.string.sleep_timer_off))
			for (minutes in listOf(15, 30, 60)) add(minutes to if (timers.sleepTimerMinutes == minutes)
				context.getString(R.string.sleep_timer_remaining, timers.remainingSleepMinutes())
			else context.getString(R.string.sleep_timer_minutes, minutes))
			if (playbackController.currentlyPlayingItem?.type == BaseItemKind.EPISODE) add(-1 to context.getString(R.string.sleep_timer_episode_end))
		}
		val current = options.indexOfFirst { it.first == timers.sleepTimerMinutes }
		popup = playerMenu(playbackController, videoPlayerAdapter, context, context.getString(R.string.sleep_timer), onDismiss = { popup = null }).apply {
			options.forEach { (minutes, label) ->
				row(label, selected = minutes == timers.sleepTimerMinutes) { timers.setSleepTimer(minutes); dismiss() }
			}
			show(current.coerceAtLeast(0))
		}
	}
	fun dismissPopup() { popup?.dismiss(); popup = null }
}
