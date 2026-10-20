package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.StreamingQuality
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackQualityScreen() {
	val router = LocalRouter.current
	val preferences = koinInject<UserPreferences>()
	var quality by rememberPreference(preferences, UserPreferences.streamingQuality)
	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_streaming_quality)) },
				captionContent = { Text(stringResource(R.string.quality_presets_description)) },
			)
		}
		items(StreamingQuality.entries.filter { it != StreamingQuality.CUSTOM }) { option ->
			ListButton(
				headingContent = { Text(stringResource(option.nameRes)) },
				captionContent = { Text(stringResource(option.descriptionRes)) },
				trailingContent = { RadioButton(checked = quality == option) },
				onClick = { quality = option; router.back() },
				modifier = Modifier.focusKey("quality_${option.name}", initialFocus = quality == option),
			)
		}
		if (quality == StreamingQuality.CUSTOM) item {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.quality_custom)) },
				captionContent = { Text(stringResource(R.string.quality_custom_description)) },
				trailingContent = { RadioButton(checked = true) },
				onClick = { router.push(Routes.PLAYBACK_MAX_BITRATE) },
				modifier = Modifier.focusKey("quality_custom"),
			)
		}
	}
}
