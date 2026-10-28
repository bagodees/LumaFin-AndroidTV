package org.jellyfin.androidtv.ui.navigation.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavEntryDecorator
import org.jellyfin.androidtv.ui.navigation.RouteContext

@Composable
fun rememberFocusRestorationNavEntryDecorator(backStack: List<RouteContext> = emptyList()): NavEntryDecorator<RouteContext> {
	val savedFocusKeys = rememberSaveable { mutableMapOf<Any, String?>() }

	return remember {
		NavEntryDecorator(
			onPop = { key -> savedFocusKeys.remove(key) },
			decorate = { entry ->
				val key = entry.contentKey
				val focusPersistManager = remember(key) { FocusPersistManager() }
				// A page sliding out after another opened keeps the row that opened it, whatever focus touches on its way out
				val top = backStack.lastOrNull()
				// The content key pairs the route (as text) with its type
				val route = (key as? Pair<*, *>)?.first ?: key
				focusPersistManager.recording = top == null || route == top || route.toString() == top.toString()

				CompositionLocalProvider(
					LocalFocusPersistManager provides focusPersistManager
				) {
					entry.Content()
				}

				LaunchedEffect(key) {
					focusPersistManager.restore(savedFocusKeys[key])
				}

				DisposableEffect(key) {
					onDispose {
						savedFocusKeys[key] = focusPersistManager.save()
					}
				}
			}
		)
	}
}
