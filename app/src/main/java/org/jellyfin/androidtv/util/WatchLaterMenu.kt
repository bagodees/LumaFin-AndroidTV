package org.jellyfin.androidtv.util

import android.content.Context
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.preference.WatchLaterPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.context.GlobalContext

fun toggleWatchLater(context: Context, item: BaseItemDto) {
	val preferences = GlobalContext.get().get<WatchLaterPreferences>()
	if (!preferences.canSave(item)) return
	val saved = preferences.contains(item.id)
	if (saved) preferences.remove(item.id.toString()) else {
		preferences.add(item)
		if (!preferences.initialized) {
			val home = GlobalContext.get().get<UserSettingPreferences>()
			if (HomeSectionType.WATCH_LATER !in home.activeHomesections) {
				home.activeHomesections = home.activeHomesections + HomeSectionType.WATCH_LATER
			}
			preferences.markInitialized()
		}
	}
	GlobalContext.get().get<CustomMessageRepository>().pushMessage(CustomMessage.HomeShortcutsChanged)
	Toast.makeText(context, if (saved) R.string.watch_later_removed else R.string.watch_later_added, Toast.LENGTH_SHORT).show()
}

fun addWatchLaterMenu(menu: PopupMenu, activity: FragmentActivity, item: BaseItemDto) {
	val preferences = GlobalContext.get().get<WatchLaterPreferences>()
	if (!preferences.canSave(item)) return
	menu.menu.add(0, 1004, 30, if (preferences.contains(item.id)) R.string.watch_later_remove else R.string.watch_later_add)
		.setOnMenuItemClickListener { toggleWatchLater(activity, item); true }
}
