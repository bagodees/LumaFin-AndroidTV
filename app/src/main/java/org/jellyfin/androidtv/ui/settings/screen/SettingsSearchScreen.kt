package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import org.jellyfin.androidtv.ui.settings.composable.LocalInSettingsWorkspace
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsSearchField
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.search.searchSettings
import org.jellyfin.androidtv.ui.settings.search.settingsSearchCatalog
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
fun SettingsSearchScreen(mainMenu: (LazyListScope.() -> Unit)? = null) {
	val context = LocalContext.current
	val owner = LocalLifecycleOwner.current
	val router = LocalRouter.current
	val prefs = koinInject<UserPreferences>()
	val userSettings = koinInject<UserSettingPreferences>()
	val segments = koinInject<MediaSegmentRepository>()
	val users = koinInject<UserRepository>()
	val keyboard = LocalSoftwareKeyboardController.current
	var query by rememberSaveable { mutableStateOf("") }
	fun catalog() = settingsSearchCatalog(context, prefs, users.currentUser.value?.policy?.enableLiveTvAccess == true, userSettings, segments)
	var entries by remember(context) { mutableStateOf(catalog()) }
	LaunchedEffect(router.backStack.size) { entries = catalog() }
	DisposableEffect(owner, prefs, context) {
		val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) entries = catalog() }
		owner.lifecycle.addObserver(observer)
		onDispose { owner.lifecycle.removeObserver(observer) }
	}
	val results = remember(entries, query) { searchSettings(entries, query) }
	val firstResult = remember { FocusRequester() }
	val state = rememberLazyListState()
	val scope = rememberCoroutineScope()
	val label = stringResource(R.string.settings_search_title)
	// In full-screen settings, moving onto the Search category must not pull focus out of the menu
	val inWorkspace = LocalInSettingsWorkspace.current
	SettingsColumn(state = state) {
		item(key = "heading") {
			ListSection(overlineContent = { Text(stringResource(if (mainMenu == null) R.string.settings else R.string.app_name).uppercase()) },
				headingContent = { Text(if (mainMenu == null) label else stringResource(R.string.settings)) },
				captionContent = { Text(stringResource(if (mainMenu == null) R.string.settings_search_hint else R.string.settings_description)) })
		}
		item(key = "input") {
			SettingsSearchField(query = query, label = label, onQueryChange = { query = it; scope.launch { state.scrollToItem(0) } }, onSubmit = {
				keyboard?.hide()
				if (results.isNotEmpty() && (query.isNotBlank() || mainMenu == null)) scope.launch {
					state.scrollToItem(if (query.isNotEmpty()) 3 else 2)
					withFrameNanos { }
					firstResult.requestFocus()
				}
			}, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
				.focusKey("settings_search_input", initialFocus = mainMenu == null && !inWorkspace))
		}
		if (query.isNotEmpty()) item(key = "clear") {
			ListButton(headingContent = { Text(stringResource(R.string.settings_search_clear)) },
				onClick = { query = ""; keyboard?.hide() }, modifier = Modifier.focusKey("settings_search_clear"))
		}
		if (query.isBlank() && mainMenu != null) mainMenu() else {
		if (results.isEmpty()) item(key = "empty") {
			ListSection(headingContent = { Text(stringResource(R.string.settings_search_empty)) })
		}
		itemsIndexed(results, key = { _, entry -> entry.id }) { index, entry ->
			ListButton(navigates = true, headingContent = { Text(entry.title) }, overlineContent = { Text(entry.section) },
				captionContent = { Text(listOfNotNull(entry.description, entry.currentValue?.let {
					context.getString(R.string.settings_search_current, it)
				}).joinToString("\n")) },
				onClick = { keyboard?.hide(); router.push(entry.route, parameters = entry.parameters) },
				modifier = (if (index == 0) Modifier.focusRequester(firstResult) else Modifier).focusKey("search_${entry.id}"))
		}
		}
	}
}
