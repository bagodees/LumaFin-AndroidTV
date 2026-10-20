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
fun SettingsPlaybackLiveTvScreen() {
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_live_tv_cat)) },
				captionContent = { Text(stringResource(R.string.settings_live_direct_description)) },
			)
		}

		item {
			var liveTvDirectPlayEnabled by rememberPreference(userPreferences, UserPreferences.liveTvDirectPlayEnabled)

			ListButton(

				toggleState = liveTvDirectPlayEnabled,
				headingContent = { Text(stringResource(R.string.lbl_direct_stream_live)) },
				captionContent = { Text(stringResource(R.string.settings_live_direct_description)) },
				trailingContent = { SettingsSwitch(checked = liveTvDirectPlayEnabled) },
				onClick = { liveTvDirectPlayEnabled = !liveTvDirectPlayEnabled },
				modifier = Modifier.focusKey("live_tv_direct_play_enabled")
			)
		}

	}
}
