package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackMediaSegmentScreen(
	segmentType: MediaSegmentType
) {
	val router = LocalRouter.current
	val mediaSegmentRepository = koinInject<MediaSegmentRepository>()
	val action = mediaSegmentRepository.getDefaultSegmentTypeAction(segmentType)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings_intro_recap_credits).uppercase()) },
				headingContent = { Text(stringResource(if (segmentType == MediaSegmentType.INTRO) R.string.intro_skipping else segmentType.nameRes)) },
				captionContent = { Text(stringResource(R.string.settings_segment_requirement)) },
			)
		}

		items(MediaSegmentAction.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(if (segmentType == MediaSegmentType.INTRO) entry.introNameRes else entry.nameRes)) },
				captionContent = { Text(stringResource(if (segmentType == MediaSegmentType.INTRO) entry.introDescriptionRes else when (entry) {
					MediaSegmentAction.NOTHING -> R.string.settings_segment_nothing
					MediaSegmentAction.SKIP -> R.string.settings_segment_skip
					MediaSegmentAction.ASK_TO_SKIP -> R.string.settings_segment_ask
				})) },
				trailingContent = { RadioButton(checked = action == entry) },
				onClick = {
					mediaSegmentRepository.setDefaultSegmentTypeAction(segmentType, entry)
					router.back()
				},
				modifier = Modifier
					.focusKey("media_segment_action_${entry.name}", initialFocus = action == entry)
			)
		}
	}
}
