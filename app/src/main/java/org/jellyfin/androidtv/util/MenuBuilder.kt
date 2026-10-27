package org.jellyfin.androidtv.util

import android.content.Context
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.GravityInt
import org.jellyfin.androidtv.ui.base.dialog.showLumaFin

class MenuBuilder(
	private val menu: Menu
) {
	fun item(
		title: String,
		onClick: () -> Unit
	): MenuItem = menu.add(title).apply {
		setOnMenuItemClickListener {
			onClick()
			true
		}
	}

	fun subMenu(title: String, init: MenuBuilder.() -> Unit): SubMenu = menu.addSubMenu(title).apply {
		MenuBuilder(this).init()
	}
}

fun popupMenu(
	context: Context,
	anchor: View,
	@GravityInt gravity: Int = Gravity.NO_GRAVITY,
	init: MenuBuilder.() -> Unit
): PopupMenu = PopupMenu(context, anchor, gravity).apply {
	MenuBuilder(menu).init()
}

/** Shows the menu in LumaFin's menu panel (#84) when it has anything to show. */
fun PopupMenu.showIfNotEmpty(anchor: View) = if (menu.hasVisibleItems()) {
	showLumaFin(anchor)
} else false
