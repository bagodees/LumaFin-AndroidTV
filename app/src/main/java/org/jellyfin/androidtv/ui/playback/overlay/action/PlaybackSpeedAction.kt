package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoSpeedController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import java.util.Locale

class PlaybackSpeedAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val speedController = VideoSpeedController(playbackController)
	private val speeds = VideoSpeedController.SpeedSteps.entries.toTypedArray()
	private var popup: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null

	init {
		initializeWithIcon(R.drawable.ic_playback_speed)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		dismissPopup()
		val current = speeds.indexOf(speedController.currentSpeed)
		popup = playerMenu(playbackController, videoPlayerAdapter, context, context.getString(R.string.lbl_playback_speed), onDismiss = { popup = null }).apply {
			speeds.forEachIndexed { i, speed ->
				// Purely numeric, so a fixed locale keeps the format consistent
				row(String.format(Locale.US, "%.2fx", speed.speed), selected = i == current) {
					speedController.currentSpeed = speed
					dismiss()
				}
			}
			show(current.coerceAtLeast(0))
		}
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
