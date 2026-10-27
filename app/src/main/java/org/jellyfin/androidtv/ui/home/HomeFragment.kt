package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.HomeStyle
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinActivityViewModel

/** Height of the focused title's details in the top bar style, between the bar and the rows. */
private val TopBarInfoHeight = 216.dp

class HomeFragment : Fragment() {
	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()
	private val userPreferences by inject<UserPreferences>()
	private val resumeCount = MutableStateFlow(0)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val rowsFocusRequester = remember { FocusRequester() }
		val railFocusRequester = remember { FocusRequester() }
		LaunchedEffect(rowsFocusRequester) { rowsFocusRequester.requestFocus() }

		// The leanback code has its own awful focus handling that doesn't work properly with Compose view inteop to workaround this
		// issue we add custom behavior that only allows focus exit when the current selected row is the first one. Additionally when
		// we do switch the focus, we reset the leanback state so it won't cause weird behavior when focus is regained
		val rowsSupportFragment = remember { mutableStateOf<HomeRowsFragment?>(null) }
		var newHome by remember { mutableStateOf(userPreferences[UserPreferences.newHomeEnabled]) }
		var homeStyle by remember { mutableStateOf(userPreferences[UserPreferences.homeStyle]) }
		// Coming back from details, search or playback, the view system can restore focus to a menu
		// item; on the new Home return to the rows (the last focused card) instead.
		val resumes by resumeCount.collectAsState()
		LaunchedEffect(resumes) {
			if (newHome && resumes > 1) runCatching { rowsFocusRequester.requestFocus() }
		}

		// The settings screen is a dialog overlay rather than a separate navigation destination, so
		// HomeRowsFragment never receives an onResume() call when it closes. Reload manually instead.
		val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
		val settingsVisible by settingsViewModel.visible.collectAsState()
		LaunchedEffect(settingsVisible) {
			if (!settingsVisible) {
				newHome = userPreferences[UserPreferences.newHomeEnabled]
				homeStyle = userPreferences[UserPreferences.homeStyle]
				rowsSupportFragment.value?.reloadHomeRowsIfSectionsChanged()
			}
		}

		if (newHome) {
			val focusedItemFlow = remember { MutableStateFlow<HomeRowsFragment?>(null) }
			focusedItemFlow.value = rowsSupportFragment.value
			val focusedItem by remember { focusedItemFlow.flatMapLatest { it?.focusedItem ?: flowOf(null) } }.collectAsState(null)
			val rowsTopOffset = with(LocalDensity.current) { 40.dp.roundToPx() }

			if (homeStyle == HomeStyle.TOP_BAR) Column(modifier = Modifier.fillMaxSize()) {
				HomeTopBar(railFocusRequester, rowsFocusRequester)
				// A tall info block leaves the focused title's backdrop showing above it
				HomeBackdropInfo(focusedItem, Modifier.height(TopBarInfoHeight))
				HomeRows(rowsFocusRequester, rowsSupportFragment, exitDirections = setOf(FocusDirection.Up), resetOnUp = false, topOffsetPx = rowsTopOffset, rowSpacingDp = 12)
			} else NewHomeLayout(
				rail = {
					MainSideRail(MainToolbarActiveButton.Home, railFocusRequester, rowsFocusRequester)
				},
				preview = { HomeFocusedPreview(focusedItem) },
				rows = {
					// The rows place the focused row by a share of their height; in this shorter area that
					// clips the first row's heading. Align the focused row's cards just below its heading
					// instead, with a small positive gap between rows so the row above scrolls fully out of
					// view behind the preview.
					HomeRows(rowsFocusRequester, rowsSupportFragment, exitDirections = setOf(FocusDirection.Left), resetOnUp = false, topOffsetPx = rowsTopOffset, rowSpacingDp = 12)
				},
			)
		} else {
			Column {
				MainToolbar(MainToolbarActiveButton.Home)
				HomeRows(rowsFocusRequester, rowsSupportFragment, exitDirections = setOf(FocusDirection.Up))
			}
		}
	}

	/**
	 * The Home rows. Focus may leave them only in [exitDirections]: Up from the first row (to the classic
	 * toolbar), and Left from a row's first card (to the side menu).
	 */
	@Composable
	private fun HomeRows(
		focusRequester: FocusRequester,
		fragment: MutableState<HomeRowsFragment?>,
		exitDirections: Set<FocusDirection>,
		resetOnUp: Boolean = true,
		topOffsetPx: Int? = null,
		rowSpacingDp: Int = -20,
	) {
		AndroidFragment<HomeRowsFragment>(
			modifier = Modifier
				.focusGroup()
				.focusRequester(focusRequester)
				.focusProperties {
					onExit = {
						val isFirstRowSelected = fragment.value?.selectedPosition?.let { it <= 0 } ?: false
						val isFirstCardSelected = fragment.value?.focusedItemIndex?.let { it <= 0 } ?: false
						when {
							requestedFocusDirection !in exitDirections -> cancelFocusChange()
							requestedFocusDirection == FocusDirection.Up && !isFirstRowSelected -> cancelFocusChange()
							requestedFocusDirection == FocusDirection.Left && !isFirstCardSelected -> cancelFocusChange()
							requestedFocusDirection == FocusDirection.Up && resetOnUp -> {
								fragment.value?.selectedPosition = 0
								fragment.value?.verticalGridView?.clearFocus()
							}
						}
					}
				}
				.fillMaxSize(),
			onUpdate = { rows ->
				fragment.value = rows
				topOffsetPx?.let(rows::setAlignment)
				rows.rowSpacingDp = rowSpacingDp
			}
		)
	}

	override fun onResume() {
		super.onResume()
		resumeCount.value++
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setBackgroundResource(R.drawable.details_readability_gradient)

		sessionRepository.currentSession
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.map { session ->
				if (session == null) null
				else serverRepository.getServer(session.serverId)
			}
			.onEach { server ->
				notificationRepository.updateServerNotifications(server)
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}
}
