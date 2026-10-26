package org.jellyfin.androidtv.ui.home

import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.HorizontalGridView
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.LaunchedEffect
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.sdk.model.api.CollectionType
import timber.log.Timber
import java.util.UUID
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.androidtv.util.sdk.compat.canResume
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.composable.rememberReducedMotion
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.browsing.composable.inforow.BaseItemInfoRow
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.ui.shared.toolbar.QuickUserSwitcher
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.androidtv.util.descriptionForDisplay
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

/** Width of the side menu while the rows have focus; the rows are laid out beside this. */
internal val RailCollapsedWidth = 64.dp
private val RailExpandedWidth = 200.dp

/** The library a screen belongs to: matched by [id] first, then by [collectionType] (a movie's details highlight Movies). */
data class SideMenuLibrary(val id: UUID?, val collectionType: CollectionType?)

/** Libraries shown in the side menu, kept between screens so the menu doesn't change while they reload. */
private object SideMenuLibraries {
	val cached = MutableStateFlow<Pair<UUID, List<BaseItemDto>>?>(null)
}

/** Library entries that fit in the menu above Settings at TV height; more than this shows "Libraries" for the rest. */
private const val MaxLibraryEntries = 5

/** Main libraries first; collections and playlists after everything else. Same-type libraries keep the server's order. */
private val libraryOrder = listOf(
	CollectionType.MOVIES,
	CollectionType.TVSHOWS,
	CollectionType.LIVETV,
	CollectionType.MUSIC,
	CollectionType.MUSICVIDEOS,
	CollectionType.HOMEVIDEOS,
	CollectionType.PHOTOS,
	null,
	CollectionType.BOXSETS,
	CollectionType.PLAYLISTS,
)

private fun libraryIcon(type: CollectionType?) = when (type) {
	CollectionType.MOVIES -> R.drawable.ic_movie
	CollectionType.TVSHOWS -> R.drawable.ic_tv
	CollectionType.LIVETV -> R.drawable.ic_tv_guide
	CollectionType.MUSIC -> R.drawable.ic_music_album
	CollectionType.MUSICVIDEOS, CollectionType.HOMEVIDEOS -> R.drawable.ic_clapperboard
	CollectionType.PHOTOS -> R.drawable.ic_photos
	CollectionType.BOXSETS -> R.drawable.ic_grid
	CollectionType.PLAYLISTS -> R.drawable.ic_album
	else -> R.drawable.ic_folder
}

/**
 * Side menu for the new navigation (concept A): slim and icons-only while browsing, expanding over the
 * content with labels while it has focus. [active] is highlighted and is where focus lands on entry.
 */
