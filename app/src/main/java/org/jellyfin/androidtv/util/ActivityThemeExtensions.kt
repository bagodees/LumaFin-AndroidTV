package org.jellyfin.androidtv.util

import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.TextScale
import android.content.Context
import android.content.res.Configuration
import org.koin.core.context.GlobalContext
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Getter to get the style resource for a given theme.
 */
private val AppTheme.style
	get() = when (this) {
		AppTheme.DARK -> R.style.Theme_Jellyfin
		AppTheme.EMERALD -> R.style.Theme_Jellyfin_Emerald
		AppTheme.MUTED_PURPLE -> R.style.Theme_Jellyfin_MutedPurple
	}

/**
 * Private view model for the [applyTheme] extension to store the currently set theme.
 */
class ThemeViewModel : ViewModel() {
	var theme: AppTheme? = null
	var textScale: TextScale? = null
}

/**
 * Extension function to set the theme. Should be called in [FragmentActivity.onCreate] and
 * [FragmentActivity.onResume]. It recreates the activity when the theme changed after it was set.
 * Do not call during resume if the activity may not be recreated (like in the video player).
 */
fun FragmentActivity.applyTheme() {
	val viewModel by viewModels<ThemeViewModel>()
	val userPreferences by inject<UserPreferences>()
	val theme = userPreferences[UserPreferences.appTheme]
	val textScale = userPreferences[UserPreferences.textScale]

	// Text size is applied in attachBaseContext, so a change needs a fresh activity.
	if (viewModel.textScale != null && viewModel.textScale != textScale) {
		Timber.i("Recreating activity to apply text size")
		viewModel.theme = null
		viewModel.textScale = null
		recreate()
		return
	}
	viewModel.textScale = textScale

	if (viewModel.theme != theme) {
		if (viewModel.theme != null) {
			Timber.i("Recreating activity to apply theme")
			viewModel.theme = null
			recreate()
		} else {
			Timber.i("Applying theme $theme")
			viewModel.theme = theme
			setTheme(theme.style)
		}
	}
}

/**
 * Applies the app text size (#23) on top of the TV's own font scale. Call from attachBaseContext after super;
 * it affects both the XML views and Compose, since Compose reads the configuration's font scale.
 */
fun FragmentActivity.applyTextScale(base: Context) {
	val factor = runCatching { GlobalContext.get().get<UserPreferences>()[UserPreferences.textScale].factor }.getOrDefault(1f)
	if (factor == 1f) return
	applyOverrideConfiguration(Configuration().apply { fontScale = base.resources.configuration.fontScale * factor })
}
