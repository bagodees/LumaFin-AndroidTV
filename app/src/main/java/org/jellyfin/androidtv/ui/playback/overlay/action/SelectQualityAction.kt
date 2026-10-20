package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.constant.StreamingQuality
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class SelectQualityAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	private val userPreferences: UserPreferences
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog? = null

	init {
		initializeWithIcon(R.drawable.ic_select_quality)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		dismissPopup()
		val qualityProfiles = StreamingQuality.entries.filter { it != StreamingQuality.CUSTOM || userPreferences[UserPreferences.streamingQuality] == it }
		val current = qualityProfiles.indexOf(userPreferences[UserPreferences.streamingQuality])
		popup = playerMenu(playbackController, videoPlayerAdapter, context, context.getString(R.string.pref_streaming_quality), onDismiss = { popup = null }).apply {
			qualityProfiles.forEachIndexed { i, profile ->
				row(context.getString(profile.nameRes), selected = i == current) {
					dismiss()
					if (i != current) {
						userPreferences[UserPreferences.streamingQuality] = profile
						playbackController.refreshStream()
					}
				}
			}
			show(current.coerceAtLeast(0))
		}
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
