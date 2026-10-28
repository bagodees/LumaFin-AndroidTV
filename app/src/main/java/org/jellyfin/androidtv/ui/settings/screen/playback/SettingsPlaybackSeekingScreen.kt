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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
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
import kotlin.math.roundToInt

@Composable
fun SettingsPlaybackSeekingScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var skipBackLength by rememberPreference(userSettingPreferences, UserSettingPreferences.skipBackLength)
	var skipForwardLength by rememberPreference(userSettingPreferences, UserSettingPreferences.skipForwardLength)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings_seeking)) },
				captionContent = { Text(stringResource(R.string.settings_seeking_description)) },
			)
		}

		item {
			var resumeSubtractDuration by rememberPreference(userPreferences, UserPreferences.resumeSubtractDuration)
			val options = getResumeSubtractDurationOptions()

			ListButton(

				navigates = true,
				headingContent = { Text(stringResource(R.string.settings_resume_rewind)) },
				trailingContent = { SettingsValue(options[resumeSubtractDuration].orEmpty()) },
				onClick = { router.push(Routes.PLAYBACK_RESUME_SUBTRACT_DURATION) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_RESUME_SUBTRACT_DURATION)
			)
		}

		item {
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.skip_back_length)) },
				captionContent = { Text(stringResource(R.string.skip_length_description)) },
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("skip_back_length")
			) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					RangeControl(
						modifier = Modifier.height(4.dp).weight(1f),
						interactionSource = interactionSource,
						min = 5_000f,
						max = 30_000f,
						stepForward = 5_000f,
						value = skipBackLength.toFloat(),
						onValueChange = { skipBackLength = it.roundToInt() }
					)
					Spacer(Modifier.width(Tokens.Space.spaceSm))
					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${skipBackLength / 1000}s")
					}
				}
			}
		}

		item {
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.skip_forward_length)) },
				captionContent = { Text(stringResource(R.string.skip_length_description)) },
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("skip_forward_length")
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						// 5 - 30 seconds with 5 second increment
						min = 5_000f,
						max = 30_000f,
						stepForward = 5_000f,
						value = skipForwardLength.toFloat(),
						onValueChange = { skipForwardLength = it.roundToInt() }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${skipForwardLength / 1000}s")
					}
				}
			}
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.reset_skip_lengths)) },
				captionContent = { Text(stringResource(R.string.reset_skip_lengths_description)) },
				onClick = {
					skipBackLength = UserSettingPreferences.skipBackLength.defaultValue
					skipForwardLength = UserSettingPreferences.skipForwardLength.defaultValue
				},
				modifier = Modifier.focusKey("reset_skip_lengths")
			)
		}

	}
}
