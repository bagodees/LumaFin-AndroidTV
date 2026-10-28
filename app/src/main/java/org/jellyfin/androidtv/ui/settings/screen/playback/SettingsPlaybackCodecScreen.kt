package org.jellyfin.androidtv.ui.settings.screen.playback

import org.jellyfin.androidtv.ui.settings.composable.SettingsValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.BitstreamAudioFormat
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.AndroidVersion
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackCodecScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.preference_codecs)) },
				captionContent = { Text(stringResource(R.string.preference_codecs_summary)) },
			)
		}

		item {
			var userAVCLevel by rememberPreference(userPreferences, UserPreferences.userAVCLevel)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.user_avc_level)) },
				trailingContent = { SettingsValue(stringResource(userAVCLevel.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_AVC_LEVEL) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_AVC_LEVEL)
			)
		}

		item {
			var userHEVCLevel by rememberPreference(userPreferences, UserPreferences.userHEVCLevel)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.user_hevc_level)) },
				trailingContent = { SettingsValue(stringResource(userHEVCLevel.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_HEVC_LEVEL) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_HEVC_LEVEL)
			)
		}

		items(BitstreamAudioFormat.entries.size) { index ->
			val format = BitstreamAudioFormat.entries[index]
			val mode by rememberPreference(userPreferences, format.preference)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(format.nameRes)) },
				trailingContent = { SettingsValue(stringResource(mode.nameRes)) },
				onClick = {
					router.push(
						route = Routes.PLAYBACK_BITSTREAM_AUDIO,
						parameters = mapOf("format" to format.name),
					)
				},
				modifier = Modifier.focusKey("bitstream_audio_${format.name}"),
			)
		}

		if (AndroidVersion.isAtLeastQ) {
			item {
				var softwareCodecsEnabled by rememberPreference(userPreferences, UserPreferences.softwareCodecsEnabled)

				ListButton(

					toggleState = softwareCodecsEnabled,
					headingContent = { Text(stringResource(R.string.pref_software_codecs_enabled)) },
					captionContent = { Text(stringResource(R.string.pref_software_codecs_enabled_description)) },
					trailingContent = { SettingsSwitch(checked = softwareCodecsEnabled) },
					onClick = { softwareCodecsEnabled = !softwareCodecsEnabled },
					modifier = Modifier.focusKey("software_codecs_enabled")
				)
			}
		}
	}
}
