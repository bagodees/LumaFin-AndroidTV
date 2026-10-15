package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes

@Composable
fun SettingsMainScreen() {
	val router = LocalRouter.current
	SettingsSearchScreen(mainMenu = {
		items(settingsMainGroups.size, key = { settingsMainGroups[it].route }) { index ->
			val group = settingsMainGroups[index]
			ListButton(
				leadingContent = { Icon(painterResource(group.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
				headingContent = { Text(stringResource(group.title)) },
				captionContent = { Text(stringResource(group.description)) },
				onClick = { router.push(group.route) },
				modifier = Modifier.focusKey(group.route, initialFocus = index == 0),
			)
		}
	})
}

private data class SettingsMainGroup(val route: String, val title: Int, val description: Int, val icon: Int)

private val settingsMainGroups = listOf(
	SettingsMainGroup(Routes.PLAYBACK, R.string.pref_playback, R.string.settings_group_playback_description, R.drawable.ic_next),
	SettingsMainGroup(Routes.CUSTOMIZATION, R.string.settings_group_appearance, R.string.settings_group_appearance_description, R.drawable.ic_adjust),
	SettingsMainGroup(Routes.AUTHENTICATION, R.string.settings_group_accounts, R.string.settings_group_accounts_description, R.drawable.ic_users),
	SettingsMainGroup(Routes.HELP, R.string.settings_group_help, R.string.settings_group_help_description, R.drawable.ic_maintenance),
)