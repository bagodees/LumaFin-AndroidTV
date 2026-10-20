package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackSubtitleCompatibilityScreen() {
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings_subtitle_compatibility)) },
				captionContent = { Text(stringResource(R.string.settings_subtitle_compatibility_description)) },
			)
		}

		item {
			var pgsDirectPlay by rememberPreference(userPreferences, UserPreferences.pgsDirectPlay)

			ListButton(

				toggleState = pgsDirectPlay,
				headingContent = { Text(stringResource(R.string.preference_enable_pgs)) },
				captionContent = { Text(stringResource(R.string.preference_enable_pgs_description)) },
				trailingContent = { SettingsSwitch(checked = pgsDirectPlay) },
				onClick = { pgsDirectPlay = !pgsDirectPlay },
				modifier = Modifier.focusKey("pgs_direct_play")
			)
		}

		item {
			var assDirectPlay by rememberPreference(userPreferences, UserPreferences.assDirectPlay)

			ListButton(

				toggleState = assDirectPlay,
				headingContent = { Text(stringResource(R.string.preference_enable_ass)) },
				captionContent = { Text(stringResource(R.string.preference_enable_ass_description)) },
				trailingContent = { SettingsSwitch(checked = assDirectPlay) },
				onClick = { assDirectPlay = !assDirectPlay },
				modifier = Modifier.focusKey("ass_direct_play")
			)
		}

		item {
			var subtitlesBurnDuringTranscode by rememberPreference(userPreferences, UserPreferences.subtitlesBurnDuringTranscode)

			ListButton(

				toggleState = subtitlesBurnDuringTranscode,
				headingContent = { Text(stringResource(R.string.pref_burn_subtitles_when_transcoding)) },
				captionContent = { Text(stringResource(R.string.pref_burn_subtitles_when_transcoding_description)) },
				trailingContent = { SettingsSwitch(checked = subtitlesBurnDuringTranscode) },
				onClick = { subtitlesBurnDuringTranscode = !subtitlesBurnDuringTranscode },
				modifier = Modifier.focusKey("subtitles_burn_during_transcode")
			)
		}

	}
}
