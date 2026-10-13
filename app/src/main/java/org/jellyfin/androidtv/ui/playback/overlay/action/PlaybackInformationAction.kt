package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.playbackInformation
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class PlaybackInformationAction(context: Context, glue: CustomPlaybackTransportControlGlue) : CustomAction(context, glue) {
	private var dialog: AlertDialog? = null
	init { initializeWithIcon(R.drawable.ic_info) }
	override fun handleClickAction(playbackController: PlaybackController, videoPlayerAdapter: VideoPlayerAdapter, context: Context, view: View) {
		dismissDialog()
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val padding = (24 * context.resources.displayMetrics.density).toInt()
		val text = TextView(context).apply {
			text = playbackController.playbackInformation(context)
			textSize = 18f
			setPadding(padding, padding / 2, padding, padding / 2)
		}
		val scroll = ScrollView(context).apply { addView(text) }
		dialog = AlertDialog.Builder(context)
			.setTitle(R.string.playback_information)
			.setView(scroll)
			.setPositiveButton(android.R.string.ok, null)
			.create().apply {
				setOnDismissListener {
					videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
					dialog = null
				}
				show()
			}
	}
	fun dismissDialog() { dialog?.dismiss(); dialog = null }
}
