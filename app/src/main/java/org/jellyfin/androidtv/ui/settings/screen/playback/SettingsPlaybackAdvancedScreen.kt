package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn

@Composable
fun SettingsPlaybackAdvancedScreen() {
	val router = LocalRouter.current
	SettingsColumn {
		item {
			ListSection(overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) })
		}
		items(advancedPlaybackGroups.size, key = { advancedPlaybackGroups[it].route }) { index ->
			val group = advancedPlaybackGroups[index]
			ListButton(navigates = true, headingContent = { Text(stringResource(group.title)) },
				captionContent = { Text(stringResource(group.description)) },
				onClick = { router.push(group.route) }, modifier = Modifier.focusKey(group.route))
		}
	}
}

private data class AdvancedPlaybackGroup(val route: String, val title: Int, val description: Int)
private val advancedPlaybackGroups = listOf(
	AdvancedPlaybackGroup(Routes.PLAYBACK_SEEKING, R.string.settings_seeking, R.string.settings_seeking_description),
	AdvancedPlaybackGroup(Routes.PLAYBACK_VIDEO, R.string.pref_video, R.string.settings_video_description),
	AdvancedPlaybackGroup(Routes.PLAYBACK_AUDIO, R.string.pref_audio, R.string.settings_audio_description),
	AdvancedPlaybackGroup(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY, R.string.settings_subtitle_compatibility, R.string.settings_subtitle_compatibility_description),
	AdvancedPlaybackGroup(Routes.PLAYBACK_LIVE_TV, R.string.pref_live_tv_cat, R.string.settings_live_direct_description),
	AdvancedPlaybackGroup(Routes.PLAYBACK_TROUBLESHOOTING, R.string.pref_troubleshooting, R.string.settings_troubleshooting_description),
)