package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackAudioScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_audio)) },
				captionContent = { Text(stringResource(R.string.settings_audio_description)) },
			)
		}

		item {
			var audioBehaviour by rememberPreference(userPreferences, UserPreferences.audioBehaviour)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_audio_output)) },
				captionContent = { Text(stringResource(audioBehaviour.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_AUDIO_BEHAVIOR) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_AUDIO_BEHAVIOR)
			)
		}

		item {
			var audioNightMode by rememberPreference(userPreferences, UserPreferences.audioNightMode)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_audio_night_mode)) },
				captionContent = { Text(stringResource(R.string.settings_night_audio_description)) },
				trailingContent = { Checkbox(checked = audioNightMode) },
				onClick = { audioNightMode = !audioNightMode },
				modifier = Modifier.focusKey("audio_night_mode")
			)
		}

		item {
			var preferExoPlayerFfmpeg by rememberPreference(userPreferences, UserPreferences.preferExoPlayerFfmpeg)
			ListButton(
				headingContent = { Text(stringResource(R.string.prefer_exoplayer_ffmpeg)) },
				trailingContent = { Checkbox(checked = preferExoPlayerFfmpeg) },
				captionContent = { Text(stringResource(R.string.prefer_exoplayer_ffmpeg_content)) },
				onClick = { preferExoPlayerFfmpeg = !preferExoPlayerFfmpeg },
				modifier = Modifier.focusKey("prefer_exoplayer_ffmpeg")
			)
		}

	}
}
