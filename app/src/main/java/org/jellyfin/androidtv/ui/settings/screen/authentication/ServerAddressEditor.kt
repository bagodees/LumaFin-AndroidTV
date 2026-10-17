package org.jellyfin.androidtv.ui.settings.screen.authentication

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.BackupAddressResult
import org.jellyfin.androidtv.auth.repository.ServerAddressRepository
import java.util.UUID

fun showServerAddressEditor(
	context: Context,
	scope: CoroutineScope,
	repository: ServerAddressRepository,
	serverId: UUID,
	current: String?,
	onSaved: suspend () -> Unit,
) {
	val address = EditText(context).apply {
		inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
		imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
		setSingleLine(true)
		setText(current.orEmpty())
		contentDescription = context.getString(R.string.server_backup_title)
	}
	val message = TextView(context).apply { setText(R.string.server_backup_description) }
	val spacing = (24 * context.resources.displayMetrics.density).toInt()
	val content = LinearLayout(context).apply {
		orientation = LinearLayout.VERTICAL
		setPadding(spacing, spacing / 2, spacing, spacing / 2)
		addView(message)
		addView(address)
	}
	val dialog = AlertDialog.Builder(context).setTitle(R.string.server_backup_title).setView(content)
		.setPositiveButton(R.string.server_backup_verify, null).setNegativeButton(android.R.string.cancel, null)
		.apply { if (current != null) setNeutralButton(R.string.server_backup_remove, null) }.create()
	var pending: kotlinx.coroutines.Job? = null
	dialog.setOnDismissListener { pending?.cancel() }
	dialog.setOnShowListener {
		val verify = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
		fun finishEditing() {
			(context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
				?.hideSoftInputFromWindow(address.windowToken, 0)
			address.clearFocus()
			verify.requestFocus()
			// Some TV keyboards release focus asynchronously after their Done action.
			verify.post { if (dialog.isShowing && verify.isEnabled) verify.requestFocus() }
		}
		address.setOnEditorActionListener { _, actionId, event ->
			val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER || event?.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
			if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || enter) {
				if (event == null || event.action == KeyEvent.ACTION_UP) finishEditing()
				true
			} else false
		}
		fun save(input: String) {
			if (pending?.isActive == true) return
			finishEditing()
			address.isEnabled = false
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
			dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = false
			dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
			message.setText(R.string.server_backup_checking)
			pending = scope.launch {
				when (val result = repository.saveBackup(serverId, input)) {
					is BackupAddressResult.Saved, BackupAddressResult.Removed -> {
						onSaved()
						dialog.dismiss()
					}
					else -> {
						message.setText(when (result) {
							BackupAddressResult.InvalidAddress -> R.string.server_backup_invalid
							BackupAddressResult.SameAddress -> R.string.server_backup_same
							BackupAddressResult.DifferentServer -> R.string.server_backup_different
							BackupAddressResult.MissingServerIdentity -> R.string.server_backup_no_identity
							BackupAddressResult.ServerNotFound -> R.string.server_backup_missing
							is BackupAddressResult.Unavailable -> result.failure.message
							else -> R.string.connection_unknown_message
						})
						address.isEnabled = true
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
						dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = true
						dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
						verify.requestFocus()
					}
				}
			}
		}
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { save(address.text.toString()) }
		dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener { save("") }
	}
	dialog.show()
	dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}
