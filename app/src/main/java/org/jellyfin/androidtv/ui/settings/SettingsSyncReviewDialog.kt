package org.jellyfin.androidtv.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SettingsSync
import org.jellyfin.androidtv.preference.SettingsSyncModel

/** Lists only the settings that differ, with this TV's and the synced value, and applies one choice to all of them. */
object SettingsSyncReviewDialog {
	fun show(context: Context, scope: CoroutineScope, sync: SettingsSync, review: SettingsSync.Review, onDone: (SettingsSync.Outcome?) -> Unit = {}): AlertDialog {
		val lines = review.differences.entries.joinToString("\n\n") { (key, values) ->
			val name = SettingsValueLabels.labels[key]?.let(context::getString) ?: key
			context.getString(R.string.settings_sync_difference, name,
				SettingsValueLabels.display(context, key, values.first), SettingsValueLabels.display(context, key, values.second))
		}
		val intro = context.getString(if (review.enabling) R.string.settings_sync_enable_review else R.string.settings_sync_conflict_review)
		fun apply(resolution: SettingsSyncModel.Resolution) = scope.launch {
			val outcome = sync.resolve(review, resolution)
			if (outcome is SettingsSync.Outcome.Failed) Toast.makeText(context, R.string.settings_sync_failed, Toast.LENGTH_LONG).show()
			onDone(outcome)
		}
		var answered = false
		return AlertDialog.Builder(context)
			.setTitle(if (review.enabling) R.string.settings_sync_enable_title else R.string.settings_sync_conflict_title)
			.setMessage("$intro\n\n$lines")
			.setPositiveButton(if (review.enabling) R.string.settings_sync_use_synced_all else R.string.settings_sync_use_synced) { _, _ ->
				answered = true
				apply(SettingsSyncModel.Resolution.USE_SYNCED)
			}
			.setNeutralButton(if (review.enabling) R.string.settings_sync_replace_synced else R.string.settings_sync_keep_this_tv) { _, _ ->
				answered = true
				apply(SettingsSyncModel.Resolution.KEEP_THIS_TV)
			}
			.setNegativeButton(android.R.string.cancel, null)
			.setOnDismissListener {
				if (!answered) {
					sync.dismissReview()
					onDone(null)
				}
			}
			.show()
	}
}
