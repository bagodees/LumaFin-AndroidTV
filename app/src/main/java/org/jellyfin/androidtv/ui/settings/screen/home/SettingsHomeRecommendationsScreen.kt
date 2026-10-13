package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.RecommendationPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsHomeRecommendationsScreen() {
	val preferences = koinInject<RecommendationPreferences>()
	var hidden by remember { mutableStateOf(preferences.hiddenRows) }
	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.home_prefs).uppercase()) },
				headingContent = { Text(stringResource(R.string.recommendation_settings)) },
				captionContent = { Text(stringResource(R.string.recommendation_restore_hint)) },
			)
		}
		if (hidden.isEmpty()) item {
			ListSection(headingContent = { Text(stringResource(R.string.recommendation_none_hidden)) })
		}
		items(hidden.entries.toList(), key = { it.key }) { (id, name) ->
			ListButton(
				headingContent = { Text(stringResource(R.string.because_you_watched, name)) },
				captionContent = { Text(stringResource(R.string.recommendation_restore)) },
				onClick = { preferences.restore(id); hidden = preferences.hiddenRows },
				modifier = Modifier.focusKey("restore_recommendation_$id"),
			)
		}
	}
}
