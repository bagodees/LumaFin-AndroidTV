package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class ZoomAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null

	init {
		initializeWithIcon(R.drawable.ic_aspect_ratio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		dismissPopup()
		val modes = listOf(ZoomMode.FIT to R.string.lbl_fit, ZoomMode.AUTO_CROP to R.string.lbl_auto_crop, ZoomMode.STRETCH to R.string.lbl_stretch)
		popup = playerMenu(playbackController, videoPlayerAdapter, context, context.getString(R.string.lbl_zoom), onDismiss = { popup = null }).apply {
			modes.forEach { (mode, label) ->
				row(context.getString(label), selected = playbackController.zoomMode == mode) { playbackController.setZoom(mode); dismiss() }
			}
			show(modes.indexOfFirst { it.first == playbackController.zoomMode }.coerceAtLeast(0))
		}
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
