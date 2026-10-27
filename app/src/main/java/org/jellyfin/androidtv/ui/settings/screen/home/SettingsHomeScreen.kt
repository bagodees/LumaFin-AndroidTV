package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.constant.HomeLayoutPreset
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSwitch
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.base.list.ListControlDefaults
import org.jellyfin.androidtv.ui.settings.composable.SettingsControl as ListControl
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import androidx.compose.foundation.layout.Column
import org.jellyfin.androidtv.preference.constant.HomeStyle
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * A single list of every home section type. Selecting a shown section picks it up ("grab" mode):
 * up/down then moves it through the list and OK or Back drops it. Left on a shown section hides
 * it; selecting a hidden section shows it again.
 */
@Composable
fun SettingsHomeScreen() {
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	val router = LocalRouter.current

	val accountRevision = userSettingPreferences.accountRevision
	var visibleSections by remember(accountRevision) { mutableStateOf(userSettingPreferences.activeHomesections) }
	var grabbed by remember(accountRevision) { mutableStateOf<HomeSectionType?>(null) }
	var layoutBeforeReset by remember(accountRevision) { mutableStateOf<List<HomeSectionType>?>(null) }
	var layoutBeforePreset by remember(accountRevision) { mutableStateOf<List<HomeSectionType>?>(null) }
	var presetsExpanded by remember { mutableStateOf(false) }
	var limitsExpanded by remember { mutableStateOf(false) }
	var latestMoviesLimit by rememberPreference(userSettingPreferences, UserSettingPreferences.latestMoviesItemLimit)
	var latestShowsLimit by rememberPreference(userSettingPreferences, UserSettingPreferences.latestShowsItemLimit)
	var recentlyAddedMoviesLimit by rememberPreference(userSettingPreferences, UserSettingPreferences.recentlyAddedMoviesItemLimit)
	var recentlyAddedShowsLimit by rememberPreference(userSettingPreferences, UserSettingPreferences.recentlyAddedShowsItemLimit)
	var hideWatchedLatestMovies by rememberPreference(userSettingPreferences, UserSettingPreferences.hideWatchedLatestMovies)
	var hideWatchedRecentlyAddedMovies by rememberPreference(userSettingPreferences, UserSettingPreferences.hideWatchedRecentlyAddedMovies)
	val userPreferences = koinInject<UserPreferences>()
	var newHomeEnabled by rememberPreference(userPreferences, UserPreferences.newHomeEnabled)
	var newHomeCardTitles by rememberPreference(userPreferences, UserPreferences.newHomeCardTitles)
	var homeStyle by rememberPreference(userPreferences, UserPreferences.homeStyle)
	val listState = rememberLazyListState()
	val scope = rememberCoroutineScope()
	val hiddenSections = remember(visibleSections) {
		HomeSectionType.entries.filter { it != HomeSectionType.NONE && it !in visibleSections }
	}

	fun persist(newOrder: List<HomeSectionType>) {
		layoutBeforeReset = null
		layoutBeforePreset = null
		visibleSections = newOrder
		userSettingPreferences.activeHomesections = newOrder
	}

	// Scroll the list one row ahead of the move first so the picked-up row stays composed
	// (and focused) instead of leaving the viewport and losing focus.
	fun move(type: HomeSectionType, delta: Int) {
		scope.launch {
			val index = visibleSections.indexOf(type)
			val target = index + delta
			if (index < 0 || target !in visibleSections.indices) return@launch

			// +1 accounts for the header item preceding the visible sections
			val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target + 1 }
			val viewportEnd = listState.layoutInfo.viewportEndOffset
			val fullyVisible = targetItem != null &&
				targetItem.offset >= listState.layoutInfo.viewportStartOffset &&
				targetItem.offset + targetItem.size <= viewportEnd
			if (!fullyVisible) {
				if (delta < 0) listState.animateScrollToItem(target + 1)
				else listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
			}
			persist(visibleSections.toMutableList().apply { add(target, removeAt(index)) })
		}
	}

	BackHandler(enabled = grabbed != null) { grabbed = null }

	// Keep the picked-up row on screen after each move, including reaching the very top/bottom.
	LaunchedEffect(visibleSections, grabbed) {
		val type = grabbed ?: return@LaunchedEffect
		// +1 accounts for the header item preceding the visible sections
		val lazyIndex = visibleSections.indexOf(type) + 1
		if (lazyIndex <= 0) return@LaunchedEffect

		if (lazyIndex == 1) {
			listState.animateScrollToItem(0)
			return@LaunchedEffect
		}

		val info = listState.layoutInfo
		val item = info.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
		val fullyVisible = item != null &&
			item.offset >= info.viewportStartOffset &&
			item.offset + item.size <= info.viewportEndOffset
		if (fullyVisible) return@LaunchedEffect

		if (lazyIndex < listState.firstVisibleItemIndex + 1) {
			listState.animateScrollToItem(lazyIndex)
		} else {
			val rowSize = item?.size ?: info.visibleItemsInfo.lastOrNull()?.size ?: 0
			listState.animateScrollToItem(lazyIndex, -(info.viewportEndOffset - info.viewportStartOffset - rowSize))
		}
	}

	SettingsColumn(state = listState) {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.home_prefs)) },
				captionContent = { Text(stringResource(R.string.home_sections_hint)) },
			)
		}

		item(key = "new_home") {
			ListButton(
				toggleState = !newHomeEnabled,
				headingContent = { Text(stringResource(R.string.new_home_title)) },
				captionContent = { Text(stringResource(R.string.new_home_description)) },
				trailingContent = { SettingsSwitch(checked = !newHomeEnabled) },
				enabled = grabbed == null,
				onClick = { newHomeEnabled = !newHomeEnabled },
				modifier = Modifier.focusKey("new_home")
			)
		}
		// Kept in one list item with the card titles switch so the section rows' list positions don't shift
		if (newHomeEnabled) item(key = "new_home_card_titles") { Column {
			ListButton(
				headingContent = { Text(stringResource(R.string.home_style)) },
				captionContent = {
					Text(stringResource(if (homeStyle == HomeStyle.TOP_BAR) R.string.home_style_top_bar_description else R.string.home_style_side_menu_description))
				},
				trailingContent = { Text(stringResource(homeStyle.nameRes)) },
				enabled = grabbed == null,
				onClick = { homeStyle = if (homeStyle == HomeStyle.TOP_BAR) HomeStyle.SIDE_MENU else HomeStyle.TOP_BAR },
				modifier = Modifier.focusKey("home_style")
			)
			ListButton(
				toggleState = newHomeCardTitles,
				headingContent = { Text(stringResource(R.string.new_home_card_titles)) },
				captionContent = { Text(stringResource(R.string.new_home_card_titles_description)) },
				trailingContent = { SettingsSwitch(checked = newHomeCardTitles) },
				enabled = grabbed == null,
				onClick = { newHomeCardTitles = !newHomeCardTitles },
				modifier = Modifier.focusKey("new_home_card_titles")
			)
		} }

		items(visibleSections, key = { "visible_${it.name}" }) { type ->
			val isGrabbed = grabbed == type

			HomeSectionRow(
				type = type,
				visible = true,
				grabbed = isGrabbed,
				onClick = { grabbed = if (isGrabbed) null else type },
				onKey = { key ->
					when {
						isGrabbed && key == Key.DirectionUp -> { move(type, -1); true }
						isGrabbed && key == Key.DirectionDown -> { move(type, 1); true }
						!isGrabbed && grabbed == null && key == Key.DirectionLeft -> { persist(visibleSections - type); true }
						else -> false
					}
				},
				modifier = Modifier.focusKey("home_section_${type.name}")
			)
		}

		item(key = "home_layout_presets") {
			ListButton(
				headingContent = { Text(stringResource(R.string.home_layout_presets)) },
				captionContent = { Text(stringResource(R.string.home_layout_presets_description)) },
				enabled = grabbed == null,
				onClick = { presetsExpanded = !presetsExpanded },
				modifier = Modifier.focusKey("home_layout_presets")
			)
		}

		if (presetsExpanded) {
			items(HomeLayoutPreset.entries, key = { "preset_${it.name}" }) { preset ->
				ListButton(
					headingContent = { Text(stringResource(preset.nameRes)) },
					captionContent = { Text(stringResource(preset.descriptionRes)) },
					trailingContent = { RadioButton(checked = visibleSections == preset.sections) },
					enabled = grabbed == null,
					onClick = {
						if (visibleSections != preset.sections) {
							val previous = visibleSections.toList()
							persist(preset.sections)
							layoutBeforePreset = previous
						}
					},
					modifier = Modifier.focusKey("home_preset_${preset.name}")
				)
			}
		}

		if (layoutBeforePreset != null) {
			item(key = "undo_home_preset") {
				ListButton(
					headingContent = { Text(stringResource(R.string.home_layout_undo_preset)) },
					captionContent = { Text(stringResource(R.string.home_layout_undo_preset_description)) },
					enabled = grabbed == null,
					onClick = { layoutBeforePreset?.let(::persist) },
					modifier = Modifier.focusKey("undo_home_preset")
				)
			}
		}

		item(key = "home_shortcuts") {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.home_shortcuts)) },
				captionContent = { Text(stringResource(R.string.home_shortcuts_settings_description)) },
				enabled = grabbed == null,
				onClick = { router.push(Routes.HOME_SHORTCUTS) },
				modifier = Modifier.focusKey("home_shortcuts"),
			)
		}

		item(key = "hidden_recommendations") {
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.recommendation_settings)) },
				captionContent = { Text(stringResource(R.string.recommendation_settings_description)) },
				enabled = grabbed == null,
				onClick = { router.push(Routes.HOME_RECOMMENDATIONS) },
				modifier = Modifier.focusKey("hidden_recommendations"),
			)
		}

		item(key = "home_item_limits") {
			ListButton(
				headingContent = { Text(stringResource(R.string.home_item_limits)) },
				captionContent = { Text(stringResource(R.string.home_item_limits_description)) },
				enabled = grabbed == null,
				onClick = { limitsExpanded = !limitsExpanded },
				modifier = Modifier.focusKey("home_item_limits")
			)
		}
		if (limitsExpanded) {
			item(key = "hide_watched_latest_movies") {
				ListButton(
					toggleState = hideWatchedLatestMovies,
					headingContent = { Text(stringResource(R.string.home_hide_watched_latest_movies)) },
					captionContent = { Text(stringResource(R.string.home_hide_watched_latest_movies_description)) },
					trailingContent = { SettingsSwitch(checked = hideWatchedLatestMovies) },
					enabled = grabbed == null,
					onClick = { hideWatchedLatestMovies = !hideWatchedLatestMovies },
					modifier = Modifier.focusKey("hide_watched_latest_movies")
				)
			}
			item(key = "hide_watched_recently_added_movies") {
				ListButton(
					toggleState = hideWatchedRecentlyAddedMovies,
					headingContent = { Text(stringResource(R.string.home_hide_watched_recently_added_movies)) },
					captionContent = { Text(stringResource(R.string.home_hide_watched_recently_added_movies_description)) },
					trailingContent = { SettingsSwitch(checked = hideWatchedRecentlyAddedMovies) },
					enabled = grabbed == null,
					onClick = { hideWatchedRecentlyAddedMovies = !hideWatchedRecentlyAddedMovies },
					modifier = Modifier.focusKey("hide_watched_recently_added_movies")
				)
			}
			item(key = "latest_movies_limit") {
				HomeItemLimitControl(R.string.home_section_latest_movies, latestMoviesLimit, grabbed == null) { latestMoviesLimit = it }
			}
			item(key = "latest_shows_limit") {
				HomeItemLimitControl(R.string.home_section_latest_shows, latestShowsLimit, grabbed == null) { latestShowsLimit = it }
			}
			item(key = "recently_added_movies_limit") {
				HomeItemLimitControl(R.string.home_section_recently_added_movies, recentlyAddedMoviesLimit, grabbed == null) { recentlyAddedMoviesLimit = it }
			}
			item(key = "recently_added_shows_limit") {
				HomeItemLimitControl(R.string.home_section_recently_added_shows, recentlyAddedShowsLimit, grabbed == null) { recentlyAddedShowsLimit = it }
			}
			item(key = "reset_home_item_limits") {
				ListButton(
					headingContent = { Text(stringResource(R.string.home_item_limits_reset)) },
					captionContent = { Text(stringResource(R.string.home_item_limits_reset_description)) },
					enabled = grabbed == null,
					onClick = {
						latestMoviesLimit = UserSettingPreferences.latestMoviesItemLimit.defaultValue
						latestShowsLimit = UserSettingPreferences.latestShowsItemLimit.defaultValue
						recentlyAddedMoviesLimit = UserSettingPreferences.recentlyAddedMoviesItemLimit.defaultValue
						recentlyAddedShowsLimit = UserSettingPreferences.recentlyAddedShowsItemLimit.defaultValue
					},
					modifier = Modifier.focusKey("reset_home_item_limits")
				)
			}
		}

		item(key = "reset_home_layout") {
			val canUndo = layoutBeforeReset != null
			ListButton(
				headingContent = {
					Text(stringResource(if (canUndo) R.string.home_layout_undo_reset else R.string.home_layout_reset))
				},
				captionContent = {
					Text(stringResource(if (canUndo) R.string.home_layout_undo_reset_description else R.string.home_layout_reset_description))
				},
				enabled = grabbed == null && (canUndo || visibleSections != UserSettingPreferences.defaultHomeSectionOrder),
				onClick = {
					val previous = layoutBeforeReset
					if (previous != null) {
						persist(previous)
					} else {
						val current = visibleSections.toList()
						persist(UserSettingPreferences.defaultHomeSectionOrder)
						layoutBeforeReset = current
					}
				},
				modifier = Modifier.focusKey("reset_home_layout")
			)
		}

		if (hiddenSections.isNotEmpty()) {
			item {
				ListSection(
					headingContent = { Text(stringResource(R.string.home_sections_hidden)) },
				)
			}

			items(hiddenSections, key = { "hidden_${it.name}" }) { type ->
				HomeSectionRow(
					type = type,
					visible = false,
					grabbed = false,
					onClick = { if (grabbed == null) persist(visibleSections + type) },
					onKey = { false },
					modifier = Modifier.focusKey("home_section_${type.name}")
				)
			}
		}
	}
}

