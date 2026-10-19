package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.CardLongPressAction
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsRemoteShortcutsScreen() {
	val preferences = koinInject<UserPreferences>()
	val router = LocalRouter.current
	var action by rememberPreference(preferences, UserPreferences.cardLongPressAction)
	SettingsColumn {
		item { ListSection(overlineContent = { Text(stringResource(R.string.settings_group_appearance).uppercase()) },
			headingContent = { Text(stringResource(R.string.remote_shortcuts)) },
			captionContent = { Text(stringResource(R.string.remote_shortcuts_help)) }) }
		items(CardLongPressAction.entries) { option ->
			ListButton(headingContent = { Text(stringResource(option.nameRes)) },
				captionContent = { Text(stringResource(option.descriptionRes)) },
				trailingContent = { RadioButton(checked = action == option) },
				modifier = Modifier.focusKey("remote_hold_${option.name}", initialFocus = action == option),
				onClick = { action = option; router.back() })
		}
	}
}
