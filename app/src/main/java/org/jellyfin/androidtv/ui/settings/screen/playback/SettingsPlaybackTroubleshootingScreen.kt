package org.jellyfin.androidtv.ui.settings.screen.playback

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsAsyncActionListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.profile.createDeviceProfileReport
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.jellyfin.sdk.model.ServerVersion
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackTroubleshootingScreen() {
	val context = LocalContext.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_troubleshooting)) },
				captionContent = { Text(stringResource(R.string.settings_troubleshooting_description)) },
			)
		}

		item {
			val api = koinInject<ApiClient>()
			val serverVersion = koinInject<ServerVersion>()

			SettingsAsyncActionListButton(
				headingContent = { Text(stringResource(R.string.pref_report_device_profile_title)) },
				captionContent = { Text(stringResource(R.string.pref_report_device_profile_summary)) },
				action = {
					val report = createDeviceProfileReport(context, userPreferences, serverVersion)
					val response by api.clientLogApi.logFile(report)
					response
				},
				onSuccess = { result ->
					Toast.makeText(
						context,
						@SuppressLint("LocalContextGetResourceValueCall")
						context.getString(R.string.pref_report_device_profile_success, result.fileName),
						Toast.LENGTH_LONG
					).show()
				},
				onFailure = {
					Toast.makeText(context, R.string.pref_report_device_profile_failure, Toast.LENGTH_LONG).show()
				},
				modifier = Modifier.focusKey("report_device_profile")
			)
		}
	}
}
