package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoQueueDialog
import org.jellyfin.androidtv.ui.playback.legacyVideoQueueEditor
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class VideoQueueAction(context: Context, private val glue: CustomPlaybackTransportControlGlue) : CustomAction(context, glue) {
	private var dialog: VideoQueueDialog? = null
	init { initializeWithIcon(R.drawable.ic_next_up) }
	override fun handleClickAction(playbackController: PlaybackController, videoPlayerAdapter: VideoPlayerAdapter, context: Context, view: View) {
		dismissDialog()
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dialog = VideoQueueDialog(context, legacyVideoQueueEditor(playbackController)) {
			videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
			glue.refreshQueueActions()
			dialog = null
		}.also { it.show() }
	}
	fun dismissDialog() { dialog?.dismiss(); dialog = null }
}
