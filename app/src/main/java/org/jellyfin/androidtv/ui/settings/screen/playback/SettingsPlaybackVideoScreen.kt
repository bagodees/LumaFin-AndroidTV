package org.jellyfin.androidtv.ui.settings.screen.playback

import org.jellyfin.androidtv.ui.settings.composable.SettingsValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsControl as ListControl
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import java.text.DecimalFormat
import kotlin.math.roundToLong

@Composable
fun SettingsPlaybackVideoScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_video)) },
				captionContent = { Text(stringResource(R.string.settings_video_description)) },
			)
		}
		item {
			var bufferLength by rememberPreference(userPreferences, UserPreferences.bufferLength)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.playback_buffer_length)) },
				captionContent = { Text(stringResource(R.string.settings_search_buffer)) },
				trailingContent = { SettingsValue(stringResource(bufferLength.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_BUFFER_LENGTH) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_BUFFER_LENGTH)
			)
		}


		item {
			val bitrate by rememberPreference(userPreferences, UserPreferences.maxBitrate)
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.pref_max_bitrate_title)) },
				captionContent = { Text(getQualityProfiles(context)[bitrate].orEmpty()) },
				onClick = { router.push(Routes.PLAYBACK_MAX_BITRATE) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_MAX_BITRATE),
			)
		}

		item {
			var refreshRateSwitchingBehavior by rememberPreference(userPreferences, UserPreferences.refreshRateSwitchingBehavior)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_refresh_switching)) },
				captionContent = { Text(stringResource(R.string.settings_search_refresh)) },
				trailingContent = { SettingsValue(stringResource(refreshRateSwitchingBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR)
			)
		}

		item {
			var videoStartDelay by rememberPreference(userPreferences, UserPreferences.videoStartDelay)
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.video_start_delay)) },
				captionContent = { Text(stringResource(R.string.settings_video_delay_description)) },
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("video_start_delay")
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						// 0 - 5 seconds with 0.25 second increment
						min = 0_000f,
						max = 5_000f,
						stepForward = 250f,
						value = videoStartDelay.toFloat(),
						onValueChange = { videoStartDelay = it.roundToLong() }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 48.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						val formatter = remember { DecimalFormat("0.##") }
						Text("${formatter.format(videoStartDelay / 1000f)}s")
					}
				}
			}
		}

		item {
			var playerZoomMode by rememberPreference(userPreferences, UserPreferences.playerZoomMode)

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.default_video_zoom)) },
				captionContent = { Text(stringResource(R.string.settings_search_zoom)) },
				trailingContent = { SettingsValue(stringResource(playerZoomMode.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_ZOOM_MODE) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_ZOOM_MODE)
			)
		}

		item {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.preference_hdr_overrides)) },
				captionContent = { Text(stringResource(R.string.preference_hdr_overrides_summary)) },
				onClick = { router.push(Routes.PLAYBACK_HDR_OVERRIDES) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_HDR_OVERRIDES)
			)
		}

		item {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.preference_codecs)) },
				captionContent = { Text(stringResource(R.string.preference_codecs_summary)) },
				onClick = { router.push(Routes.PLAYBACK_CODEC) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_CODEC)
			)
		}

	}
}
