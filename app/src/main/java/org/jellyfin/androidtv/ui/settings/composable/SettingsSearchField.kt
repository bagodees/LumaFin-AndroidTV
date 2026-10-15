package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput

/** Navigation focus highlights a button; only an explicit click creates an editor. */
@Composable
fun SettingsSearchField(query: String, label: String, onQueryChange: (String) -> Unit,
	onSubmit: () -> Unit, modifier: Modifier = Modifier) {
	var editing by remember { mutableStateOf(false) }
	var editorWasFocused by remember { mutableStateOf(false) }
	val requester = remember { FocusRequester() }
	val keyboard = LocalSoftwareKeyboardController.current
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	val color = if (focused) JellyfinTheme.colorScheme.inputFocused to JellyfinTheme.colorScheme.onInputFocused
		else JellyfinTheme.colorScheme.input to JellyfinTheme.colorScheme.onInput
	LaunchedEffect(editing) {
		if (editing) { requester.requestFocus(); keyboard?.show() }
	}
	Box(modifier) {
		if (editing) {
			SearchTextInput(query, onQueryChange, onQuerySubmit = {
				editing = false
				editorWasFocused = false
				keyboard?.hide()
				onSubmit()
			}, placeholder = label, modifier = Modifier.fillMaxWidth().focusRequester(requester).onFocusChanged {
				if (it.isFocused) editorWasFocused = true
				else if (editorWasFocused) {
					editorWasFocused = false
					editing = false
					keyboard?.hide()
				}
			})
		} else {
			Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
				.clickable(interactionSource = interaction, indication = null) { editing = true }
				.border(2.dp, color.first, RoundedCornerShape(percent = 30)).padding(12.dp)) {
				Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = color.second)
				Spacer(Modifier.width(12.dp))
				Text(query.ifEmpty { label }, fontSize = 16.sp, color = color.second, maxLines = 1)
			}
		}
	}
}
