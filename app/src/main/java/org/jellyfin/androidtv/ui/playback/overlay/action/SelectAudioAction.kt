package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.AlertDialog
import java.util.Locale
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.util.trackLabels
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.audioTimingAvailable
import org.jellyfin.androidtv.ui.playback.audioDelayMs
import org.jellyfin.androidtv.ui.playback.adjustAudioTiming
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class SelectAudioAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PopupMenu? = null
	private var timingDialog: AlertDialog? = null

	init {
		initializeWithIcon(R.drawable.ic_select_audio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val audioTracks = playbackController.currentStreamInfo?.selectableAudioStreams ?: return
		val labels = audioTracks.trackLabels(context)
		val currentAudioIndex = playbackController.audioStreamIndex

		dismissPopup()
		popup = PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
				for (track in audioTracks) {
					add(0, track.index, track.index, labels.getValue(track.index)).apply {
						isChecked = currentAudioIndex == track.index
					}
				}
				setGroupCheckable(0, true, false)
				add(1, -2, audioTracks.size, context.getString(R.string.audio_timing)).isEnabled = playbackController.audioTimingAvailable()
			}

			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { item ->
				if (item.itemId == -2) view.post { showTiming(playbackController, videoPlayerAdapter, context) }
				else playbackController.switchAudioStream(item.itemId)
				true
			}
		}
		popup?.show()
	}

	fun dismissPopup() {
		popup?.dismiss()
		timingDialog?.dismiss()
	}
    private fun showTiming(controller: PlaybackController, adapter: VideoPlayerAdapter, context: Context) {
        adapter.leanbackOverlayFragment.setFading(false)
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.audio_timing)
            .setMessage(R.string.audio_timing_description)
            .setNegativeButton(R.string.audio_earlier, null)
            .setPositiveButton(R.string.audio_later, null)
            .setNeutralButton(R.string.subtitle_timing_reset, null)
            .create()
        fun refresh() {
            dialog.setTitle(context.getString(R.string.audio_timing_value,
                String.format(Locale.getDefault(), "%+.1f", controller.audioDelayMs() / 1000.0)))
        }
        dialog.setOnDismissListener { adapter.leanbackOverlayFragment.setFading(true); timingDialog = null }
        dialog.setOnShowListener {
            refresh()
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                controller.adjustAudioTiming(-100); refresh()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                controller.adjustAudioTiming(100); refresh()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                controller.adjustAudioTiming(0, reset = true); refresh()
            }
        }
        timingDialog = dialog
        dialog.show()
    }

}
