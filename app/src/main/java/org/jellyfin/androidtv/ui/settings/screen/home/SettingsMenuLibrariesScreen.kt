package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.ui.home.SideMenuLibraries
import org.jellyfin.androidtv.ui.home.rememberAllMenuLibraries
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection

/**
 * Which libraries the side menu and top bar show, and in what order (#113). Hidden libraries stay
 * reachable under Libraries.
 */
@Composable
fun SettingsMenuLibrariesScreen() {
	val context = LocalContext.current
	val owner = LocalLifecycleOwner.current
	val all = rememberAllMenuLibraries() ?: return
	var order by remember(all) { mutableStateOf(SideMenuLibraries.ordered(all)) }
	var hidden by remember(all) { mutableStateOf(SideMenuLibraries.hidden()) }
	fun save() = SideMenuLibraries.save(order, hidden)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.home_prefs).uppercase()) },
				headingContent = { Text(stringResource(R.string.menu_libraries)) },
				captionContent = { Text(stringResource(R.string.menu_libraries_hint)) },
			)
		}
		itemsIndexed(order, key = { _, library -> library.id }) { index, library ->
			val id = library.id.toString()
			val shown = id !in hidden
			ListButton(
				headingContent = { Text(library.name.orEmpty()) },
				trailingContent = {
					Text(stringResource(if (shown) R.string.menu_library_shown else R.string.menu_library_hidden),
						color = if (shown) LumaFinPalette.Cyan else LumaFinPalette.SecondaryText)
				},
				modifier = Modifier.focusKey("menu_library_$id"),
				onClick = {
					LumaFinMenuDialog(context, owner, library.name.orEmpty(), fitContent = true).apply {
						row(context.getString(if (shown) R.string.menu_library_hide else R.string.menu_library_show), navigates = false) {
							dismiss()
							hidden = if (shown) hidden + id else hidden - id
							save()
						}
						if (index > 0) row(context.getString(R.string.home_shortcut_move_earlier), navigates = false) {
							dismiss()
							order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
							save()
						}
						if (index < order.lastIndex) row(context.getString(R.string.home_shortcut_move_later), navigates = false) {
							dismiss()
							order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
							save()
						}
						show()
					}
				},
			)
		}
	}
}
