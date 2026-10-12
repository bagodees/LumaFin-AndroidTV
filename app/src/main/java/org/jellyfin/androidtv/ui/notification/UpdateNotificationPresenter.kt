package org.jellyfin.androidtv.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import org.jellyfin.androidtv.data.model.AppNotification
import org.jellyfin.androidtv.databinding.ViewCardUpdateNotificationBinding

/**
 * Shows highlighted notifications (the "update available" notice) as a full-width banner in
 * LumaFin's colors, so they stand out from the regular notification cards.
 */
class UpdateNotificationPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val binding = ViewCardUpdateNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		// Span the row, keeping the same inset on the right as the rows have on the left
		val inset = parent.paddingStart
		binding.root.layoutParams.width = parent.resources.displayMetrics.widthPixels - inset * 2
		return UpdateNotificationViewHolder(binding)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		viewHolder as UpdateNotificationViewHolder
		item as AppNotification

		viewHolder.binding.message.text = item.message
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit

	private class UpdateNotificationViewHolder(val binding: ViewCardUpdateNotificationBinding) : ViewHolder(binding.root)
}

/** Picks the banner for highlighted notifications and the regular card for the rest. */
class AppNotificationPresenterSelector : PresenterSelector() {
	private val cardPresenter = AppNotificationPresenter()
	private val bannerPresenter = UpdateNotificationPresenter()

	override fun getPresenter(item: Any?): Presenter =
		if ((item as? AppNotification)?.highlight == true) bannerPresenter else cardPresenter

	override fun getPresenters(): Array<Presenter> = arrayOf(cardPresenter, bannerPresenter)
}
