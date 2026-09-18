package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import org.koin.compose.koinInject

/**
 * A single reorderable, toggleable list of every home section type, instead of navigating into
 * numbered slots to assign a type to each. Enabled sections are shown in their display order at
 * the top; while one is focused, left/right moves it up/down. Selecting any row toggles it
 * between shown and hidden - hidden sections are listed below, dimmed, in declaration order.
 */
@Composable
fun SettingsHomeScreen() {
	val userSettingPreferences = koinInject<UserSettingPreferences>()

	var visibleSections by remember { mutableStateOf(userSettingPreferences.activeHomesections) }
	val hiddenSections = remember(visibleSections) {
		HomeSectionType.entries.filter { it != HomeSectionType.NONE && it !in visibleSections }
	}

	fun persist(newOrder: List<HomeSectionType>) {
		visibleSections = newOrder
		userSettingPreferences.activeHomesections = newOrder
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.home_prefs)) },
			)
		}

		items(visibleSections, key = { "visible_${it.name}" }) { type ->
			val index = visibleSections.indexOf(type)

			HomeSectionRow(
				type = type,
				visible = true,
				onToggle = { persist(visibleSections - type) },
				onMoveUp = {
					if (index > 0) persist(visibleSections.toMutableList().apply { add(index - 1, removeAt(index)) })
				},
				onMoveDown = {
					if (index in 0 until visibleSections.lastIndex) persist(visibleSections.toMutableList().apply { add(index + 1, removeAt(index)) })
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
					onToggle = { persist(visibleSections + type) },
					onMoveUp = {},
					onMoveDown = {},
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
	onToggle: () -> Unit,
	onMoveUp: () -> Unit,
	onMoveDown: () -> Unit,
	modifier: Modifier = Modifier,
) {
	ListButton(
		modifier = modifier
			.alpha(if (visible) 1f else 0.5f)
			.onKeyEvent { event ->
				if (!visible || event.type != KeyEventType.KeyDown) return@onKeyEvent false

				when (event.key) {
					Key.DirectionLeft -> { onMoveUp(); true }
					Key.DirectionRight -> { onMoveDown(); true }
					else -> false
				}
			},
		leadingContent = {
			Icon(
				imageVector = ImageVector.vectorResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off),
				contentDescription = null,
				modifier = Modifier.size(24.dp),
			)
		},
		headingContent = { Text(stringResource(type.nameRes)) },
		onClick = onToggle,
	)
}
