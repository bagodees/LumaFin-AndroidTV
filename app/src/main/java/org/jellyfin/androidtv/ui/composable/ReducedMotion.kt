package org.jellyfin.androidtv.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.jellyfin.androidtv.preference.UserPreferences
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

@Composable
fun rememberReducedMotion(): Boolean {
	val preferences = koinInject<UserPreferences>()
	val reducedMotion by preferences.reducedMotionChanges.collectAsState(initial = preferences[UserPreferences.reducedMotion])
	return reducedMotion
}