@Composable
private fun HomeItemLimitControl(titleRes: Int, value: Int, enabled: Boolean, onValueChange: (Int) -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }
	ListControl(
		headingContent = { Text(stringResource(titleRes)) },
		captionContent = { Text(stringResource(R.string.home_item_limit_hint)) },
		enabled = enabled,
		interactionSource = interactionSource,
		modifier = Modifier.focusKey("home_item_limit_$titleRes"),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			RangeControl(
				modifier = Modifier.height(4.dp).weight(1f),
				interactionSource = interactionSource,
				min = 5f,
				max = 50f,
				stepForward = 5f,
				value = value.coerceIn(5, 50).toFloat(),
				onValueChange = { onValueChange(it.roundToInt()) },
				enabled = enabled,
			)
			Spacer(Modifier.width(16.dp))
			Text(value.toString())
		}
	}
}

@Composable
private fun HomeSectionRow(
	type: HomeSectionType,
	visible: Boolean,
	grabbed: Boolean,
	onClick: () -> Unit,
	onKey: (Key) -> Boolean,
	modifier: Modifier = Modifier,
) {
	val rowColors = ListControlDefaults.colors()
	val movingColor = Color(0xFF006B9A)
	ListButton(
		colors = if (grabbed) rowColors.copy(
			containerColor = movingColor,
			focusedContainerColor = movingColor,
		) else rowColors,
		modifier = modifier
			.alpha(if (visible) 1f else 0.5f)
			.onKeyEvent { event ->
				if (event.type != KeyEventType.KeyDown) false else onKey(event.key)
			},
		leadingContent = {
			Icon(
				imageVector = ImageVector.vectorResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off),
				contentDescription = null,
				modifier = Modifier.size(24.dp),
			)
		},
		headingContent = { Text(stringResource(type.nameRes)) },
		trailingContent = if (visible) {
			{
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_drag_handle),
					contentDescription = null,
					modifier = Modifier.size(24.dp),
				)
			}
		} else null,
		onClick = onClick,
	)
}
