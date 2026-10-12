package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.java.KoinJavaComponent

class SleepTimerAction(context: Context, glue: CustomPlaybackTransportControlGlue) : CustomAction(context, glue) {
	private var popup: PopupMenu? = null
	init { initializeWithIcon(R.drawable.ic_sleep_timer) }
	override fun handleClickAction(playbackController: PlaybackController, videoPlayerAdapter: VideoPlayerAdapter, context: Context, view: View) {
		dismissPopup()
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val timers = KoinJavaComponent.get<PlaybackControllerContainer>(PlaybackControllerContainer::class.java)
		popup = PopupMenu(context, view, Gravity.END).apply {
			menu.add(0, 0, 0, context.getString(R.string.sleep_timer_off))
			for (minutes in listOf(15, 30, 60)) {
				val label = if (timers.sleepTimerMinutes == minutes)
					context.getString(R.string.sleep_timer_remaining, timers.remainingSleepMinutes())
				else context.getString(R.string.sleep_timer_minutes, minutes)
				menu.add(0, minutes, minutes, label)
			}
			if (playbackController.currentlyPlayingItem?.type == BaseItemKind.EPISODE)
				menu.add(0, -1, 100, context.getString(R.string.sleep_timer_episode_end))
			menu.setGroupCheckable(0, true, true)
			menu.findItem(timers.sleepTimerMinutes)?.isChecked = true
			setOnMenuItemClickListener { timers.setSleepTimer(it.itemId); true }
			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			show()
		}
	}
	fun dismissPopup() { popup?.dismiss(); popup = null }
}
