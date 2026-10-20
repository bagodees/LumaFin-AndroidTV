package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.ColorSwatch
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsControl as ListControl
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable.SubtitleStylePreview
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun SettingsSubtitlesScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	var subtitleBrightness by rememberPreference(userPreferences, UserPreferences.subtitleBrightness)
	var subtitlesBackgroundColor by rememberPreference(userPreferences, UserPreferences.subtitlesBackgroundColor)
	var subtitlesTextWeight by rememberPreference(userPreferences, UserPreferences.subtitlesTextWeight)
	var subtitlesTextColor by rememberPreference(userPreferences, UserPreferences.subtitlesTextColor)
	var subtitleTextStrokeColor by rememberPreference(userPreferences, UserPreferences.subtitleTextStrokeColor)
	var subtitlesTextSize by rememberPreference(userPreferences, UserPreferences.subtitlesTextSize)
	var subtitlesOffsetPosition by rememberPreference(userPreferences, UserPreferences.subtitlesOffsetPosition)


    Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
        ListSection(
            overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
            headingContent = { Text(stringResource(R.string.pref_subtitles)) },
            captionContent = { Text(stringResource(R.string.settings_subtitle_appearance_scope)) },
        )
        SubtitleStylePreview(
            userPreferences = userPreferences,
            subtitlesBackgroundColor = subtitlesBackgroundColor,
            subtitlesTextWeight = subtitlesTextWeight,
            subtitlesTextColor = subtitlesTextColor,
            subtitleTextStrokeColor = subtitleTextStrokeColor,
            subtitlesTextSize = subtitlesTextSize,
            subtitleBrightness = subtitleBrightness,
        )
        SettingsColumn(modifier = Modifier.weight(1f)) {
        item {
            val interactionSource = remember { MutableInteractionSource() }
            ListControl(
                headingContent = { Text(stringResource(R.string.subtitle_brightness)) },
                captionContent = { Text(stringResource(R.string.subtitle_brightness_description)) },
                interactionSource = interactionSource,
                modifier = Modifier.focusKey("subtitle_brightness"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RangeControl(
                        modifier = Modifier.height(4.dp).weight(1f),
                        interactionSource = interactionSource,
                        min = 25f, max = 100f, stepForward = 5f,
                        value = subtitleBrightness.coerceIn(25, 100).toFloat(),
                        onValueChange = { subtitleBrightness = it.roundToInt() },
                    )
                    Spacer(Modifier.width(Tokens.Space.spaceSm))
                    Text("${subtitleBrightness.coerceIn(25, 100)}%")
                }
            }
        }
        item {
            ListButton(
                headingContent = { Text(stringResource(R.string.subtitle_brightness_reset)) },
                captionContent = { Text(stringResource(R.string.settings_brightness_reset_scope)) },
                onClick = { subtitleBrightness = 100 },
                modifier = Modifier.focusKey("subtitle_brightness_reset"),
            )
        }

		item {
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.pref_subtitles_size)) },
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("subtitles_size")
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						min = 8f,
						max = 32f,
						stepForward = 1f,
						value = subtitlesTextSize,
						onValueChange = { subtitlesTextSize = it }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text(subtitlesTextSize.roundToInt().toString())
					}
				}
			}
		}

		item {
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.pref_subtitles_weight)) },
				captionContent = {
					val normalizedValue = subtitlesTextWeight
						.coerceIn(100, 900)
						.floorDiv(100)
						.times(100)

					val name = when (normalizedValue) {
						100 -> stringResource(R.string.font_weight_100)
						200 -> stringResource(R.string.font_weight_200)
						300 -> stringResource(R.string.font_weight_300)
						400 -> stringResource(R.string.font_weight_400)
						500 -> stringResource(R.string.font_weight_500)
						600 -> stringResource(R.string.font_weight_600)
						700 -> stringResource(R.string.font_weight_700)
						800 -> stringResource(R.string.font_weight_800)
						900 -> stringResource(R.string.font_weight_900)
						else -> normalizedValue.toString()
					}
					Text(name)
				},
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("subtitles_weight")
			) {
				RangeControl(
					modifier = Modifier
						.fillMaxWidth()
						.height(4.dp),
					interactionSource = interactionSource,
					min = 100f,
					max = 900f,
					stepForward = 100f,
					value = subtitlesTextWeight.toFloat(),
					onValueChange = { subtitlesTextWeight = it.toInt() }
				)
			}
		}

		item {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_subtitle_text_color)) },
				trailingContent = { ColorSwatch(color = Color(subtitlesTextColor.toInt())) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR)
			)
		}

		item {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_subtitle_background_color)) },
				trailingContent = { ColorSwatch(color = Color(subtitlesBackgroundColor.toInt())) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR)
			)
		}

		item {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.lbl_subtitle_text_stroke_color)) },
				trailingContent = { ColorSwatch(color = Color(subtitleTextStrokeColor.toInt())) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR)
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.settings_subtitle_reset_appearance)) },
				captionContent = { Text(stringResource(R.string.settings_subtitle_reset_appearance_scope)) },
				onClick = {
					subtitleBrightness = UserPreferences.subtitleBrightness.defaultValue
					subtitlesTextSize = UserPreferences.subtitlesTextSize.defaultValue
					subtitlesTextWeight = UserPreferences.subtitlesTextWeight.defaultValue
					subtitlesTextColor = UserPreferences.subtitlesTextColor.defaultValue
					subtitlesBackgroundColor = UserPreferences.subtitlesBackgroundColor.defaultValue
					subtitleTextStrokeColor = UserPreferences.subtitleTextStrokeColor.defaultValue
				}, modifier = Modifier.focusKey("reset_subtitle_appearance"),
			)
		}
		item { ListSection(headingContent = { Text(stringResource(R.string.settings_subtitle_position)) }) }
		item {
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.pref_subtitles_position)) },
				captionContent = { Text(stringResource(R.string.settings_subtitle_position_description)) },
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("subtitles_position")
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						min = 0f,
						max = 0.8f,
						stepForward = 0.01f,
						value = subtitlesOffsetPosition,
						onValueChange = { subtitlesOffsetPosition = it }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${(subtitlesOffsetPosition * 100f).roundToInt()}%")
					}
				}
			}
		}
		item {
			ListButton(headingContent = { Text(stringResource(R.string.settings_subtitle_reset_position)) },
				captionContent = { Text(stringResource(R.string.settings_subtitle_reset_position_scope)) },
				onClick = { subtitlesOffsetPosition = UserPreferences.subtitlesOffsetPosition.defaultValue },
				modifier = Modifier.focusKey("reset_subtitle_position"))
		}
		item {
			ListSection(headingContent = { Text(stringResource(R.string.settings_subtitle_timing_help)) },
				captionContent = { Text(stringResource(R.string.settings_subtitle_timing_help_description)) })
		}
		item {
			ListButton(navigates = true, headingContent = { Text(stringResource(R.string.settings_subtitle_compatibility)) },
				captionContent = { Text(stringResource(R.string.settings_subtitle_compatibility_link)) },
				onClick = { router.push(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY))
		}
	}
    }
}
