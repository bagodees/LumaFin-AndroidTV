package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalAccessibility
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.list.ListControlColors
import org.jellyfin.androidtv.ui.base.list.ListControlDefaults
import org.jellyfin.androidtv.ui.base.list.ListItemContent
import org.jellyfin.androidtv.ui.base.list.ListControl as BaseListControl

/** Settings-only styling, including slider rows. Other app menus keep their own layout. */
@Composable
fun SettingsControl(
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: InteractionSource? = null,
	colors: ListControlColors = ListControlDefaults.colors(),
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	footerContent: (@Composable () -> Unit)? = null,
) {
	val source = interactionSource ?: remember { MutableInteractionSource() }
	val focused by source.collectIsFocusedAsState()
	val strong = LocalAccessibility.current.strongFocus
	val bringIntoView = remember { BringIntoViewRequester() }
	LaunchedEffect(focused) {
		// A slider's small thumb is not enough: reveal its label and entire outline.
		if (focused) bringIntoView.bringIntoView()
	}
	BaseListControl(
		headingContent = headingContent,
		modifier = modifier.bringIntoViewRequester(bringIntoView).heightIn(min = 56.dp).then(
			if (focused && enabled) Modifier.border(
				if (strong) 3.dp else 2.dp,
				if (strong) Color.White else JellyfinTheme.colorScheme.inputFocused,
				JellyfinTheme.shapes.large,
			) else Modifier
		),
		enabled = enabled, interactionSource = source, colors = colors,
		overlineContent = overlineContent, captionContent = captionContent,
		leadingContent = leadingContent, trailingContent = trailingContent, footerContent = footerContent,
	)
}

/** Navigation is explicit: actions and choice rows must never imply another screen. */
@Composable
fun SettingsButton(
	onClick: () -> Unit,
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource? = null,
	colors: ListControlColors = ListControlDefaults.colors(),
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	navigates: Boolean = false,
	toggleState: Boolean? = null,
	footerContent: (@Composable () -> Unit)? = null,
) {
	val source = interactionSource ?: remember { MutableInteractionSource() }
	val click = if (toggleState != null) Modifier.toggleable(
		value = toggleState, enabled = enabled, role = Role.Switch,
		interactionSource = source, indication = null, onValueChange = { onClick() },
	) else Modifier.clickable(
		interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick,
	)
	SettingsControl(
		headingContent = headingContent, modifier = modifier.then(click), enabled = enabled,
		interactionSource = source, colors = colors, overlineContent = overlineContent,
		captionContent = captionContent, leadingContent = leadingContent,
		trailingContent = if (navigates) ({
			Row(verticalAlignment = Alignment.CenterVertically) {
				trailingContent?.invoke()
				if (trailingContent != null) Spacer(Modifier.width(8.dp))
				Icon(painterResource(R.drawable.ic_settings_chevron), contentDescription = null,
					tint = JellyfinTheme.colorScheme.listCaption, modifier = Modifier.size(20.dp))
			}
		}) else trailingContent,
		footerContent = footerContent,
	)
}

/** Passive indicator: the parent row owns focus, click handling and switch semantics. */
@Composable
fun SettingsSwitch(checked: Boolean, modifier: Modifier = Modifier) {
	Box(
		modifier = modifier.size(36.dp, 22.dp)
			.background(if (checked) JellyfinTheme.colorScheme.inputFocused else JellyfinTheme.colorScheme.listOverline.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
			.padding(3.dp),
		contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
	) {
		Box(Modifier.size(16.dp).background(
			if (checked) JellyfinTheme.colorScheme.background else JellyfinTheme.colorScheme.listHeadline, CircleShape,
		))
	}
}

@Composable
fun SettingsSection(
	modifier: Modifier = Modifier,
	headingContent: @Composable () -> Unit,
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	footerContent: (@Composable () -> Unit)? = null,
) {
	ListItemContent(
		modifier = modifier.padding(top = 4.dp), headingContent = headingContent,
		overlineContent = overlineContent, captionContent = captionContent,
		leadingContent = leadingContent, trailingContent = trailingContent, footerContent = footerContent,
		headingStyle = JellyfinTheme.typography.listHeader.copy(
			fontSize = if (overlineContent != null) 20.sp else 14.sp,
			lineHeight = if (overlineContent != null) 26.sp else 20.sp,
			fontWeight = FontWeight.SemiBold,
			color = if (overlineContent != null) JellyfinTheme.colorScheme.listHeader else JellyfinTheme.colorScheme.inputFocused,
		),
	)
}
