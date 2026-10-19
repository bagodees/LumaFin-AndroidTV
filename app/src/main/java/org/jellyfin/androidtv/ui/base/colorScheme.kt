package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.jellyfin.design.Tokens

/** Logo-derived palette. Classic views use the matching lumafin_* resources. */
internal object LumaFinPalette {
	val Background = Color(0xFF080D1C)
	val Surface = Color(0xFF101A2D)
	val Raised = Color(0xFF1B2A45)
	val FocusedRow = Color(0xFF233E64)
	val Cyan = Color(0xFF39CEF3)
	val Blue = Color(0xFF679BFF)
	val Purple = Color(0xFFB9A4FF)
	val Selected = Color(0xFF302448)
	val Text = Color(0xFFF2F5FF)
	val SecondaryText = Color(0xFFB6C3DC)
	val Muted = Color(0xFF9BAFD0)
}

fun colorScheme(): ColorScheme = ColorScheme(
	background = LumaFinPalette.Background,
	onBackground = LumaFinPalette.Text,
	button = LumaFinPalette.Raised,
	onButton = LumaFinPalette.SecondaryText,
	buttonFocused = LumaFinPalette.Cyan,
	onButtonFocused = LumaFinPalette.Background,
	buttonDisabled = LumaFinPalette.Raised.copy(alpha = 0.4f),
	onButtonDisabled = LumaFinPalette.Muted.copy(alpha = 0.65f),
	buttonActive = LumaFinPalette.Selected,
	onButtonActive = LumaFinPalette.Purple,
	input = LumaFinPalette.Muted,
	onInput = LumaFinPalette.SecondaryText,
	inputFocused = LumaFinPalette.Cyan,
	onInputFocused = LumaFinPalette.Text,
	rangeControlBackground = LumaFinPalette.Raised,
	rangeControlFill = LumaFinPalette.Blue,
	rangeControlKnob = LumaFinPalette.Cyan,
	seekbarBuffer = LumaFinPalette.Muted,
	recording = Tokens.Color.colorRed300,
	onRecording = Tokens.Color.colorRed25,
	badge = LumaFinPalette.Cyan,
	onBadge = LumaFinPalette.Background,
	listHeader = LumaFinPalette.Text,
	listOverline = LumaFinPalette.Muted,
	listHeadline = LumaFinPalette.Text,
	listCaption = LumaFinPalette.SecondaryText,
	listButton = Color.Transparent,
	listButtonFocused = LumaFinPalette.FocusedRow,
	surface = LumaFinPalette.Surface,
	scrim = LumaFinPalette.Background.copy(alpha = 0.78f),
)

@Immutable
data class ColorScheme(
	val background: Color,
	val onBackground: Color,

	val button: Color,
	val onButton: Color,
	val buttonFocused: Color,
	val onButtonFocused: Color,
	val buttonDisabled: Color,
	val onButtonDisabled: Color,
	val buttonActive: Color,
	val onButtonActive: Color,

	val input: Color,
	val onInput: Color,
	val inputFocused: Color,
	val onInputFocused: Color,

	val rangeControlBackground: Color,
	val rangeControlFill: Color,
	val rangeControlKnob: Color,
	val seekbarBuffer: Color,

	val recording: Color,
	val onRecording: Color,

	val badge: Color,
	val onBadge: Color,

	val listHeader: Color,
	val listOverline: Color,
	val listHeadline: Color,
	val listCaption: Color,
	val listButton: Color,
	val listButtonFocused: Color,

	val surface: Color,
	val scrim: Color,
)

val LocalColorScheme = staticCompositionLocalOf { colorScheme() }
