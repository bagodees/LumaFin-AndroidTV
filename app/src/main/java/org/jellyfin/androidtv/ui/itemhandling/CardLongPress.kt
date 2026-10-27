package org.jellyfin.androidtv.ui.itemhandling

import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.CardLongPressAction
import org.jellyfin.androidtv.ui.home.HomeShortcutRowItem
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.KeyProcessor
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.core.context.GlobalContext

/** Preserve specialized menus on libraries, unavailable pins, music, queue cards and other controls. */
internal fun cardLongPressAction(item: BaseRowItem, configured: CardLongPressAction): CardLongPressAction {
	if (item.baseRowType != BaseRowType.BaseItem || item is HomeShortcutRowItem && !item.available) return CardLongPressAction.MENU
	return if (item.baseItem?.type in setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES)) configured else CardLongPressAction.MENU
}

internal fun handleCardLongPress(view: View, item: BaseRowItem, configured: CardLongPressAction): Boolean {
	val activity = view.context.getActivity() ?: return true
	fun menu() { activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) }
	when (cardLongPressAction(item, configured)) {
		CardLongPressAction.MENU -> menu()
		CardLongPressAction.DETAILS -> item.itemId?.let { GlobalContext.get().get<NavigationRepository>().navigate(Destinations.itemDetails(it)) }
		CardLongPressAction.PLAY -> {
			val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity
			if (fragmentActivity == null || !GlobalContext.get().get<KeyProcessor>().handleKey(KeyEvent.KEYCODE_MEDIA_PLAY, item, fragmentActivity)) menu()
		}
		CardLongPressAction.DISABLED -> Unit
	}
	// Consume the hold even when disabled/unsupported, so release cannot also trigger a short click.
	return true
}

/**
 * Tells people once that title cards have a menu (#84), the first time one is focused. Only while the
 * long-press shortcut opens the menu; anyone who changed it already knows.
 */
internal fun showCardMenuHintOnce(view: View, item: BaseRowItem) {
	val preferences = GlobalContext.get().get<UserPreferences>()
	if (preferences[UserPreferences.cardMenuHintSeen]) return
	if (item.baseRowType != BaseRowType.BaseItem || item.baseItem?.type !in setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES)) return
	if (preferences[UserPreferences.cardLongPressAction] != CardLongPressAction.MENU) return
	preferences[UserPreferences.cardMenuHintSeen] = true
	Toast.makeText(view.context, R.string.card_menu_hint, Toast.LENGTH_LONG).show()
}
