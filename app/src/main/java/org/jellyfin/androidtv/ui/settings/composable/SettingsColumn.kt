package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jellyfin.design.Tokens

@Composable
fun SettingsColumn(
	state: LazyListState = rememberLazyListState(),
	modifier: Modifier = Modifier,
	content: LazyListScope.() -> Unit,
) = LazyColumn(
	state = state,
	modifier = modifier
		.padding(Tokens.Space.spaceSm),
	verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs),
	// Keep the first/last item's focus outline inside the scrolling viewport.
	contentPadding = PaddingValues(vertical = Tokens.Space.spaceXs),
	content = content,
)
