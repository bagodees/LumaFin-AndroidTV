package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsTelemetryScreen() {
	val telemetryPreferences = koinInject<TelemetryPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_telemetry_category)) },
			)
		}

		item {
			var crashReportEnabled by rememberPreference(telemetryPreferences, TelemetryPreferences.crashReportEnabled)
			ListButton(
				toggleState = crashReportEnabled,
				headingContent = { Text(stringResource(R.string.pref_crash_reports)) },
				trailingContent = { SettingsSwitch(checked = crashReportEnabled) },
				captionContent = {
					if (crashReportEnabled) Text(stringResource(R.string.pref_crash_reports_enabled))
					else Text(stringResource(R.string.pref_crash_reports_disabled))
				},
				onClick = { crashReportEnabled = !crashReportEnabled },
				modifier = Modifier.focusKey("crash_report_enabled")
			)
		}

		item {
			var crashReportIncludeLogs by rememberPreference(telemetryPreferences, TelemetryPreferences.crashReportIncludeLogs)
			ListButton(
				toggleState = crashReportIncludeLogs,
				headingContent = { Text(stringResource(R.string.pref_crash_report_logs)) },
				trailingContent = { SettingsSwitch(checked = crashReportIncludeLogs) },
				captionContent = {
					if (crashReportIncludeLogs) Text(stringResource(R.string.pref_crash_report_logs_enabled))
					else Text(stringResource(R.string.pref_crash_report_logs_disabled))
				},
				onClick = { crashReportIncludeLogs = !crashReportIncludeLogs },
				modifier = Modifier.focusKey("crash_report_logs")
			)
		}
	}
}
