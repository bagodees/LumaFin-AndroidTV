package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.LocalTypography
import org.jellyfin.design.Tokens

@Composable
fun SettingsLayout(
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit,
) {
	Box(
		modifier = modifier
			.padding(Tokens.Space.spaceMd)
			.clip(LocalShapes.current.large)
			.background(JellyfinTheme.colorScheme.surface)
			.width(380.dp)
			.fillMaxHeight()
			.focusRestorer(),
	) {
		val typography = JellyfinTheme.typography
		CompositionLocalProvider(LocalTypography provides typography.copy(
			listHeadline = typography.listHeadline.copy(fontSize = 15.sp, lineHeight = 22.sp),
			listCaption = typography.listCaption.copy(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
		)) {
			content()
		}
	}
}
