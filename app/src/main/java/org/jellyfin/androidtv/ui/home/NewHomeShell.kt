package org.jellyfin.androidtv.ui.home

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
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.androidtv.util.sdk.compat.canResume
import org.jellyfin.androidtv.ui.base.Icon
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

/** Labeled side menu for the new Home (concept A). Home is the active destination. */
@Composable
fun HomeSideRail(homeFocusRequester: FocusRequester, modifier: Modifier = Modifier) {
	val navigationRepository = koinInject<NavigationRepository>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val userRepository = koinInject<UserRepository>()
	val api = koinInject<ApiClient>()
	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userImage = remember(currentUser) { currentUser?.primaryImage?.getUrl(api) }
	var showUserSwitcher by remember { mutableStateOf(false) }
	if (showUserSwitcher) QuickUserSwitcher(onDismissRequest = { showUserSwitcher = false })
	var hadFocus by remember { mutableStateOf(false) }

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(6.dp),
		modifier = modifier
			.width(96.dp)
			.fillMaxHeight()
			.background(LumaFinPalette.Surface)
			.padding(vertical = 24.dp)
			// Focus arrives from the Leanback rows through the view system, which picks the nearest item;
			// entering the menu should always land on Home.
			.onFocusChanged { state ->
				if (state.hasFocus && !hadFocus) homeFocusRequester.requestFocus()
				hadFocus = state.hasFocus
			}
			.focusGroup(),
	) {
		ButtonBase(
			onClick = { showUserSwitcher = true },
			shape = CircleShape,
			colors = ButtonDefaults.colors(containerColor = LumaFinPalette.Raised),
			modifier = Modifier.size(48.dp),
		) {
			ProfilePicture(
				url = userImage,
				contentDescription = stringResource(R.string.lbl_switch_user),
				modifier = Modifier
					.aspectRatio(1f)
					.clip(CircleShape),
			)
		}
		Spacer(Modifier.height(18.dp))
		RailItem(R.drawable.ic_house, R.string.lbl_home, active = true, modifier = Modifier.focusRequester(homeFocusRequester)) {}
		RailItem(R.drawable.ic_search, R.string.lbl_search) { navigationRepository.navigate(Destinations.search()) }
		RailItem(R.drawable.ic_folder, R.string.home_section_library) { navigationRepository.navigate(Destinations.libraries) }
		RailItem(R.drawable.ic_settings, R.string.settings) { settingsViewModel.show() }
	}
}

@Composable
private fun RailItem(icon: Int, label: Int, active: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
	ButtonBase(
		onClick = onClick,
		shape = RoundedCornerShape(14.dp),
		colors = ButtonDefaults.colors(
			containerColor = if (active) LumaFinPalette.Raised else Color.Transparent,
			contentColor = if (active) LumaFinPalette.Cyan else LumaFinPalette.SecondaryText,
		),
		modifier = modifier.width(80.dp).height(64.dp),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(4.dp),
			modifier = Modifier.align(Alignment.Center),
		) {
			Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
			Text(stringResource(label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
	buttonsFocusRequester: FocusRequester,
	rowsFocusRequester: FocusRequester,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	val navigationRepository = koinInject<NavigationRepository>()
	val playbackHelper = koinInject<PlaybackHelper>()
	val playbackLauncher = koinInject<PlaybackLauncher>()
	val userPreferences = koinInject<UserPreferences>()
	Box(modifier = modifier.fillMaxWidth().height(256.dp).padding(start = 48.dp, top = 20.dp, end = 32.dp)) {
		ToolbarClock(modifier = Modifier.align(Alignment.TopEnd))
		if (item == null) return@Box
		val isEpisode = item.type == BaseItemKind.EPISODE
		Column(
			verticalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.widthIn(max = 640.dp),
		) {
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
			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier
					.padding(top = 4.dp)
					.focusRequester(buttonsFocusRequester)
					.focusProperties { down = rowsFocusRequester }
					.focusGroup(),
			) {
				if (item.type in playableTypes) {
					val resumeMs = if (item.canResume) {
						val preroll = userPreferences[UserPreferences.resumeSubtractDuration].toIntOrNull()?.times(1000) ?: 0
						(item.userData!!.playbackPositionTicks / 10_000 - preroll).coerceAtLeast(0)
					} else 0L
					PreviewButton(
						label = if (item.canResume) stringResource(R.string.lbl_resume_from, TimeUtils.formatMillis(resumeMs)) else stringResource(R.string.lbl_play),
						icon = if (item.canResume) R.drawable.ic_resume else R.drawable.ic_play,
					) {
						playbackHelper.getItemsToPlay(context, item, resumeMs == 0L && item.type == BaseItemKind.MOVIE, false, object : Response<List<BaseItemDto>>() {
							override fun onResponse(response: List<BaseItemDto>) {
								if (response.isNotEmpty()) playbackLauncher.launch(context, response, resumeMs.toInt())
							}
						})
					}
				}
				PreviewButton(label = stringResource(R.string.new_home_details), icon = R.drawable.ic_info) {
					navigationRepository.navigate(Destinations.itemDetails(item.id))
				}
			}
		}
	}
}

private val playableTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO)

/** Matches the details page buttons: navy at rest, lighter navy with a cyan outline when focused. */
@Composable
private fun PreviewButton(label: String, icon: Int, onClick: () -> Unit) {
	val strongFocus = koinInject<UserPreferences>()[UserPreferences.strongFocus]
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val shape = RoundedCornerShape(12.dp)
	ButtonBase(
		onClick = onClick,
		shape = shape,
		interactionSource = interactionSource,
		colors = ButtonDefaults.colors(
			containerColor = LumaFinPalette.Raised,
			contentColor = LumaFinPalette.Text,
			focusedContainerColor = LumaFinPalette.FocusedRow,
			focusedContentColor = LumaFinPalette.Text,
		),
		modifier = Modifier
			.height(44.dp)
			.then(
				if (focused) Modifier.border(if (strongFocus) 3.dp else 2.dp, if (strongFocus) Color.White else LumaFinPalette.Cyan, shape)
				else Modifier
			),
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
		) {
			Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
			Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
		}
	}
}

@Composable
fun NewHomeLayout(rail: @Composable () -> Unit, preview: @Composable () -> Unit, rows: @Composable () -> Unit) {
	Row(modifier = Modifier.fillMaxHeight()) {
		rail()
		Column(modifier = Modifier.fillMaxHeight().weight(1f)) {
			preview()
			rows()
		}
	}
}
