package org.jellyfin.androidtv.ui.settings.screen.playback

import org.jellyfin.androidtv.ui.settings.composable.SettingsValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.StillWatchingBehavior
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackAutoplayScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var mediaQueuingEnabled by rememberPreference(userPreferences, UserPreferences.mediaQueuingEnabled)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_autoplay)) },
			)
		}

		item {
			ListButton(
				toggleState = mediaQueuingEnabled,
				headingContent = { Text(stringResource(R.string.pref_media_queueing)) },
				trailingContent = { SettingsSwitch(checked = mediaQueuingEnabled) },
				captionContent = { Text(stringResource(R.string.pref_media_queueing_description)) },
				onClick = { mediaQueuingEnabled = !mediaQueuingEnabled },
				modifier = Modifier.focusKey("media_queuing_enabled")
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_next_up), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_next_up)) },
				captionContent = { Text(stringResource(R.string.next_up_settings_description)) },
				onClick = { router.push(Routes.PLAYBACK_NEXT_UP) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_NEXT_UP)
			)
		}

		item {
			val stillWatchingBehavior by rememberPreference(userPreferences, UserPreferences.stillWatchingBehavior)
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_zzz), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_inactivity_prompt)) },
				// The value stays on the right; the note about Autoplay being off goes underneath
				captionContent = if (mediaQueuingEnabled || stillWatchingBehavior == StillWatchingBehavior.DISABLED) null else ({
					Text(stringResource(R.string.still_watching_autoplay_off, stringResource(stillWatchingBehavior.nameRes)))
				}),
				trailingContent = { SettingsValue(stringResource(stillWatchingBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_INACTIVITY_PROMPT) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_INACTIVITY_PROMPT)
			)
		}

	}
}
