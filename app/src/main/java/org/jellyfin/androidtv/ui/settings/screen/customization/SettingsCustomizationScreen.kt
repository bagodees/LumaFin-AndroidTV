package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import org.jellyfin.androidtv.preference.constant.MediaCardSize
import org.jellyfin.androidtv.preference.constant.TextScale
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.androidtv.ui.base.form.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsCustomizationScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var cardSize by rememberPreference(userPreferences, UserPreferences.mediaCardSize)
	var cardSizesExpanded by remember { mutableStateOf(false) }
	// Settings may be shown with a wrapped context; find the hosting activity to reload it.
	val activity = generateSequence(LocalContext.current) { (it as? android.content.ContextWrapper)?.baseContext }
		.firstNotNullOfOrNull { it as? FragmentActivity }
	var textScale by rememberPreference(userPreferences, UserPreferences.textScale)
	var textScalesExpanded by remember { mutableStateOf(false) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings_group_appearance)) },
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_browsing)) }) }

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_grid), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_libraries)) },
				captionContent = { Text(stringResource(R.string.settings_search_libraries)) },
				onClick = { router.push(Routes.LIBRARIES) },
				modifier = Modifier.focusKey(Routes.LIBRARIES)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.home_prefs)) },
				captionContent = { Text(stringResource(R.string.settings_search_home)) },
				onClick = { router.push(Routes.HOME) },
				modifier = Modifier.focusKey(Routes.HOME)
			)
		}
		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_photos), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_screensaver)) },
				captionContent = { Text(stringResource(R.string.settings_search_screensaver)) },
				onClick = { router.push(Routes.CUSTOMIZATION_SCREENSAVER) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SCREENSAVER),
			)
		}
		item {
			var longPress by rememberPreference(userPreferences, UserPreferences.cardLongPressAction)
			ListButton(headingContent = { Text(stringResource(R.string.remote_shortcuts)) },
				captionContent = { Text(stringResource(longPress.nameRes)) },
				onClick = { router.push(Routes.REMOTE_SHORTCUTS) }, modifier = Modifier.focusKey(Routes.REMOTE_SHORTCUTS))
		}
		item { ListSection(headingContent = { Text(stringResource(R.string.settings_appearance_display)) }) }
		item {
			var appTheme by rememberPreference(userPreferences, UserPreferences.appTheme)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_app_theme)) },
				captionContent = { Text(stringResource(appTheme.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_THEME) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_THEME)
			)
		}

		item {
			var clockBehavior by rememberPreference(userPreferences, UserPreferences.clockBehavior)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_clock_display)) },
				captionContent = { Text(stringResource(clockBehavior.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_CLOCK) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_CLOCK)
			)
		}

		item {
			var watchedIndicatorBehavior by rememberPreference(userPreferences, UserPreferences.watchedIndicatorBehavior)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_watched_indicator)) },
				captionContent = { Text(stringResource(watchedIndicatorBehavior.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_WATCHED_INDICATOR) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_WATCHED_INDICATOR)
			)
		}

		item {
			var backdropBehavior by rememberPreference(userPreferences, UserPreferences.backdropBehavior)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_show_backdrop)) },
				captionContent = { Text(stringResource(backdropBehavior.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_BACKDROP) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_BACKDROP)
			)
		}

		item {
			var seriesThumbnailsEnabled by rememberPreference(userPreferences, UserPreferences.seriesThumbnailsEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_use_series_thumbnails)) },
				trailingContent = { Checkbox(checked = seriesThumbnailsEnabled) },
				captionContent = { Text(stringResource(R.string.lbl_use_series_thumbnails_description)) },
				onClick = { seriesThumbnailsEnabled = !seriesThumbnailsEnabled },
				modifier = Modifier.focusKey("series_thumbnails_enabled")
			)
		}

		item {
			var ratingBadgesEnabled by rememberPreference(userPreferences, UserPreferences.ratingBadgesEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_rating_badges)) },
				captionContent = { Text(stringResource(R.string.pref_rating_badges_description)) },
				trailingContent = { Checkbox(checked = ratingBadgesEnabled) },
				onClick = { ratingBadgesEnabled = !ratingBadgesEnabled },
				modifier = Modifier.focusKey("rating_badges_enabled")
			)
		}

		item {
			var hideDescriptions by rememberPreference(userPreferences, UserPreferences.hideUnwatchedEpisodeDescriptions)
			ListButton(
				headingContent = { Text(stringResource(R.string.hide_unwatched_episode_descriptions)) },
				captionContent = { Text(stringResource(R.string.hide_unwatched_episode_descriptions_description)) },
				trailingContent = { Checkbox(checked = hideDescriptions) },
				onClick = { hideDescriptions = !hideDescriptions },
				modifier = Modifier.focusKey("hide_unwatched_episode_descriptions"),
			)
		}

		item {
			var reducedMotion by rememberPreference(userPreferences, UserPreferences.reducedMotion)
			ListButton(
				headingContent = { Text(stringResource(R.string.reduced_motion)) },
				captionContent = { Text(stringResource(R.string.reduced_motion_description)) },
				trailingContent = { Checkbox(checked = reducedMotion) },
				onClick = { reducedMotion = !reducedMotion },
				modifier = Modifier.focusKey("reduced_motion"),
			)
		}

		item {
			var hideArtwork by rememberPreference(userPreferences, UserPreferences.hideUnwatchedEpisodeArtwork)
			ListButton(
				headingContent = { Text(stringResource(R.string.hide_unwatched_episode_artwork)) },
				captionContent = { Text(stringResource(R.string.hide_unwatched_episode_artwork_description)) },
				trailingContent = { Checkbox(checked = hideArtwork) },
				onClick = { hideArtwork = !hideArtwork },
				modifier = Modifier.focusKey("hide_unwatched_episode_artwork"),
			)
		}

		item(key = "media_card_size") {
			ListButton(
				headingContent = { Text(stringResource(R.string.media_card_size)) },
				captionContent = { Text(stringResource(cardSize.nameRes)) },
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

		item(key = "accessibility") {
			ListSection(headingContent = { Text(stringResource(R.string.accessibility_title)) })
		}
		item(key = "text_scale") {
			ListButton(
				headingContent = { Text(stringResource(R.string.text_scale)) },
				captionContent = { Text("${stringResource(textScale.nameRes)} · ${stringResource(R.string.text_scale_description)}") },
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
		item(key = "high_contrast") {
			var highContrast by rememberPreference(userPreferences, UserPreferences.highContrast)
			ListButton(
				headingContent = { Text(stringResource(R.string.high_contrast)) },
				captionContent = { Text(stringResource(R.string.high_contrast_description)) },
				trailingContent = { Checkbox(checked = highContrast) },
				onClick = { highContrast = !highContrast },
				modifier = Modifier.focusKey("high_contrast"),
			)
		}
		item(key = "strong_focus") {
			var strongFocus by rememberPreference(userPreferences, UserPreferences.strongFocus)
			ListButton(
				headingContent = { Text(stringResource(R.string.strong_focus)) },
				captionContent = { Text(stringResource(R.string.strong_focus_description)) },
				trailingContent = { Checkbox(checked = strongFocus) },
				onClick = { strongFocus = !strongFocus },
				modifier = Modifier.focusKey("strong_focus"),
			)
		}

	}
}
