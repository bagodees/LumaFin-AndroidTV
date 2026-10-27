package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.collectAsState
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.HomeShortcutPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsHomeShortcutsScreen() {
	val preferences = koinInject<HomeShortcutPreferences>()
	val context = LocalContext.current
	val koinMessages = koinInject<CustomMessageRepository>()
	var shortcuts by remember { mutableStateOf(preferences.shortcuts) }
	// Movies and TV libraries whose favorites can be pinned (#96); others pin from their cards and tiles
	val views by koinInject<UserViewsRepository>().views.collectAsState(emptyList())
	val favoriteLibraries = views.filter { it.collectionType == CollectionType.MOVIES || it.collectionType == CollectionType.TVSHOWS }
	fun favoritesName(library: BaseItemDto) = when {
		favoriteLibraries.count { it.collectionType == library.collectionType } > 1 -> context.getString(R.string.home_shortcut_favorites_in, library.name.orEmpty())
		library.collectionType == CollectionType.MOVIES -> context.getString(R.string.home_shortcut_favorite_movies)
		else -> context.getString(R.string.home_shortcut_favorite_shows)
	}
	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.home_prefs).uppercase()) },
				headingContent = { Text(stringResource(R.string.home_shortcuts)) },
				captionContent = { Text(stringResource(R.string.home_shortcuts_hint)) },
			)
		}
		val addable = favoriteLibraries.filterNot { library -> shortcuts.any { it.id == preferences.favoritesId(library).toString() } }
		if (addable.isNotEmpty()) item { ListSection(headingContent = { Text(stringResource(R.string.home_shortcut_add_section)) }) }
		items(addable, key = { "add_favorites_${it.id}" }) { library ->
			val name = favoritesName(library)
			ListButton(
				headingContent = { Text(stringResource(R.string.home_shortcut_add, name)) },
				captionContent = { Text(stringResource(R.string.home_shortcut_add_favorites_description, library.name.orEmpty())) },
				modifier = Modifier.focusKey("add_favorites_${library.id}"),
				onClick = {
					preferences.pinFavorites(library, name)
					koinMessages.pushMessage(CustomMessage.HomeShortcutsChanged)
					shortcuts = preferences.shortcuts
				},
			)
		}
		if (shortcuts.isEmpty()) item { ListSection(headingContent = { Text(stringResource(R.string.home_shortcuts_empty)) }) }
		itemsIndexed(shortcuts, key = { _, item -> item.id }) { index, shortcut ->
			ListButton(
				headingContent = { Text(shortcut.name) },
				captionContent = { Text(stringResource(R.string.home_shortcut_manage)) },
				modifier = Modifier.focusKey("home_shortcut_${shortcut.id}"),
				onClick = {
					val options = buildList {
						if (index > 0) add(R.string.home_shortcut_move_earlier to -1)
						if (index < shortcuts.lastIndex) add(R.string.home_shortcut_move_later to 1)
						add(R.string.home_shortcut_unpin to 0)
					}
					AlertDialog.Builder(context).setTitle(shortcut.name)
						.setItems(options.map { context.getString(it.first) }.toTypedArray()) { _, selected ->
							val delta = options[selected].second
							if (delta == 0) preferences.unpin(shortcut.id) else preferences.move(shortcut.id, delta)
							shortcuts = preferences.shortcuts
						}.setNegativeButton(R.string.lbl_cancel, null).show()
				},
			)
		}
	}
}
