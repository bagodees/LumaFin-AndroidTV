package org.jellyfin.androidtv.ui.settings.screen

import org.jellyfin.androidtv.ui.settings.SettingsValueLabels

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.AccountPreferences
import org.jellyfin.androidtv.preference.SettingsBackup
import org.jellyfin.androidtv.preference.SettingsBackupFiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.CardLongPressAction
import org.jellyfin.androidtv.preference.constant.BackdropBehavior
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.MediaCardSize
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.preference.PreferenceEnum
import org.koin.compose.koinInject

@Composable
fun SettingsBackupScreen() {
	val context = LocalContext.current
	val resolver = context.contentResolver
	val accounts = koinInject<AccountPreferences>()
	val preferences = koinInject<UserPreferences>()
	val home = koinInject<UserSettingPreferences>()
	val users = koinInject<UserRepository>()
	val servers = koinInject<ServerRepository>()
	val user by users.currentUser.collectAsState()
	val server by servers.currentServer.collectAsState()
	val router = LocalRouter.current
	val scope = rememberCoroutineScope()
	val list = rememberLazyListState()
	var busy by remember { mutableStateOf(false) }
	var message by remember { mutableStateOf<Int?>(null) }
	var pendingText by remember { mutableStateOf<String?>(null) }
	var target by remember(preferences.accountRevision) { mutableStateOf<AccountPreferences.BackupTarget?>(null) }
	var review by remember(preferences.accountRevision) { mutableStateOf<SettingsBackup.Review?>(null) }
	val changed = review?.values.orEmpty().filter { (key, value) -> target?.values?.get(key) != value }
	LaunchedEffect(review) { list.scrollToItem(0) }
	val labels = remember { SettingsValueLabels.labels }
	fun display(key: String, value: Any?) = SettingsValueLabels.display(context, key, value)
	fun load(uri: android.net.Uri) {
		if (busy) return
		val selected = runCatching { accounts.backupTarget() }.getOrElse { message = R.string.backup_sign_in; return }
		busy = true; message = null
		scope.launch {
			try {
				val imported = withContext(Dispatchers.IO) { SettingsBackup.review(SettingsBackupFiles.read(resolver, uri)) }
				check(accounts.backupTarget() == selected)
				target = selected; review = imported
			} catch (cancelled: CancellationException) { throw cancelled
			} catch (_: Exception) { message = R.string.backup_invalid
			} finally { busy = false }
		}
	}
	val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		if (uri == null) message = R.string.backup_import_cancelled else load(uri)
	}
	val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
		val text = pendingText; pendingText = null
		if (uri == null || text == null) busy = false
		else scope.launch {
			try {
				withContext(Dispatchers.IO) { SettingsBackupFiles.write(resolver, uri, text) }
				message = R.string.backup_saved_file
			} catch (cancelled: CancellationException) { throw cancelled
			} catch (_: Exception) { message = R.string.backup_failed
			} finally { busy = false }
		}
	}
	SettingsColumn(state = list) {
		item {
			ListSection(overlineContent = { Text(stringResource(R.string.settings_group_help).uppercase()) },
				headingContent = { Text(stringResource(R.string.backup_title)) },
				captionContent = { Text(stringResource(R.string.backup_scope)) })
		}
		item { ListSection(headingContent = { Text(stringResource(R.string.backup_destination, user?.name.orEmpty(), server?.name.orEmpty())) },
			captionContent = { Text(stringResource(R.string.backup_exclusions)) }) }
		if (review == null) {
			item {
				ListButton(headingContent = { Text(stringResource(if (busy) R.string.backup_working else R.string.backup_save)) },
					captionContent = { Text(stringResource(if (Build.VERSION.SDK_INT >= 29) R.string.backup_downloads else R.string.export_choose_file)) },
					modifier = Modifier.focusKey("backup_save", initialFocus = true), onClick = {
						if (!busy) {
							// Ensure legacy Home slots have been migrated before capturing the portable order.
							home.activeHomesections
							val snapshot = runCatching { SettingsBackup.encode(accounts.backupTarget().values) }.getOrNull()
							if (snapshot == null) message = R.string.backup_failed else {
								busy = true; message = null
								if (Build.VERSION.SDK_INT >= 29) scope.launch {
									try {
										withContext(Dispatchers.IO) { SettingsBackupFiles.save(resolver, snapshot) }
										message = R.string.backup_saved_downloads
									} catch (cancelled: CancellationException) { throw cancelled
									} catch (_: Exception) { message = R.string.backup_failed
									} finally { busy = false }
								} else {
									pendingText = snapshot
									try {
										val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
										if (intent.resolveActivity(context.packageManager) == null) throw ActivityNotFoundException()
										createDocument.launch(SettingsBackupFiles.filename())
									}
									catch (_: ActivityNotFoundException) { busy = false; pendingText = null; message = R.string.export_no_picker }
								}
							}
						}
					})
			}
			if (Build.VERSION.SDK_INT >= 29) item {
				ListButton(headingContent = { Text(stringResource(R.string.backup_restore_saved)) }, modifier = Modifier.focusKey("backup_local"), onClick = {
					if (!busy) {
						busy = true; message = null
						scope.launch {
							try {
								val saved = withContext(Dispatchers.IO) { SettingsBackupFiles.saved(resolver) }
								if (saved.isEmpty()) message = R.string.backup_none else AlertDialog.Builder(context)
									.setTitle(R.string.backup_restore_saved).setItems(saved.map { it.name }.toTypedArray()) { _, index -> load(saved[index].uri) }
									.setNegativeButton(android.R.string.cancel, null).show()
							} catch (cancelled: CancellationException) { throw cancelled
							} catch (_: Exception) { message = R.string.backup_failed
							} finally { busy = false }
						}
					}
				})
			}
			item {
				ListButton(headingContent = { Text(stringResource(R.string.backup_import)) }, modifier = Modifier.focusKey("backup_import"), onClick = {
					if (!busy) try {
						message = null
						val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
						if (intent.resolveActivity(context.packageManager) == null) throw ActivityNotFoundException()
						openDocument.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
					} catch (_: ActivityNotFoundException) { message = R.string.backup_no_picker }
				})
			}
		} else {
			item { ListSection(headingContent = { Text(stringResource(R.string.backup_review)) },
				captionContent = { Text(stringResource(R.string.backup_review_count, changed.size, review!!.values.size - changed.size, review!!.skipped)) }) }
			changed.forEach { (key, value) -> item(key = key) {
				ListButton(headingContent = { Text(stringResource(labels.getValue(key))) },
					captionContent = { Text(stringResource(R.string.backup_change, display(key, target!!.values[key]), display(key, value))) },
					modifier = Modifier.focusKey("backup_review_$key"), onClick = {})
			} }
			if (changed.isNotEmpty()) item {
				ListButton(headingContent = { Text(stringResource(R.string.backup_apply)) }, modifier = Modifier.focusKey("backup_apply"), onClick = {
					if (!busy) AlertDialog.Builder(context).setTitle(R.string.backup_apply)
						.setMessage(context.getString(R.string.backup_confirm, user?.name.orEmpty(), server?.name.orEmpty(), changed.size))
						.setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.backup_apply) { _, _ ->
							val selected = target; val imported = review
							if (!busy && selected != null && imported != null) {
								busy = true
								scope.launch {
									try {
										withContext(Dispatchers.IO) { accounts.restoreBackup(selected, imported) }
										review = null; target = null; message = R.string.backup_restored
									} catch (cancelled: CancellationException) { throw cancelled
									} catch (_: Exception) { review = null; target = null; message = R.string.backup_restore_failed
									} finally { busy = false }
								}
							}
						}.show()
				})
			}
			item { ListButton(headingContent = { Text(stringResource(android.R.string.cancel)) }, modifier = Modifier.focusKey("backup_cancel_review"),
				onClick = { if (!busy) { review = null; target = null; message = null } }) }
		}
		message?.let { id -> item { ListSection(headingContent = { Text(stringResource(id)) }) } }
		item { ListButton(headingContent = { Text(stringResource(R.string.backup_back)) }, modifier = Modifier.focusKey("backup_back"), onClick = { if (!busy) router.back() }) }
	}
}
