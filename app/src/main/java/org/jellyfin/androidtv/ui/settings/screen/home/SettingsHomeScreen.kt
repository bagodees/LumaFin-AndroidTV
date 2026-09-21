package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * A single list of every home section type. Selecting a shown section picks it up ("grab" mode):
 * up/down then moves it through the list and OK or Back drops it. Left on a shown section hides
 * it; selecting a hidden section shows it again.
 */
@Composable
fun SettingsHomeScreen() {
	val userSettingPreferences = koinInject<UserSettingPreferences>()

	var visibleSections by remember { mutableStateOf(userSettingPreferences.activeHomesections) }
	var grabbed by remember { mutableStateOf<HomeSectionType?>(null) }
	val listState = rememberLazyListState()
	val scope = rememberCoroutineScope()
	val hiddenSections = remember(visibleSections) {
		HomeSectionType.entries.filter { it != HomeSectionType.NONE && it !in visibleSections }
	}

	fun persist(newOrder: List<HomeSectionType>) {
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
private fun HomeSectionRow(
	type: HomeSectionType,
	visible: Boolean,
	grabbed: Boolean,
	onClick: () -> Unit,
	onKey: (Key) -> Boolean,
	modifier: Modifier = Modifier,
) {
	ListButton(
		modifier = modifier
			.alpha(if (visible) 1f else 0.5f)
			.then(if (grabbed) Modifier.background(Color(0x3300A4DC), RoundedCornerShape(8.dp)) else Modifier)
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
