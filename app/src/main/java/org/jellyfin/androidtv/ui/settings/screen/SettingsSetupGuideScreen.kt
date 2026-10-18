package org.jellyfin.androidtv.ui.settings.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun SettingsSetupGuideScreen(firstRun: Boolean = false) {
	val router = LocalRouter.current
	val settings = koinActivityViewModel<SettingsViewModel>()
	val close = { if (firstRun) settings.hide() else router.back() }
	BackHandler(enabled = firstRun) { settings.hide() }
	SettingsColumn {
		item {
			ListSection(headingContent = { Text(stringResource(R.string.setup_guide_title)) },
				captionContent = { Text(stringResource(R.string.setup_guide_intro)) })
		}
		item {
			ListButton(headingContent = { Text(stringResource(if (firstRun) R.string.setup_guide_skip else R.string.setup_guide_close)) },
				onClick = close, modifier = Modifier.focusKey("setup_skip", initialFocus = true))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_guide_layout)) },
				captionContent = { Text(stringResource(R.string.setup_guide_layout_description)) },
				onClick = { router.push(Routes.HOME) }, modifier = Modifier.focusKey("setup_layout"))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_guide_subtitles)) },
				captionContent = { Text(stringResource(R.string.setup_guide_subtitles_description)) },
				onClick = { router.push(Routes.SETUP_SUBTITLES) }, modifier = Modifier.focusKey("setup_subtitles"))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_guide_autoplay)) },
				captionContent = { Text(stringResource(R.string.setup_guide_autoplay_description)) },
				onClick = { router.push(Routes.PLAYBACK_AUTOPLAY) }, modifier = Modifier.focusKey("setup_autoplay"))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_guide_done)) },
				onClick = close, modifier = Modifier.focusKey("setup_done"))
		}
	}
}

@Composable
fun SettingsSetupSubtitlesScreen() {
	val router = LocalRouter.current
	SettingsColumn {
		item {
			ListSection(headingContent = { Text(stringResource(R.string.pref_subtitles)) },
				captionContent = { Text(stringResource(R.string.setup_subtitle_language)) })
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_subtitle_appearance)) },
				captionContent = { Text(stringResource(R.string.setup_subtitle_appearance_description)) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES) },
				modifier = Modifier.focusKey("setup_subtitle_appearance", initialFocus = true))
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.setup_guide_back)) },
				onClick = { router.back() }, modifier = Modifier.focusKey("setup_subtitle_back"))
		}
	}
}
