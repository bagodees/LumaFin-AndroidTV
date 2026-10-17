package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.playbackInformation
import org.jellyfin.androidtv.ui.playback.diagnostics
import org.jellyfin.androidtv.ui.playback.description
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class PlaybackInformationAction(context: Context, glue: CustomPlaybackTransportControlGlue) : CustomAction(context, glue) {
	private var dialog: AlertDialog? = null
	private val handler = Handler(Looper.getMainLooper())
	private var refresh: Runnable? = null
	init { initializeWithIcon(R.drawable.ic_info) }
	override fun handleClickAction(playbackController: PlaybackController, videoPlayerAdapter: VideoPlayerAdapter, context: Context, view: View) {
		dismissDialog()
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val padding = (24 * context.resources.displayMetrics.density).toInt()
		val text = TextView(context).apply {
			text = playbackController.playbackInformation(context) + "\n\n" + playbackController.diagnostics().description(context)
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
					refresh?.let(handler::removeCallbacks)
					refresh = null
					videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
					dialog = null
				}
				show()
			}
		refresh = object : Runnable {
			override fun run() {
				if (dialog?.isShowing != true) return
				val updated = playbackController.playbackInformation(context) + "\n\n" + playbackController.diagnostics().description(context)
				if (text.text.toString() != updated) text.text = updated
				handler.postDelayed(this, 1000)
			}
		}.also { handler.postDelayed(it, 1000) }
	}
	fun dismissDialog() {
		refresh?.let(handler::removeCallbacks)
		refresh = null
		dialog?.dismiss()
		dialog = null
	}
}
