package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.CardShape
import org.jellyfin.androidtv.preference.constant.MediaCardSize
import org.jellyfin.androidtv.preference.constant.TextScale
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.settings.composable.SettingsValue
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.preference.Preference
import org.koin.compose.koinInject
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection

/* Appearance is split into small pages (#125); every setting keeps its preference and value. */

@Composable
private fun GroupHeading(title: Int) = ListSection(
	overlineContent = { Text(stringResource(R.string.settings_group_appearance).uppercase()) },
	headingContent = { Text(stringResource(title)) },
)

@Composable
private fun ToggleRow(preference: Preference<Boolean>, title: Int, description: Int) {
	val userPreferences = koinInject<UserPreferences>()
	var enabled by rememberPreference(userPreferences, preference)
	ListButton(
		toggleState = enabled,
		headingContent = { Text(stringResource(title)) },
		captionContent = { Text(stringResource(description)) },
		trailingContent = { SettingsSwitch(checked = enabled) },
		onClick = { enabled = !enabled },
		modifier = Modifier.focusKey(preference.key),
	)
}

@Composable
private fun PageRow(route: String, title: Int, description: Int, value: String) {
	val router = LocalRouter.current
	ListButton(
		navigates = true,
		headingContent = { Text(stringResource(title)) },
		captionContent = { Text(stringResource(description)) },
		trailingContent = { SettingsValue(value) },
		onClick = { router.push(route) },
		modifier = Modifier.focusKey(route),
	)
}

@Composable
fun SettingsAppearanceStyleScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val appTheme by rememberPreference(userPreferences, UserPreferences.appTheme)
	val clock by rememberPreference(userPreferences, UserPreferences.clockBehavior)
	val watched by rememberPreference(userPreferences, UserPreferences.watchedIndicatorBehavior)
	val backdrop by rememberPreference(userPreferences, UserPreferences.backdropBehavior)
	SettingsColumn {
		item { GroupHeading(R.string.appearance_style) }
		item { PageRow(Routes.CUSTOMIZATION_THEME, R.string.pref_app_theme, R.string.pref_app_theme_description, stringResource(appTheme.nameRes)) }
		item { PageRow(Routes.CUSTOMIZATION_CLOCK, R.string.pref_clock_display, R.string.pref_clock_display_description, stringResource(clock.nameRes)) }
		item { PageRow(Routes.CUSTOMIZATION_WATCHED_INDICATOR, R.string.pref_watched_indicator, R.string.pref_watched_indicator_description, stringResource(watched.nameRes)) }
		item { PageRow(Routes.CUSTOMIZATION_BACKDROP, R.string.lbl_show_backdrop, R.string.lbl_show_backdrop_description, stringResource(backdrop.nameRes)) }
		item { ToggleRow(UserPreferences.reducedMotion, R.string.reduced_motion, R.string.reduced_motion_description) }
	}
}

