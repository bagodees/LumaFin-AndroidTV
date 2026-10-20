package org.jellyfin.androidtv.ui.settings.screen.livetv

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.preference.constant.LiveTvChannelOrder
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

@Composable
fun SettingsLiveTvGuideOptionsScreen() {
	val router = LocalRouter.current
	val liveTvPreferences = koinInject<LiveTvPreferences>()

	val indicators = listOf(
		rememberPreference(liveTvPreferences, LiveTvPreferences.showHDIndicator) to stringResource(R.string.lbl_hd_programs),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showLiveIndicator) to stringResource(R.string.lbl_live_broadcasts),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showNewIndicator) to stringResource(R.string.lbl_new_episodes),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showPremiereIndicator) to stringResource(R.string.lbl_premieres),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showRepeatIndicator) to stringResource(R.string.lbl_repeat_episodes),
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.lbl_live_tv_guide).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings)) },
			)
		}

		item {
			var channelOrder by rememberPreference(liveTvPreferences, LiveTvPreferences.channelOrder)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_sort_by)) },
				captionContent = { Text(stringResource(LiveTvChannelOrder.fromString(channelOrder).nameRes)) },
				onClick = { router.push(Routes.LIVETV_GUIDE_CHANNEL_ORDER) },
				modifier = Modifier.focusKey(Routes.LIVETV_GUIDE_CHANNEL_ORDER)
			)
		}

		item {
			var favsAtTop by rememberPreference(liveTvPreferences, LiveTvPreferences.favsAtTop)

			ListButton(

				toggleState = favsAtTop,
				headingContent = { Text(stringResource(R.string.lbl_start_favorites)) },
				trailingContent = { SettingsSwitch(checked = favsAtTop) },
				onClick = { favsAtTop = !favsAtTop },
				modifier = Modifier.focusKey("favs_at_top")
			)
		}

		item {
			var colorCodeGuide by rememberPreference(liveTvPreferences, LiveTvPreferences.colorCodeGuide)

			ListButton(

				toggleState = colorCodeGuide,
				headingContent = { Text(stringResource(R.string.lbl_colored_backgrounds)) },
				trailingContent = { SettingsSwitch(checked = colorCodeGuide) },
				onClick = { colorCodeGuide = !colorCodeGuide },
				modifier = Modifier.focusKey("color_code_guide")
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.lbl_show_indicators)) }) }

		items(indicators) { (preference, label) ->
			var enabled by preference

			ListButton(

				toggleState = enabled,
				headingContent = { Text(label) },
				trailingContent = { SettingsSwitch(checked = enabled) },
				onClick = { enabled = !enabled },
				modifier = Modifier.focusKey("indicator_$label")
			)
		}
	}
}
