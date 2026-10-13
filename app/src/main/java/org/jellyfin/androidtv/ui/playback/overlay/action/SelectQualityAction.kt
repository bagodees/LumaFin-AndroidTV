package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
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
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.ic_select_quality)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		val qualityProfiles = StreamingQuality.entries.filter { it != StreamingQuality.CUSTOM || userPreferences[UserPreferences.streamingQuality] == it }
		popup = PopupMenu(context, view, Gravity.END).apply {
			qualityProfiles.forEachIndexed { i, selected ->
				menu.add(0, i, i, context.getString(selected.nameRes))
			}

			menu.setGroupCheckable(0, true, true)
			val currentQualityIndex = qualityProfiles.indexOf(userPreferences[UserPreferences.streamingQuality])
			if (currentQualityIndex != -1) {
				menu.getItem(currentQualityIndex)?.let { item ->
					item.isChecked = true
				}
			}

			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { menuItem ->
				userPreferences[UserPreferences.streamingQuality] = qualityProfiles[menuItem.itemId]
				playbackController.refreshStream()
				dismiss()
				true
			}
		}
		popup?.show()
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
