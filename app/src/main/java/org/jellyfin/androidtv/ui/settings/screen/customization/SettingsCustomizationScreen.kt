package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.composable.SettingsValue
import org.koin.compose.koinInject
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection

/** Appearance as a short list of groups (#125), each opening its own page and showing its main value. */
@Composable
fun SettingsCustomizationScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val appTheme by rememberPreference(userPreferences, UserPreferences.appTheme)
	val cardSize by rememberPreference(userPreferences, UserPreferences.mediaCardSize)
	val hideDescriptions by rememberPreference(userPreferences, UserPreferences.hideUnwatchedEpisodeDescriptions)
	val hideArtwork by rememberPreference(userPreferences, UserPreferences.hideUnwatchedEpisodeArtwork)
	val textScale by rememberPreference(userPreferences, UserPreferences.textScale)
	val screensaver by rememberPreference(userPreferences, UserPreferences.screensaverInAppEnabled)
	val longPress by rememberPreference(userPreferences, UserPreferences.cardLongPressAction)
	fun onOff(on: Boolean) = if (on) R.string.settings_search_on else R.string.settings_search_off

	@Composable
	fun Group(route: String, icon: Int, title: Int, description: Int, value: String) = ListButton(
		navigates = true,
		leadingContent = { Icon(painterResource(icon), contentDescription = null) },
		headingContent = { Text(stringResource(title)) },
		captionContent = { Text(stringResource(description)) },
		trailingContent = { SettingsValue(value) },
		onClick = { router.push(route) },
		modifier = Modifier.focusKey(route),
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings_group_appearance)) },
			)
		}
		item { Group(Routes.CUSTOMIZATION_STYLE, R.drawable.ic_adjust, R.string.appearance_style, R.string.appearance_style_description, stringResource(appTheme.nameRes)) }
		item { Group(Routes.CUSTOMIZATION_CARDS, R.drawable.ic_grid, R.string.appearance_cards, R.string.appearance_cards_description, stringResource(cardSize.nameRes)) }
		item { Group(Routes.CUSTOMIZATION_SPOILERS, R.drawable.ic_eye_off, R.string.appearance_spoilers, R.string.appearance_spoilers_description, stringResource(onOff(hideDescriptions || hideArtwork))) }
		item { Group(Routes.CUSTOMIZATION_ACCESSIBILITY, R.drawable.ic_abc, R.string.accessibility_title, R.string.appearance_accessibility_description, stringResource(textScale.nameRes)) }

		item { ListSection(headingContent = { Text(stringResource(R.string.appearance_more)) }) }
		item { Group(Routes.CUSTOMIZATION_SCREENSAVER, R.drawable.ic_photos, R.string.pref_screensaver, R.string.settings_search_screensaver, stringResource(onOff(screensaver))) }
		item { Group(Routes.REMOTE_SHORTCUTS, R.drawable.ic_more, R.string.remote_shortcuts, R.string.remote_shortcuts_description, stringResource(longPress.nameRes)) }
	}
}
