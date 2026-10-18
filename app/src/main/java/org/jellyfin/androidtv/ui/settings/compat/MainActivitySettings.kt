package org.jellyfin.androidtv.ui.settings.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.koinInject

@Composable
fun MainActivitySettings() {
	val viewModel = koinActivityViewModel<SettingsViewModel>()
	val visible by viewModel.visible.collectAsState()
	val preferences = koinInject<UserPreferences>()
	LaunchedEffect(preferences.accountRevision) {
		if (!preferences[UserPreferences.setupGuideSeen]) {
			// Record presentation, so Back or an interrupted visit never forces setup again.
			preferences[UserPreferences.setupGuideSeen] = true
			viewModel.showSetupGuide()
		}
	}

	ProvideRouter(routes, Routes.MAIN) {
		val router = LocalRouter.current
		LaunchedEffect(visible) {
			if (visible) {
				router.backStack.clear()
				router.push(Routes.MAIN)
				if (viewModel.openSetupGuide) router.push(Routes.SETUP_GUIDE, mapOf("firstRun" to "true"))
			}
		}
		SettingsDialog(
			visible = visible,
			onDismissRequest = { viewModel.hide() }
		) {
			SettingsRouterContent()
		}
	}
}
