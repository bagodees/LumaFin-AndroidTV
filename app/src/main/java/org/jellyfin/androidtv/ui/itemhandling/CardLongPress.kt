package org.jellyfin.androidtv.ui.itemhandling

import android.view.KeyEvent
import android.view.View
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
