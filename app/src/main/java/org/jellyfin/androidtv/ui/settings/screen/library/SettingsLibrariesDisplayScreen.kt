package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun SettingsLibrariesDisplayScreen(itemId: UUID, displayPreferencesId: String) {
	val router = LocalRouter.current
	val userViewsRepository = koinInject<UserViewsRepository>()
	val preferencesRepository = koinInject<PreferencesRepository>()
	val userView = rememberUserView(itemId)
	val libraryPreferences = remember(displayPreferencesId) { preferencesRepository.getLibraryPreferences(displayPreferencesId) }

	val allowViewSelection = userViewsRepository.allowViewSelection(userView?.collectionType)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_libraries).uppercase()) },
				headingContent = { Text(userView?.name.orEmpty()) },
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.settings_library_layout)) }) }

		item {
			var posterSize by rememberPreference(libraryPreferences, LibraryPreferences.posterSize)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_image_size)) },
				captionContent = { Text(stringResource(posterSize.nameRes)) },
				onClick = {
					router.push(
						Routes.LIBRARIES_DISPLAY_IMAGE_SIZE,
						mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
					)
				},
				modifier = Modifier.focusKey(Routes.LIBRARIES_DISPLAY_IMAGE_SIZE)
			)
		}

		item {
			var imageType by rememberPreference(libraryPreferences, LibraryPreferences.imageType)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_image_type)) },
				captionContent = { Text(stringResource(imageType.nameRes)) },
				onClick = {
					router.push(
						Routes.LIBRARIES_DISPLAY_IMAGE_TYPE,
						mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
					)
				},
				modifier = Modifier.focusKey(Routes.LIBRARIES_DISPLAY_IMAGE_TYPE)
			)
		}

		item {
			var gridDirection by rememberPreference(libraryPreferences, LibraryPreferences.gridDirection)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.grid_direction)) },
				captionContent = { Text(stringResource(gridDirection.nameRes)) },
				onClick = {
					router.push(
						Routes.LIBRARIES_DISPLAY_GRID,
						mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
					)
				},
				modifier = Modifier.focusKey(Routes.LIBRARIES_DISPLAY_GRID)
			)
		}

		if (allowViewSelection || org.jellyfin.androidtv.ui.browsing.supportsCompactList(userView?.collectionType)) item {
			ListSection(headingContent = { Text(stringResource(R.string.settings_library_browsing)) })
		}

		if (org.jellyfin.androidtv.ui.browsing.supportsCompactList(userView?.collectionType)) item {
			var compactList by rememberPreference(libraryPreferences, LibraryPreferences.compactList)
			ListButton(
				toggleState = compactList,
				headingContent = { Text(stringResource(R.string.library_compact_list)) },
				captionContent = { Text(stringResource(R.string.settings_compact_list_summary)) },
				trailingContent = { SettingsSwitch(checked = compactList) },
				onClick = { compactList = !compactList },
				modifier = Modifier.focusKey("compact_list"),
			)
		}

		if (allowViewSelection) item {
			var enableSmartScreen by rememberPreference(libraryPreferences, LibraryPreferences.enableSmartScreen)

			ListButton(

				toggleState = enableSmartScreen,
				headingContent = { Text(stringResource(R.string.enable_smart_view)) },
				trailingContent = { SettingsSwitch(checked = enableSmartScreen) },
				captionContent = { Text(stringResource(R.string.enable_smart_view_description)) },
				onClick = { enableSmartScreen = !enableSmartScreen },
				modifier = Modifier.focusKey("enable_smart_view")
			)
		}
	}
}
