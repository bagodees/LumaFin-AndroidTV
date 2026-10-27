package org.jellyfin.androidtv.ui.settings.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes

private data class SettingsCategory(val route: String, val title: Int, val icon: Int)

private val categories = listOf(
	SettingsCategory(Routes.HOME, R.string.home_prefs, R.drawable.ic_house),
	SettingsCategory(Routes.CUSTOMIZATION, R.string.settings_category_appearance, R.drawable.ic_adjust),
	SettingsCategory(Routes.LIBRARIES, R.string.pref_libraries, R.drawable.ic_grid),
	SettingsCategory(Routes.PLAYBACK, R.string.pref_playback, R.drawable.ic_next),
	SettingsCategory(Routes.CUSTOMIZATION_SUBTITLES, R.string.pref_subtitles, R.drawable.ic_subtitles),
	SettingsCategory(Routes.AUTHENTICATION, R.string.settings_group_accounts, R.drawable.ic_users),
	SettingsCategory(Routes.HELP, R.string.settings_group_help, R.drawable.ic_maintenance),
	SettingsCategory(Routes.SEARCH, R.string.settings_search_title, R.drawable.ic_search),
)

/**
 * Full-screen settings (#83): categories on the left and the chosen category's options on the right.
 * Moving through the categories shows each one; Right or OK goes into its options. Back from the
 * options returns to the categories, and Back from there closes settings. Every existing route and
 * preference store is reused unchanged.
 */
@Composable
fun SettingsWorkspace(onDismissRequest: () -> Unit) {
	val router = LocalRouter.current
	val categoryFocus = remember { FocusRequester() }
	val contentFocus = remember { FocusRequester() }
	var selected by remember { mutableStateOf(Routes.HOME) }
	var categoriesFocused by remember { mutableStateOf(true) }
	val currentRoute = router.backStack.lastOrNull()?.route

	fun open(route: String) {
		if (router.backStack.lastOrNull()?.route == route && router.backStack.size == 2) return
		router.backStack.clear()
		router.push(Routes.MAIN)
		router.push(route)
	}

	LaunchedEffect(currentRoute) {
		// Search results and nested pages keep their category highlighted
		categories.sortedByDescending { it.route.length }.firstOrNull {
			currentRoute == it.route || currentRoute?.startsWith("${it.route}/") == true
		}?.let { selected = it.route }
	}
	LaunchedEffect(Unit) { runCatching { categoryFocus.requestFocus() } }

	Row(Modifier.fillMaxSize().background(LumaFinPalette.Background).padding(horizontal = 48.dp, vertical = 32.dp)) {
		Column(
			verticalArrangement = Arrangement.spacedBy(2.dp),
			modifier = Modifier
				.width(300.dp)
				.fillMaxHeight()
				.onFocusChanged { categoriesFocused = it.hasFocus }
				.focusRestorer(categoryFocus)
				.focusGroup(),
		) {
			Text(
				stringResource(R.string.settings),
				fontSize = 28.sp,
				fontWeight = FontWeight.Bold,
				color = LumaFinPalette.Text,
				modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
			)
			for (category in categories) {
				val active = selected == category.route
				CategoryItem(
					category = category,
					active = active,
					modifier = Modifier
						.then(if (active) Modifier.focusRequester(categoryFocus) else Modifier)
						.focusProperties { right = contentFocus }
						.onFocusChanged { if (it.isFocused) { selected = category.route; open(category.route) } },
					onClick = { open(category.route); runCatching { contentFocus.requestFocus() } },
				)
			}
		}
		Box(
			modifier = Modifier
				.padding(start = 32.dp)
				.weight(1f)
				.widthIn(max = 1100.dp)
				.fillMaxHeight()
				.focusRequester(contentFocus)
				.focusRestorer()
				.focusGroup(),
		) {
			SettingsRouterContent()
		}
	}

	// Inside a category's options, Back first returns to the categories (deeper pages still go back a level)
	BackHandler(enabled = !categoriesFocused && router.backStack.size <= 2) { runCatching { categoryFocus.requestFocus() } }
	BackHandler(enabled = categoriesFocused, onBack = onDismissRequest)
}

@Composable
private fun CategoryItem(category: SettingsCategory, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
	ButtonBase(
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		colors = ButtonDefaults.colors(
			containerColor = if (active) LumaFinPalette.Raised else Color.Transparent,
			contentColor = if (active) LumaFinPalette.Cyan else LumaFinPalette.SecondaryText,
		),
		modifier = modifier.fillMaxWidth().height(48.dp),
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp),
		) {
			Icon(imageVector = ImageVector.vectorResource(category.icon), contentDescription = null, modifier = Modifier.size(22.dp))
			Text(
				stringResource(category.title),
				fontSize = 17.sp,
				fontWeight = FontWeight.SemiBold,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(start = 14.dp),
			)
		}
	}
}
