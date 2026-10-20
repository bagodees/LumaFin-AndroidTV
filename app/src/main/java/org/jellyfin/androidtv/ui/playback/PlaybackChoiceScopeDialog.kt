package org.jellyfin.androidtv.ui.playback

import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.PlaybackChoicePreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.core.context.GlobalContext
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog

/** Shared by the details track chips and the player track menus; shown only once something is remembered. */
object PlaybackChoiceScopeDialog {
	private val choices get() = GlobalContext.get().get<PlaybackChoicePreferences>()

	fun available(item: BaseItemDto?) = item != null && choices.hasChoices(item)

	private fun label(context: Context, item: BaseItemDto, scope: PlaybackChoiceScope) = context.getString(when (scope) {
		PlaybackChoiceScope.SERIES -> R.string.playback_choices_series
		PlaybackChoiceScope.SEASON -> R.string.playback_choices_season
		PlaybackChoiceScope.TITLE -> if (item.type == BaseItemKind.EPISODE) R.string.playback_choices_episode else R.string.playback_choices_movie
	})

	fun menuLabel(context: Context, item: BaseItemDto): String {
		val scope = choices.scope(item) ?: PlaybackChoiceScope.TITLE
		return context.getString(R.string.playback_choices_menu, label(context, item, scope))
	}

	internal fun showModern(context: Context, owner: LifecycleOwner, item: BaseItemDto,
		onChanged: () -> Unit = {}, onDismiss: () -> Unit = {}): LumaFinMenuDialog {
		val scopes = PlaybackChoiceKeys.forItem(item).map { it.first }.reversed()
		val current = choices.scope(item)
		return LumaFinMenuDialog(context, owner, context.getString(R.string.playback_choices_title),
			context.getString(R.string.playback_choices_help), fitContent = true).apply {
			if (scopes.size > 1) scopes.forEach { scope -> row(label(context, item, scope), selected = scope == current) {
				if (scope != current) { choices.moveTo(item, scope); onChanged() }
				dismiss()
			} }
			row(context.getString(R.string.playback_choices_forget), navigates = false) {
				choices.reset(item); onChanged(); dismiss()
			}
			button(context.getString(android.R.string.cancel)) { dismiss() }
			onDismiss(onDismiss)
			show(scopes.indexOf(current).coerceAtLeast(0))
		}
	}

	fun show(context: Context, item: BaseItemDto, onChanged: () -> Unit = {}, onDismiss: () -> Unit = {}): AlertDialog {
		val scopes = PlaybackChoiceKeys.forItem(item).map { it.first }.reversed()
		val current = scopes.indexOf(choices.scope(item)).coerceAtLeast(0)
		val builder = AlertDialog.Builder(context)
			.setTitle(R.string.playback_choices_title)
			.setNegativeButton(android.R.string.cancel, null)
			.setNeutralButton(R.string.playback_choices_forget) { _, _ ->
				choices.reset(item)
				onChanged()
			}
		// Movies have a single level, so there is nothing to choose between.
		if (scopes.size > 1) {
			builder.setSingleChoiceItems(scopes.map { label(context, item, it) }.toTypedArray(), current) { dialog, which ->
				if (which != current) {
					choices.moveTo(item, scopes[which])
					onChanged()
				}
				dialog.dismiss()
			}
		} else {
			builder.setMessage(R.string.playback_choices_help)
		}
		return builder.create().apply {
			setOnDismissListener { onDismiss() }
			show()
		}
	}
}