@Composable
fun MainSideRail(
	active: MainToolbarActiveButton,
	activeFocusRequester: FocusRequester,
	contentFocusRequester: FocusRequester?,
	modifier: Modifier = Modifier,
	onRight: (() -> Unit)? = null,
	// False on screens that only highlight a section (details pages, library grids), so its item still opens it
	onActiveScreen: Boolean = true,
	// Which library the screen belongs to, so its entry is highlighted when [active] is Libraries
	library: SideMenuLibrary? = null,
) {
	// Right from any menu item returns to where you were in the content, not the nearest control.
	// View-based content can't take a FocusRequester, so it supplies [onRight] instead.
	val toRows = when {
		onRight != null -> Modifier.onPreviewKeyEvent { event ->
			if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) { onRight(); true } else false
		}
		contentFocusRequester != null -> Modifier.focusProperties { right = contentFocusRequester }
		else -> Modifier
	}
	val navigationRepository = koinInject<NavigationRepository>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val userViews = koinInject<UserViewsRepository>()
	val serverRepository = koinInject<ServerRepository>()
	val currentServer by serverRepository.currentServer.collectAsState()
	val itemLauncher = koinInject<ItemLauncher>()
	val userRepository = koinInject<UserRepository>()
	val api = koinInject<ApiClient>()
	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userImage = remember(currentUser) { currentUser?.primaryImage?.getUrl(api) }
	val cachedLibraries by SideMenuLibraries.cached.collectAsState()
	val libraries = cachedLibraries?.takeIf { it.first == currentUser?.id }?.second
	LaunchedEffect(currentUser?.id) {
		val userId = currentUser?.id ?: return@LaunchedEffect
		runCatching { userViews.views.first().sortedBy { libraryOrder.indexOf(it.collectionType).let { i -> if (i < 0) libraryOrder.size else i } } }
			.onSuccess { SideMenuLibraries.cached.value = userId to it }
			.onFailure { Timber.w(it, "Failed to load libraries for the side menu") }
	}
	// One entry per library; "Libraries" only when they don't all fit (or before they load)
	val overflow = libraries.isNullOrEmpty() || libraries.size > MaxLibraryEntries
	val shownLibraries = when {
		libraries == null -> emptyList()
		overflow -> libraries.take(MaxLibraryEntries - 1)
		else -> libraries
	}
	val activeLibrary = if (active != MainToolbarActiveButton.Libraries || library == null) null
		else shownLibraries.firstOrNull { it.id == library.id }
			?: shownLibraries.firstOrNull { library.collectionType != null && it.collectionType == library.collectionType }
	var showUserSwitcher by remember { mutableStateOf(false) }
	if (showUserSwitcher) QuickUserSwitcher(onDismissRequest = { showUserSwitcher = false })
	var expanded by remember { mutableStateOf(false) }
	val reducedMotion = rememberReducedMotion()
	val targetWidth = if (expanded) RailExpandedWidth else RailCollapsedWidth
	val width by animateDpAsState(targetWidth, animationSpec = if (reducedMotion) snap() else tween(180), label = "rail width")

	Column(
		verticalArrangement = Arrangement.spacedBy(6.dp),
		modifier = modifier
			.width(width)
			.fillMaxHeight()
			.background(LumaFinPalette.Surface)
			.padding(horizontal = 8.dp, vertical = 24.dp)
			// Focus arrives from the Leanback rows through the view system, which picks the nearest item;
			// entering the menu should always land on Home.
			.onFocusChanged { state ->
				if (state.hasFocus && !expanded) activeFocusRequester.requestFocus()
				expanded = state.hasFocus
			}
			.focusGroup(),
	) {
		ButtonBase(
			onClick = { showUserSwitcher = true },
			shape = RoundedCornerShape(24.dp),
			colors = ButtonDefaults.colors(containerColor = Color.Transparent, contentColor = LumaFinPalette.Text),
			modifier = toRows.height(48.dp).then(if (expanded) Modifier.fillMaxWidth() else Modifier.width(48.dp)),
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.align(Alignment.CenterStart),
			) {
				ProfilePicture(
					url = userImage,
					contentDescription = stringResource(R.string.lbl_switch_user),
					modifier = Modifier.size(48.dp).clip(CircleShape),
				)
				if (expanded) Column {
					Text(currentUser?.name.orEmpty(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
					// Which server you're on, for households with more than one
					currentServer?.name?.let { Text(it, fontSize = 12.sp, color = LumaFinPalette.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
				}
			}
		}
		Spacer(Modifier.height(18.dp))
		// Entering the menu lands on the highlighted entry; on a Libraries screen with no matching entry,
		// that's the overflow item, or the first library when there is none
		val focusTarget: Any? = when {
			active != MainToolbarActiveButton.Libraries -> active
			activeLibrary != null -> activeLibrary.id
			overflow -> MainToolbarActiveButton.Libraries
			else -> shownLibraries.firstOrNull()?.id
		}
		fun item(key: Any) = if (key == focusTarget) toRows.focusRequester(activeFocusRequester) else toRows
		RailItem(R.drawable.ic_house, stringResource(R.string.lbl_home), expanded, active = active == MainToolbarActiveButton.Home, modifier = item(MainToolbarActiveButton.Home)) {
			if (!onActiveScreen || active != MainToolbarActiveButton.Home) navigationRepository.navigate(Destinations.home, replace = true)
		}
		RailItem(R.drawable.ic_search, stringResource(R.string.lbl_search), expanded, active = active == MainToolbarActiveButton.Search, modifier = item(MainToolbarActiveButton.Search)) {
			if (!onActiveScreen || active != MainToolbarActiveButton.Search) navigationRepository.navigate(Destinations.search())
		}
		for (entry in shownLibraries) {
			RailItem(libraryIcon(entry.collectionType), entry.name.orEmpty(), expanded, active = entry == activeLibrary, modifier = item(entry.id)) {
				// Already on this library's own screen
				if (library?.id == entry.id) return@RailItem
				// Live TV goes straight to the guide; its On now and recordings screen is a button there
				if (entry.collectionType == CollectionType.LIVETV) navigationRepository.navigate(Destinations.liveTvGuide)
				else itemLauncher.launchUserView(entry)
			}
		}
		if (overflow) {
			RailItem(R.drawable.ic_folder, stringResource(R.string.home_section_library), expanded,
				active = active == MainToolbarActiveButton.Libraries && activeLibrary == null, modifier = item(MainToolbarActiveButton.Libraries)) {
				if (!onActiveScreen || active != MainToolbarActiveButton.Libraries) navigationRepository.navigate(Destinations.libraries)
			}
		}
		// Settings sits at the bottom, apart from the places you browse
		Spacer(Modifier.weight(1f))
		RailItem(R.drawable.ic_settings, stringResource(R.string.settings), expanded, modifier = toRows) { settingsViewModel.show() }
	}
}

@Composable
private fun RailItem(icon: Int, text: String, expanded: Boolean, active: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
	ButtonBase(
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		colors = ButtonDefaults.colors(
			containerColor = if (active) LumaFinPalette.Raised else Color.Transparent,
			contentColor = if (active) LumaFinPalette.Cyan else LumaFinPalette.SecondaryText,
		),
		modifier = modifier.height(48.dp).then(if (expanded) Modifier.fillMaxWidth() else Modifier.width(48.dp)),
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp),
			modifier = Modifier.align(Alignment.CenterStart).padding(start = 13.dp),
		) {
			// The icon carries the label for accessibility while the text is hidden
			Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = if (expanded) null else text, modifier = Modifier.size(22.dp))
			if (expanded) Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
		}
	}
}

