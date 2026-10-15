package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import org.jellyfin.androidtv.util.trackLabels
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.ui.playback.setSubtitleIndex
import org.jellyfin.androidtv.ui.playback.subtitleTimingAvailable
import org.jellyfin.androidtv.ui.playback.subtitleDelayMs
import org.jellyfin.androidtv.ui.playback.adjustSubtitleTiming
import java.util.Locale
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

class ClosedCaptionsAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PopupMenu? = null
	private var timingDialog: AlertDialog? = null

	init {
		initializeWithIcon(R.drawable.ic_select_subtitle)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		if (playbackController.currentStreamInfo == null) {
			Timber.w("StreamInfo null trying to obtain subtitles")
			Toast.makeText(context, "Unable to obtain subtitle info", Toast.LENGTH_LONG).show()
			return
		}

		val labels = playbackController.currentMediaSource.mediaStreams.orEmpty().filter { it.type == MediaStreamType.SUBTITLE }.trackLabels(context)
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		removePopup()
		popup = PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
				var order = 0
				add(0, -1, order++, context.getString(R.string.lbl_track_off)).apply {
					isChecked = playbackController.subtitleStreamIndex == -1
				}

				for (sub in playbackController.currentMediaSource.mediaStreams.orEmpty()) {
					if (sub.type != MediaStreamType.SUBTITLE) continue

					add(0, sub.index, order++, labels.getValue(sub.index)).apply {
						isChecked = sub.index == playbackController.subtitleStreamIndex
					}
				}

				setGroupCheckable(0, true, false)
				add(1, -2, order, context.getString(R.string.subtitle_timing)).isEnabled = playbackController.subtitleTimingAvailable()
			}
			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { item ->
				if (item.itemId == -2) {
					view.post { showTiming(playbackController, videoPlayerAdapter, context) }
				} else playbackController.setSubtitleIndex(item.itemId)
				true
			}
		}
		popup?.show()
	}

	fun removePopup() {
		popup?.dismiss()
		timingDialog?.dismiss()
	}

	private fun showTiming(controller: PlaybackController, adapter: VideoPlayerAdapter, context: Context) {
		adapter.leanbackOverlayFragment.setFading(false)
		val dialog = AlertDialog.Builder(context)
			.setTitle(R.string.subtitle_timing)
			.setMessage(R.string.settings_subtitle_timing_dialog)
			.setNegativeButton(R.string.subtitle_earlier, null)
			.setPositiveButton(R.string.subtitle_later, null)
			.setNeutralButton(R.string.subtitle_timing_reset, null)
			.create()
		fun updateTitle() {
			val seconds = String.format(Locale.getDefault(), "%+.1f", controller.subtitleDelayMs() / 1000.0)
			dialog.setTitle(context.getString(R.string.subtitle_timing_value, seconds))
		}
		dialog.setOnDismissListener {
			adapter.leanbackOverlayFragment.setFading(true)
			timingDialog = null
		}
		dialog.setOnShowListener {
			updateTitle()
			dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { controller.adjustSubtitleTiming(-100); updateTitle() }
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { controller.adjustSubtitleTiming(100); updateTitle() }
			dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { controller.adjustSubtitleTiming(0, reset = true); updateTitle() }
		}
		timingDialog = dialog
		dialog.show()
	}
}
