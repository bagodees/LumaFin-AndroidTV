package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.isTvDevice
import org.koin.compose.koinInject

@Composable
fun SettingsDeveloperScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val systemPreferences = koinInject<SystemPreferences>()
	val context = LocalContext.current
	val isTvDevice = remember(context) { context.isTvDevice() }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_about_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_developer_link)) },
			)
		}

		item {
			// Legacy debug flag
			// Not in use by much components anymore
			var debuggingEnabled by rememberPreference(userPreferences, UserPreferences.debuggingEnabled)
			ListButton(
				toggleState = debuggingEnabled,
				headingContent = { Text(stringResource(R.string.lbl_enable_debug)) },
				trailingContent = { SettingsSwitch(checked = debuggingEnabled) },
				captionContent = { Text(stringResource(R.string.desc_debug)) },
				onClick = { debuggingEnabled = !debuggingEnabled },
				modifier = Modifier.focusKey("debugging_enabled")
			)
		}

		// UI Mode toggle
		if (!isTvDevice) item {
			var disableUiModeWarning by rememberPreference(systemPreferences, SystemPreferences.disableUiModeWarning)
			ListButton(
				toggleState = disableUiModeWarning,
				headingContent = { Text(stringResource(R.string.disable_ui_mode_warning)) },
				trailingContent = { SettingsSwitch(checked = disableUiModeWarning) },
				onClick = { disableUiModeWarning = !disableUiModeWarning },
				modifier = Modifier.focusKey("disable_ui_mode_warning")
			)
		}

	}
}
