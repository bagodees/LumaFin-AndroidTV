package org.jellyfin.androidtv.ui.settings.screen

import android.text.format.DateUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SettingsSync
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.SettingsSyncReviewDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsSyncScreen() {
	val context = LocalContext.current
	val sync = koinInject<SettingsSync>()
	val state by sync.state.collectAsState()
	val scope = rememberCoroutineScope()
	var busy by remember { mutableStateOf(false) }

	fun report(outcome: SettingsSync.Outcome?) {
		val message = when (outcome) {
			is SettingsSync.Outcome.Failed -> R.string.settings_sync_failed
			SettingsSync.Outcome.AccountChanged -> R.string.settings_sync_account_changed
			else -> null
		}
		message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
	}

	fun run(action: suspend () -> SettingsSync.Outcome) {
		if (busy) return
		busy = true
		scope.launch {
			val outcome = try { action() } finally { busy = false }
			if (outcome is SettingsSync.Outcome.NeedsReview) SettingsSyncReviewDialog.show(context, scope, sync, outcome.review, ::report)
			else report(outcome)
		}
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings_group_help).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings_sync_title)) },
				captionContent = { Text(stringResource(R.string.settings_sync_description)) },
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.settings_sync_toggle)) },
				captionContent = {
					Text(when {
						busy || state.syncing -> stringResource(R.string.settings_sync_working)
						!state.enabled -> stringResource(R.string.settings_sync_off_caption)
						state.failed -> stringResource(R.string.settings_sync_failed_short)
						state.lastSynced == null -> stringResource(R.string.settings_sync_working)
						System.currentTimeMillis() - state.lastSynced!! < DateUtils.MINUTE_IN_MILLIS -> stringResource(R.string.settings_sync_just_now)
						else -> stringResource(R.string.settings_sync_last, DateUtils.getRelativeTimeSpanString(state.lastSynced!!).toString())
					})
				},
				trailingContent = { Checkbox(checked = state.enabled) },
				onClick = { if (state.enabled) sync.disable() else run { sync.enable() } },
				modifier = Modifier.focusKey("settings_sync_toggle"),
			)
		}
		state.review?.takeIf { !it.enabling }?.let { review ->
			item {
				ListButton(
					headingContent = { Text(stringResource(R.string.settings_sync_conflict_title)) },
					captionContent = { Text(context.resources.getQuantityString(R.plurals.settings_sync_conflicts, review.differences.size, review.differences.size)) },
					onClick = { SettingsSyncReviewDialog.show(context, scope, sync, review, ::report) },
					modifier = Modifier.focusKey("settings_sync_review"),
				)
			}
		}
		if (state.enabled) item {
			ListButton(
				headingContent = { Text(stringResource(R.string.settings_sync_now)) },
				enabled = !busy,
				onClick = { run { sync.sync() } },
				modifier = Modifier.focusKey("settings_sync_now"),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.settings_sync_remove)) },
				captionContent = { Text(stringResource(R.string.settings_sync_remove_description)) },
				enabled = !busy,
				onClick = {
					AlertDialog.Builder(context)
						.setMessage(R.string.settings_sync_remove_confirm)
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(R.string.settings_sync_remove) { _, _ ->
							run { sync.removeRemote() }
						}
						.show()
				},
				modifier = Modifier.focusKey("settings_sync_remove"),
			)
		}
		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.settings_sync_scope_heading)) },
				captionContent = { Text(stringResource(R.string.settings_sync_scope)) },
			)
		}
	}
}
