package org.jellyfin.androidtv.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import org.jellyfin.androidtv.ui.composable.rememberReducedMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filterNotNull
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.shared.toolbar.QuickUserSwitcher
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

/** Library tabs that fit across the top bar beside Home; more than this shows "Libraries" for the rest. */
private const val MaxLibraryTabs = 5

/**
 * The top bar Home style (concept B): Home and library tabs, then search, settings, the profile and the
 * clock. Down returns to [contentFocusRequester]; entering the bar lands on Home.
 */
@Composable
fun HomeTopBar(
	homeFocusRequester: FocusRequester,
	contentFocusRequester: FocusRequester,
	modifier: Modifier = Modifier,
) {
	val navigationRepository = koinInject<NavigationRepository>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val itemLauncher = koinInject<ItemLauncher>()
	val userRepository = koinInject<UserRepository>()
	val api = koinInject<ApiClient>()
	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userImage = remember(currentUser) { currentUser?.primaryImage?.getUrl(api) }
	var showUserSwitcher by remember { mutableStateOf(false) }
	if (showUserSwitcher) QuickUserSwitcher(onDismissRequest = { showUserSwitcher = false })

	val libraries = rememberMenuLibraries()
	val overflow = libraries.isNullOrEmpty() || libraries.size > MaxLibraryTabs
	val shownLibraries = when {
		libraries == null -> emptyList()
		overflow -> libraries.take(MaxLibraryTabs - 1)
		else -> libraries
	}
	val toContent = Modifier.focusProperties { down = contentFocusRequester }
	var hasFocus by remember { mutableStateOf(false) }

	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		modifier = modifier
			.fillMaxWidth()
			.height(88.dp)
			.padding(horizontal = 48.dp)
			// Focus arrives from the Leanback rows through the view system, which picks the nearest tab;
			// entering the bar should always land on Home, like the side menu
			.onFocusChanged { state ->
				if (state.hasFocus && !hasFocus) homeFocusRequester.requestFocus()
				hasFocus = state.hasFocus
			}
			.focusGroup(),
	) {
		TopBarTab(stringResource(R.string.lbl_home), active = true, modifier = toContent.focusRequester(homeFocusRequester)) {}
		for (library in shownLibraries) {
			TopBarTab(library.name.orEmpty(), modifier = toContent) {
				// Live TV goes straight to the guide, like the side menu
				if (library.collectionType == CollectionType.LIVETV) navigationRepository.navigate(Destinations.liveTvGuide)
				else itemLauncher.launchUserView(library)
			}
		}
		if (overflow) TopBarTab(stringResource(R.string.home_section_library), modifier = toContent) {
			navigationRepository.navigate(Destinations.libraries)
		}
		Spacer(Modifier.weight(1f))
		TopBarIcon(R.drawable.ic_search, stringResource(R.string.lbl_search), toContent) { navigationRepository.navigate(Destinations.search()) }
		TopBarIcon(R.drawable.ic_settings, stringResource(R.string.settings), toContent) { settingsViewModel.show() }
		ButtonBase(
			onClick = { showUserSwitcher = true },
			shape = CircleShape,
			colors = ButtonDefaults.colors(containerColor = Color.Transparent, contentColor = LumaFinPalette.Text),
			modifier = toContent.size(48.dp),
		) {
			ProfilePicture(
				url = userImage,
				contentDescription = stringResource(R.string.lbl_switch_user),
				initial = currentUser?.name?.firstOrNull()?.uppercase(),
				modifier = Modifier.align(Alignment.Center).size(40.dp).clip(CircleShape),
			)
		}
		ToolbarClock(modifier = Modifier.padding(start = 16.dp))
	}
}

@Composable
private fun TopBarTab(text: String, modifier: Modifier = Modifier, active: Boolean = false, onClick: () -> Unit) {
	ButtonBase(
		onClick = onClick,
		shape = RoundedCornerShape(24.dp),
		colors = ButtonDefaults.colors(
			containerColor = if (active) LumaFinPalette.Raised else Color.Transparent,
			contentColor = if (active) LumaFinPalette.Cyan else LumaFinPalette.SecondaryText,
		),
		modifier = modifier.height(48.dp),
	) {
		Text(
			text,
			fontSize = 17.sp,
			fontWeight = FontWeight.SemiBold,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
		)
	}
}

@Composable
private fun TopBarIcon(icon: Int, description: String, modifier: Modifier, onClick: () -> Unit) {
	ButtonBase(
		onClick = onClick,
		shape = CircleShape,
		colors = ButtonDefaults.colors(containerColor = Color.Transparent, contentColor = LumaFinPalette.SecondaryText),
		modifier = modifier.size(48.dp),
	) {
		Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = description, modifier = Modifier.align(Alignment.Center).size(24.dp))
	}
}

/**
 * The focused title's details for the top bar style, over its backdrop under the bar. It never
 * takes focus.
 */
@Composable
fun HomeBackdropInfo(item: BaseItemDto?, modifier: Modifier = Modifier) {
	// Starts just under the bar and takes only the height its details need; the rows below follow it
	// up and down, animated unless reduced motion is on
	val reducedMotion = rememberReducedMotion()
	Box(
		modifier = modifier
			.fillMaxWidth()
			.animateContentSize(if (reducedMotion) snap() else tween(200))
			.padding(start = 48.dp, top = 4.dp, end = 48.dp, bottom = 12.dp),
	) {
		if (item == null) return@Box
		Row(verticalAlignment = Alignment.Top, modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
				FocusedTitleInfo(item)
				timeLeft(item)?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LumaFinPalette.Cyan) }
			}
			// The title's logo on the right, over the backdrop
			if ((item.itemImages[ImageType.LOGO] ?: item.parentImages[ImageType.LOGO]) != null) {
				PreviewLogo(item, Modifier.weight(0.6f).height(120.dp).padding(start = 32.dp))
			} else {
				Spacer(Modifier.weight(0.6f))
			}
		}
	}
}

/** "1h 5m left" for a title that's partly watched, otherwise null. */
@Composable
private fun timeLeft(item: BaseItemDto): String? {
	val context = LocalContext.current
	val position = item.userData?.playbackPositionTicks ?: 0
	val runtime = item.runTimeTicks ?: 0
	if (position <= 0 || runtime <= position) return null
	val seconds = ((runtime - position) / 10_000_000).toInt()
	return stringResource(R.string.home_time_left, TimeUtils.formatSeconds(context, seconds))
}