@Composable
fun SettingsAppearanceCardsScreen() {
	val userPreferences = koinInject<UserPreferences>()
	var cardSize by rememberPreference(userPreferences, UserPreferences.mediaCardSize)
	var cardSizesExpanded by remember { mutableStateOf(false) }
	SettingsColumn {
		item { GroupHeading(R.string.appearance_cards) }
		// Card style (#123): OK switches between Landscape and Portrait
		listOf(
			Triple(UserPreferences.continueWatchingCardShape, R.string.card_shape_continue_watching, R.string.card_shape_continue_watching_description),
			Triple(UserPreferences.homeRowsCardShape, R.string.card_shape_home_rows, R.string.card_shape_home_rows_description),
			Triple(UserPreferences.libraryCardShape, R.string.card_shape_libraries, R.string.card_shape_libraries_description),
		).forEach { (preference, title, description) ->
			item(key = preference.key) {
				var shape by rememberPreference(userPreferences, preference)
				ListButton(
					headingContent = { Text(stringResource(title)) },
					captionContent = { Text(stringResource(description)) },
					trailingContent = { SettingsValue(stringResource(shape.nameRes)) },
					onClick = { shape = if (shape == CardShape.PORTRAIT) CardShape.LANDSCAPE else CardShape.PORTRAIT },
					modifier = Modifier.focusKey(preference.key),
				)
			}
		}
		item(key = "media_card_size") {
			ListButton(
				headingContent = { Text(stringResource(R.string.media_card_size)) },
				captionContent = { Text(stringResource(R.string.media_card_size_description)) },
				trailingContent = { SettingsValue(stringResource(cardSize.nameRes)) },
				onClick = { cardSizesExpanded = !cardSizesExpanded },
				modifier = Modifier.focusKey("media_card_size"),
			)
		}
		if (cardSizesExpanded) {
			items(MediaCardSize.entries.size, key = { "media_card_size_$it" }) { index ->
				val option = MediaCardSize.entries[index]
				ListButton(
					headingContent = { Text(stringResource(option.nameRes)) },
					trailingContent = { RadioButton(checked = cardSize == option) },
					onClick = { cardSize = option },
					modifier = Modifier.focusKey("media_card_size_${option.name}"),
				)
			}
		}
		item(key = "series_thumbnails") { ToggleRow(UserPreferences.seriesThumbnailsEnabled, R.string.lbl_use_series_thumbnails, R.string.lbl_use_series_thumbnails_description) }
		item(key = "rating_badges") { ToggleRow(UserPreferences.ratingBadgesEnabled, R.string.pref_rating_badges, R.string.pref_rating_badges_description) }
	}
}

@Composable
fun SettingsAppearanceSpoilersScreen() {
	SettingsColumn {
		item { GroupHeading(R.string.appearance_spoilers) }
		item { ToggleRow(UserPreferences.hideUnwatchedEpisodeDescriptions, R.string.hide_unwatched_episode_descriptions, R.string.hide_unwatched_episode_descriptions_description) }
		item { ToggleRow(UserPreferences.hideUnwatchedEpisodeArtwork, R.string.hide_unwatched_episode_artwork, R.string.hide_unwatched_episode_artwork_description) }
	}
}

@Composable
fun SettingsAppearanceAccessibilityScreen() {
	val userPreferences = koinInject<UserPreferences>()
	// Settings may be shown with a wrapped context; find the hosting activity to reload it.
	val activity = generateSequence(LocalContext.current) { (it as? android.content.ContextWrapper)?.baseContext }
		.firstNotNullOfOrNull { it as? FragmentActivity }
	var textScale by rememberPreference(userPreferences, UserPreferences.textScale)
	var textScalesExpanded by remember { mutableStateOf(false) }
	SettingsColumn {
		item { GroupHeading(R.string.accessibility_title) }
		item(key = "text_scale") {
			ListButton(
				headingContent = { Text(stringResource(R.string.text_scale)) },
				captionContent = { Text(stringResource(R.string.text_scale_description)) },
				trailingContent = { SettingsValue(stringResource(textScale.nameRes)) },
				onClick = { textScalesExpanded = !textScalesExpanded },
				modifier = Modifier.focusKey("text_scale"),
			)
		}
		if (textScalesExpanded) {
			items(TextScale.entries.size, key = { "text_scale_$it" }) { index ->
				val option = TextScale.entries[index]
				ListButton(
					headingContent = { Text(stringResource(option.nameRes)) },
					trailingContent = { RadioButton(checked = textScale == option) },
					onClick = {
						if (textScale != option) {
							// Store synchronously: text size is applied when the screen is created, so reload it now.
							userPreferences[UserPreferences.textScale] = option
							textScale = option
							activity?.applyTheme()
						}
					},
					modifier = Modifier.focusKey("text_scale_${option.name}"),
				)
			}
		}
		item(key = "high_contrast") { ToggleRow(UserPreferences.highContrast, R.string.high_contrast, R.string.high_contrast_description) }
		item(key = "strong_focus") { ToggleRow(UserPreferences.strongFocus, R.string.strong_focus, R.string.strong_focus_description) }
	}
}
