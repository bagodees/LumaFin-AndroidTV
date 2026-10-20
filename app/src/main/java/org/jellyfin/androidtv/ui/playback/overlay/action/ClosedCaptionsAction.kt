package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
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
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber
import org.jellyfin.androidtv.ui.playback.SubtitleLookupDialog
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceRecorder
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceScopeDialog
import org.jellyfin.androidtv.ui.playback.canGetMoreSubtitles

class ClosedCaptionsAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null
	private var timingDialog: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null
	private var lookupDialog: SubtitleLookupDialog? = null
	private var choicesDialog: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null

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

		val streams = playbackController.currentMediaSource.mediaStreams.orEmpty().filter { it.type == MediaStreamType.SUBTITLE }
		val labels = streams.trackLabels(context)
		val current = playbackController.subtitleStreamIndex
		removePopup()
		fun select(index: Int) {
			PlaybackChoiceRecorder.subtitle(playbackController.currentlyPlayingItem, playbackController.currentMediaSource?.mediaStreams, index)
			playbackController.setSubtitleIndex(index)
		}
		popup = playerMenu(playbackController, videoPlayerAdapter, context, context.getString(R.string.pref_subtitles), onDismiss = { popup = null }).apply {
			row(context.getString(R.string.lbl_track_off), selected = current == -1) { dismiss(); select(-1) }
			for (sub in streams) row(labels.getValue(sub.index), selected = sub.index == current) { dismiss(); select(sub.index) }
			if (playbackController.subtitleTimingAvailable()) row(context.getString(R.string.subtitle_timing)) {
				dismiss(); view.post { showTiming(playbackController, videoPlayerAdapter, context) }
			}
			if (canGetMoreSubtitles(playbackController.currentlyPlayingItem)) row(context.getString(R.string.subtitle_lookup_more)) {
				dismiss()
				view.post {
					val item = playbackController.currentlyPlayingItem ?: return@post
					val source = playbackController.currentMediaSource?.id ?: return@post
					val queueIndex = playbackController.currentQueueIndex
					videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
					lookupDialog = SubtitleLookupDialog(context, playbackController.fragment.viewLifecycleOwner, item, source,
						stillCurrent = { playbackController.currentlyPlayingItem?.id == item.id && playbackController.currentQueueIndex == queueIndex && playbackController.currentMediaSource?.id == source },
						playback = true, onRefreshed = { playbackController.refreshStream() },
						onDismiss = { videoPlayerAdapter.leanbackOverlayFragment.setFading(true); lookupDialog = null }).also { it.show() }
				}
			}
			playbackController.currentlyPlayingItem?.takeIf(PlaybackChoiceScopeDialog::available)?.let { item ->
				row(PlaybackChoiceScopeDialog.menuLabel(context, item)) { dismiss(); view.post { showChoices(playbackController, videoPlayerAdapter, context) } }
			}
			show((listOf(-1) + streams.map { it.index }).indexOf(current).coerceAtLeast(0))
		}
	}

	fun removePopup() {
		popup?.dismiss()
		timingDialog?.dismiss()
		lookupDialog?.dismiss()
		choicesDialog?.dismiss()
	}

	private fun showChoices(controller: PlaybackController, adapter: VideoPlayerAdapter, context: Context) {
		val item = controller.currentlyPlayingItem ?: return
		adapter.leanbackOverlayFragment.setFading(false)
		choicesDialog = PlaybackChoiceScopeDialog.showModern(context, controller.fragment.viewLifecycleOwner, item, onDismiss = {
			adapter.leanbackOverlayFragment.setFading(true)
			choicesDialog = null
		})
	}

	private fun showTiming(controller: PlaybackController, adapter: VideoPlayerAdapter, context: Context) {
		timingDialog?.dismiss()
		timingDialog = timingMenu(controller, adapter, context,
			heading = { context.getString(R.string.subtitle_timing_value, it) },
			description = context.getString(R.string.settings_subtitle_timing_dialog),
			earlier = context.getString(R.string.subtitle_earlier), later = context.getString(R.string.subtitle_later),
			currentMs = { controller.subtitleDelayMs() },
			adjust = { ms, reset -> controller.adjustSubtitleTiming(ms.toLong(), reset = reset) },
			onDismiss = { timingDialog = null })
	}
}
