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

/**
 * Subtitle or audio timing in the LumaFin panel. Earlier/Later stay open and update the value in the title,
 * so the remote can step repeatedly; Reset returns to zero.
 */
internal fun timingMenu(
	controller: PlaybackController,
	adapter: VideoPlayerAdapter,
	context: Context,
	heading: (String) -> String,
	description: String,
	earlier: String,
	later: String,
	currentMs: () -> Long,
	adjust: (Int, Boolean) -> Unit,
	onDismiss: () -> Unit,
): LumaFinMenuDialog {
	fun value() = String.format(java.util.Locale.getDefault(), "%+.1f", currentMs() / 1000.0)
	return playerMenu(controller, adapter, context, heading(value()), description, onDismiss).apply {
		fun step(ms: Int, reset: Boolean = false) { adjust(ms, reset); title(heading(value())) }
		row(earlier, navigates = false) { step(-100) }
		row(later, navigates = false) { step(100) }
		button(context.getString(org.jellyfin.androidtv.R.string.subtitle_timing_reset)) { step(0, reset = true) }
		button(context.getString(org.jellyfin.androidtv.R.string.player_menu_done), primary = true) { dismiss() }
		show(1)
	}
}
