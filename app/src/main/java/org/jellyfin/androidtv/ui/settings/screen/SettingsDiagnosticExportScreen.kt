package org.jellyfin.androidtv.ui.settings.screen

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.diagnostics.DiagnosticProblem
import org.jellyfin.androidtv.diagnostics.DiagnosticSection
import org.jellyfin.androidtv.diagnostics.DiagnosticTrigger
import org.jellyfin.androidtv.diagnostics.collectDiagnosticSnapshot
import org.jellyfin.androidtv.diagnostics.diagnosticReport
import org.jellyfin.androidtv.diagnostics.diagnosticText
import org.jellyfin.androidtv.diagnostics.saveDiagnosticDownload
import org.jellyfin.androidtv.diagnostics.writeDiagnosticExport
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsDiagnosticExportScreen() {
	val context = LocalContext.current
	val preferences = koinInject<UserPreferences>()
	val servers = koinInject<ServerRepository>()
	val server by servers.currentServer.collectAsState()
	val router = LocalRouter.current
	val scope = rememberCoroutineScope()
	val list = rememberLazyListState()
	var problem by remember { mutableStateOf(DiagnosticProblem.CONNECTION) }
	var trigger by remember { mutableStateOf(DiagnosticTrigger.START) }
	var report by remember(preferences.accountRevision) { mutableStateOf<List<DiagnosticSection>?>(null) }
	var saving by remember { mutableStateOf(false) }
	var message by remember { mutableStateOf<Int?>(null) }
	var pendingText by remember { mutableStateOf<String?>(null) }
	val resolver = context.contentResolver
	val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
		val text = pendingText
		pendingText = null
		if (uri == null || text == null) {
			saving = false
			message = R.string.export_cancelled
		} else scope.launch {
			try {
				withContext(Dispatchers.IO) { writeDiagnosticExport(resolver, uri, text) }
				message = R.string.export_saved_document
			} catch (cancelled: CancellationException) { throw cancelled
			} catch (_: Exception) { message = R.string.export_failed
			} finally { saving = false }
		}
	}
	SettingsColumn(state = list) {
		item {
			ListSection(overlineContent = { Text(stringResource(R.string.settings_group_help).uppercase()) },
				headingContent = { Text(stringResource(R.string.export_title)) },
				captionContent = { Text(stringResource(if (report == null) R.string.export_scope else R.string.export_review)) })
		}
		if (report == null) {
			item {
				ListButton(headingContent = { Text(stringResource(R.string.export_problem)) },
					captionContent = { Text(stringResource(problem.label)) }, modifier = Modifier.focusKey("export_problem", initialFocus = true),
					onClick = { AlertDialog.Builder(context).setTitle(R.string.export_problem)
						.setSingleChoiceItems(DiagnosticProblem.entries.map { context.getString(it.label) }.toTypedArray(), problem.ordinal) { dialog, index ->
							problem = DiagnosticProblem.entries[index]; dialog.dismiss()
						}.setNegativeButton(android.R.string.cancel, null).show() })
			}
			item {
				ListButton(headingContent = { Text(stringResource(R.string.export_trigger)) },
					captionContent = { Text(stringResource(trigger.label)) }, modifier = Modifier.focusKey("export_trigger"),
					onClick = { AlertDialog.Builder(context).setTitle(R.string.export_trigger)
						.setSingleChoiceItems(DiagnosticTrigger.entries.map { context.getString(it.label) }.toTypedArray(), trigger.ordinal) { dialog, index ->
							trigger = DiagnosticTrigger.entries[index]; dialog.dismiss()
						}.setNegativeButton(android.R.string.cancel, null).show() })
			}
			item {
				ListButton(headingContent = { Text(stringResource(R.string.export_prepare)) }, modifier = Modifier.focusKey("export_prepare"),
					onClick = {
						if (!saving) {
							saving = true
							message = null
							// SharedPreferences reads and platform discovery are local; no network requests.
							scope.launch {
								try {
									val snapshot = withContext(Dispatchers.Default) {
										collectDiagnosticSnapshot(context, preferences, server?.version, server?.address?.startsWith("https://", ignoreCase = true))
									}
									report = diagnosticReport(snapshot, problem, trigger)
									list.scrollToItem(0)
								} catch (cancelled: CancellationException) { throw cancelled
								} catch (_: Exception) { message = R.string.export_failed
								} finally { saving = false }
							}
						}
					})
			}
		} else {
			itemsIndexed(report.orEmpty()) { index, section ->
				var focused by remember { mutableStateOf(false) }
				ListSection(headingContent = { Text(section.heading) }, captionContent = { Text(section.body) },
					modifier = Modifier.background(if (focused) Color(0xFF343840) else Color.Transparent, RoundedCornerShape(16.dp))
						.onFocusChanged { focused = it.isFocused }.focusKey("export_section_$index", initialFocus = index == 0).focusable())
			}
			item {
				ListButton(headingContent = { Text(stringResource(if (saving) R.string.export_saving else R.string.export_save)) },
					captionContent = { Text(stringResource(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.string.export_downloads else R.string.export_choose_file)) },
					modifier = Modifier.focusKey("export_save"), onClick = {
						if (!saving) {
							val text = report.orEmpty().diagnosticText()
							val filename = "LumaFin-diagnostics-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.txt"
							saving = true
							message = null
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) scope.launch {
								try {
									withContext(Dispatchers.IO) { saveDiagnosticDownload(resolver, filename, text) }
									message = R.string.export_saved_downloads
								} catch (cancelled: CancellationException) { throw cancelled
								} catch (_: Exception) { message = R.string.export_failed
								} finally { saving = false }
							} else {
								pendingText = text
								try { createDocument.launch(filename) }
								catch (_: ActivityNotFoundException) { saving = false; pendingText = null; message = R.string.export_no_picker }
							}
						}
						})
			}
		}
		message?.let { value -> item {
			var focused by remember { mutableStateOf(false) }
			ListSection(headingContent = { Text(stringResource(value)) }, modifier = Modifier
				.background(if (focused) Color(0xFF343840) else Color.Transparent, RoundedCornerShape(16.dp))
				.onFocusChanged { focused = it.isFocused }.focusable())
		} }
		item {
			ListButton(headingContent = { Text(stringResource(android.R.string.cancel)) }, modifier = Modifier.focusKey("export_cancel"),
				onClick = { router.back() })
		}
	}
}