/**
 * A fixed-height preview of the focused card's title. It never takes focus, so the rows below keep
 * their focus positions; the item's backdrop is drawn behind by the background service.
 */
@Composable
fun HomeFocusedPreview(
	item: BaseItemDto?,
	modifier: Modifier = Modifier,
) {
	// No buttons here: OK on a card opens its details and the remote's Play key plays it, so Up and Down only move between rows.
	Box(modifier = modifier.fillMaxWidth().height(176.dp).padding(start = 48.dp, top = 20.dp, end = 32.dp)) {
		ToolbarClock(modifier = Modifier.align(Alignment.TopEnd))
		if (item == null) return@Box
		// Anchored to the bottom so the rows keep the same distance whether or not an episode line is shown.
		Column(
			verticalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 640.dp),
		) {
			FocusedTitleInfo(item)
		}
	}
}

/** Episode line, title, info row and description of [item], shared by the Home and library previews. */
@Composable
fun FocusedTitleInfo(item: BaseItemDto) {
	val context = LocalContext.current
	val isEpisode = item.type == BaseItemKind.EPISODE
	if (isEpisode && item.name != null) {
		Text(item.name!!, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LumaFinPalette.Cyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
	}
	Text(
		text = (if (isEpisode) item.seriesName else null) ?: item.name.orEmpty(),
		style = JellyfinTheme.typography.default.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = LumaFinPalette.Text),
		maxLines = 1,
		overflow = TextOverflow.Ellipsis,
	)
	// The info row's badges fill the available height, so give it the intrinsic height of its text.
	Box(modifier = Modifier.height(IntrinsicSize.Min)) {
		BaseItemInfoRow(item = item, mediaSource = null, includeRuntime = true)
	}
	descriptionForDisplay(context, item)?.takeIf { it.isNotBlank() }?.let { description ->
		Text(description, fontSize = 15.sp, color = LumaFinPalette.SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
	}
}

/**
 * The main navigation for [active] screens: the side menu with [content] beside it when the new
 * navigation is on, otherwise the classic top toolbar above it. Content gets [contentFocusRequester]
 * so Right from the menu returns to it.
 */
@Composable
fun MainNavigationFrame(
	active: MainToolbarActiveButton,
	contentFocusRequester: FocusRequester,
	content: @Composable (sideMenu: Boolean) -> Unit,
) {
	val userPreferences = koinInject<UserPreferences>()
	val sideMenu = remember { userPreferences[UserPreferences.newHomeEnabled] }
	if (sideMenu) {
		val railFocusRequester = remember { FocusRequester() }
		Box(modifier = Modifier.fillMaxHeight()) {
			// The top padding stands in for the toolbar's height, which content was laid out below
			Box(modifier = Modifier.fillMaxHeight().padding(start = RailCollapsedWidth, top = 40.dp)) { content(true) }
			MainSideRail(active, railFocusRequester, contentFocusRequester)
		}
	} else {
		Column { MainToolbar(active); content(false) }
	}
}

@Composable
fun NewHomeLayout(rail: @Composable () -> Unit, preview: @Composable () -> Unit, rows: @Composable () -> Unit) {
	// The rows sit beside the collapsed menu; the menu expands over them so nothing moves.
	Box(modifier = Modifier.fillMaxHeight()) {
		Column(modifier = Modifier.fillMaxHeight().padding(start = RailCollapsedWidth)) {
			preview()
			rows()
		}
		rail()
	}
}

/** Whether focus is on the first card of its row, so Left may leave the rows for the side menu. */
fun RowsSupportFragment.isFirstCardFocused(): Boolean {
	val focused = view?.findFocus() ?: return false
	val row = generateSequence(focused.parent) { it.parent }.filterIsInstance<HorizontalGridView>().firstOrNull() ?: return true
	val card = generateSequence(focused as View?) { it.parent as? View }.firstOrNull { it.parent === row } ?: return true
	return row.getChildAdapterPosition(card) <= 0
}
