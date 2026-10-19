package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject

/** High contrast and stronger focus (#23). Text size is applied to the whole activity instead. */
@Immutable
data class AccessibilityOptions(val highContrast: Boolean = false, val strongFocus: Boolean = false)

val LocalAccessibility = staticCompositionLocalOf { AccessibilityOptions() }

/** For Compose content not wrapped in [JellyfinTheme], such as Leanback card views. */
@Composable
fun rememberAccessibilityOptions(): AccessibilityOptions {
	val preferences = koinInject<UserPreferences>()
	val options by preferences.accessibilityChanges.collectAsState(
		initial = preferences[UserPreferences.highContrast] to preferences[UserPreferences.strongFocus]
	)
	return AccessibilityOptions(options.first, options.second)
}

/** Absolute values, so applying it to an already high-contrast scheme changes nothing. */
fun ColorScheme.highContrast() = copy(
	onBackground = Tokens.Color.colorWhite,
	onButton = Tokens.Color.colorWhite,
	onButtonFocused = Tokens.Color.colorBlack,
	buttonFocused = Tokens.Color.colorWhite,
	onButtonDisabled = Tokens.Color.colorGrey300,
	onInput = Tokens.Color.colorWhite,
	listHeader = Tokens.Color.colorWhite,
	listOverline = Tokens.Color.colorGrey100,
	listHeadline = Tokens.Color.colorWhite,
	listCaption = Tokens.Color.colorGrey25,
	listButtonFocused = Tokens.Color.colorBluegrey600,
	surface = Tokens.Color.colorBluegrey950,
	scrim = Tokens.Color.colorBlack.copy(alpha = 0.85f),
)

val FocusOutlineColor = Color.White
val FocusOutlineWidth = 3.dp

/** A thick white outline on the focused control when Stronger focus outline is on. */
@Composable
fun Modifier.focusOutline(focused: Boolean, shape: Shape, strong: Boolean = LocalAccessibility.current.strongFocus): Modifier =
	if (focused && strong) border(FocusOutlineWidth, FocusOutlineColor, shape) else this
