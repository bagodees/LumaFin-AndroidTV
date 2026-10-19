package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn

@Composable
fun SettingsHelpScreen() {
	val router = LocalRouter.current
	SettingsColumn {
		item {
			ListSection(overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings_group_help)) },
				captionContent = { Text(stringResource(R.string.settings_group_help_description)) })
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_guide_title)) },
				captionContent = { Text(stringResource(R.string.setup_guide_revisit)) },
				onClick = { router.push(Routes.SETUP_GUIDE) }, modifier = Modifier.focusKey(Routes.SETUP_GUIDE))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.maintenance_title)) },
				captionContent = { Text(stringResource(R.string.settings_search_cache)) },
				onClick = { router.push(Routes.MAINTENANCE) }, modifier = Modifier.focusKey(Routes.MAINTENANCE))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.backup_title)) },
				captionContent = { Text(stringResource(R.string.backup_scope)) },
				onClick = { router.push(Routes.SETTINGS_BACKUP) }, modifier = Modifier.focusKey(Routes.SETTINGS_BACKUP))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.settings_sync_title)) },
				captionContent = { Text(stringResource(R.string.settings_sync_description)) },
				onClick = { router.push(Routes.SETTINGS_SYNC) }, modifier = Modifier.focusKey(Routes.SETTINGS_SYNC))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.speed_test_title)) },
				captionContent = { Text(stringResource(R.string.speed_test_description)) },
				onClick = { router.push(Routes.STREAMING_SPEED) }, modifier = Modifier.focusKey(Routes.STREAMING_SPEED))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.pref_telemetry_category)) },
				captionContent = { Text(stringResource(R.string.settings_search_telemetry)) },
				onClick = { router.push(Routes.TELEMETRY) }, modifier = Modifier.focusKey(Routes.TELEMETRY))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.export_title)) },
				captionContent = { Text(stringResource(R.string.export_scope)) },
				onClick = { router.push(Routes.DIAGNOSTIC_EXPORT) }, modifier = Modifier.focusKey(Routes.DIAGNOSTIC_EXPORT))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.pref_about_title)) },
				captionContent = { Text(stringResource(R.string.settings_search_about)) },
				onClick = { router.push(Routes.ABOUT) }, modifier = Modifier.focusKey(Routes.ABOUT))
		}
	}
}
