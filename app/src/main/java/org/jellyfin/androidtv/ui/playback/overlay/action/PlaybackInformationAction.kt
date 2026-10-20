package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.playbackInformation
import org.jellyfin.androidtv.ui.playback.diagnostics
import org.jellyfin.androidtv.ui.playback.description
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class PlaybackInformationAction(context: Context, glue: CustomPlaybackTransportControlGlue) : CustomAction(context, glue) {
	private var dialog: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null
	private val handler = Handler(Looper.getMainLooper())
	private var refresh: Runnable? = null
	init { initializeWithIcon(R.drawable.ic_info) }
	override fun handleClickAction(playbackController: PlaybackController, videoPlayerAdapter: VideoPlayerAdapter, context: Context, view: View) {
		dismissDialog()
		fun content() = playbackController.playbackInformation(context) + "\n\n" + playbackController.diagnostics().description(context)
		lateinit var text: android.widget.TextView
		dialog = playerMenu(playbackController, videoPlayerAdapter, context, context.getString(R.string.playback_information), onDismiss = {
			refresh?.let(handler::removeCallbacks)
			refresh = null
			dialog = null
		}).apply {
			text = body(content())
			button(context.getString(R.string.player_menu_done), primary = true) { dismiss() }
			show()
		}
		refresh = object : Runnable {
			override fun run() {
				if (dialog?.isShowing != true) return
				val updated = content()
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
