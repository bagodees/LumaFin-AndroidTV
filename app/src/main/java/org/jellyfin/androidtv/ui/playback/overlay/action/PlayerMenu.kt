package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

/**
 * The LumaFin menu panel opened from the player. The overlay stays visible while it is open and resumes
 * auto-hiding when it closes; the panel also closes with the player's view.
 */
internal fun playerMenu(
	controller: PlaybackController,
	adapter: VideoPlayerAdapter,
	context: Context,
	title: String,
	description: String? = null,
	onDismiss: () -> Unit = {},
): LumaFinMenuDialog {
	adapter.leanbackOverlayFragment.setFading(false)
	return LumaFinMenuDialog(context, controller.fragment.viewLifecycleOwner, title, description, fitContent = true).apply {
		onDismiss {
			adapter.leanbackOverlayFragment.setFading(true)
			onDismiss()
		}
	}
}
