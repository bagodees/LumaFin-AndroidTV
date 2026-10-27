@file:JvmName("LumaFinActionMenu")

package org.jellyfin.androidtv.ui.base.dialog

import android.view.View
import android.widget.PopupMenu
import androidx.core.view.iterator
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.getActivity

/**
 * Shows a pop-up menu's items in LumaFin's menu panel instead of the standard Android pop-up (#84).
 * Items keep their order and actions: choosing one closes the panel and runs it exactly as the
 * pop-up would. Focus returns to the card or button that was focused when the panel closes.
 */
@JvmOverloads
fun PopupMenu.showLumaFin(anchor: View?, title: String? = null): Boolean {
	val items = menu.iterator().asSequence().filter { it.isVisible }.toList()
	if (items.isEmpty()) return false
	val context = anchor?.context?.getActivity() ?: return false
	val returnFocus = (context as? android.app.Activity)?.currentFocus ?: anchor
	val owner = context as? LifecycleOwner
	LumaFinMenuDialog(context, owner, title ?: context.getString(R.string.action_menu_title), fitContent = true).apply {
		for (item in items) {
			row(item.title.toString(), navigates = false, enabled = item.isEnabled) {
				dismiss()
				menu.performIdentifierAction(item.itemId, 0)
			}
		}
		onDismiss { returnFocus?.takeIf { it.isAttachedToWindow }?.requestFocus() }
		show()
	}
	return true
}

/** [showLumaFin] for menus built against an activity, titled with the item's name. */
fun PopupMenu.showLumaFin(activity: FragmentActivity, title: String?): Boolean =
	showLumaFin(activity.currentFocus ?: activity.window.decorView, title)
