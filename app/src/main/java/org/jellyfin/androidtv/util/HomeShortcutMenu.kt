package org.jellyfin.androidtv.util

import android.widget.PopupMenu
import android.widget.Toast
import android.graphics.Rect
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.preference.HomeShortcutPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject

fun addHomeShortcutMenu(menu: PopupMenu, activity: FragmentActivity, item: BaseItemDto) {
	val preferences by activity.inject<HomeShortcutPreferences>()
	val pinned = preferences.isPinned(item.id)
	if (!pinned && !preferences.canPin(item)) return
	menu.menu.add(0, 1003, 40, if (pinned) R.string.home_shortcut_unpin else R.string.home_shortcut_pin)
		.setOnMenuItemClickListener {
			if (pinned) preferences.unpin(item.id.toString()) else preferences.pin(item)
			val messages by activity.inject<CustomMessageRepository>()
			messages.pushMessage(CustomMessage.HomeShortcutsChanged)
			Toast.makeText(activity, if (pinned) R.string.home_shortcut_unpinned else R.string.home_shortcut_pinned, Toast.LENGTH_SHORT).show()
			true
		}
}

fun showHomeShortcutPopup(activity: FragmentActivity, item: BaseItemDto) {
	val anchor = activity.currentFocus ?: return
	val preferences by activity.inject<HomeShortcutPreferences>()
	if (!preferences.canPin(item)) return
	val pinned = preferences.isPinned(item.id)
	val bounds = Rect()
	if (!anchor.getGlobalVisibleRect(bounds)) return
	val position = IntArray(2).also(anchor::getLocationOnScreen)
	val density = activity.resources.displayMetrics.density
	val gap = (8 * density).toInt()
	val action = LayoutInflater.from(activity).inflate(R.layout.home_shortcut_menu_item, null) as TextView
	action.setText(if (pinned) R.string.home_shortcut_unpin else R.string.home_shortcut_pin)
	action.isFocusable = true
	action.isFocusableInTouchMode = true
	// A single action needs no list container, padding or platform popup shadow.
	val popup = PopupWindow(action, bounds.width(), (48 * density).toInt(), true).apply {
		setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		elevation = 0f
		isOutsideTouchable = true
	}
	action.setOnClickListener {
		if (pinned) preferences.unpin(item.id.toString()) else preferences.pin(item)
		val messages by activity.inject<CustomMessageRepository>()
		messages.pushMessage(CustomMessage.HomeShortcutsChanged)
		popup.dismiss()
		Toast.makeText(activity, if (pinned) R.string.home_shortcut_unpinned else R.string.home_shortcut_pinned, Toast.LENGTH_SHORT).show()
	}
	// Include Leanback's focus scale when positioning below the card and caption.
	popup.showAsDropDown(anchor, bounds.left - position[0], bounds.bottom - position[1] - anchor.height + gap, Gravity.START)
	action.requestFocus()
}
