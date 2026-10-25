package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinActivityViewModel

class HomeFragment : Fragment() {
	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()
	private val userPreferences by inject<UserPreferences>()

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

		// The settings screen is a dialog overlay rather than a separate navigation destination, so
		// HomeRowsFragment never receives an onResume() call when it closes. Reload manually instead.
		val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
		val settingsVisible by settingsViewModel.visible.collectAsState()
		LaunchedEffect(settingsVisible) {
			if (!settingsVisible) {
				newHome = userPreferences[UserPreferences.newHomeEnabled]
				rowsSupportFragment.value?.reloadHomeRowsIfSectionsChanged()
			}
		}

		if (newHome) {
			val focusedItemFlow = remember { MutableStateFlow<HomeRowsFragment?>(null) }
			focusedItemFlow.value = rowsSupportFragment.value
			val focusedItem by remember { focusedItemFlow.flatMapLatest { it?.focusedItem ?: flowOf(null) } }.collectAsState(null)

			NewHomeLayout(
				rail = {
					HomeSideRail(railFocusRequester, modifier = Modifier.focusProperties { right = rowsFocusRequester })
				},
				preview = { HomeFocusedPreview(focusedItem) },
				rows = { HomeRows(rowsFocusRequester, rowsSupportFragment, exitDirection = FocusDirection.Left) },
			)
		} else {
			Column {
				MainToolbar(MainToolbarActiveButton.Home)
				HomeRows(rowsFocusRequester, rowsSupportFragment, exitDirection = FocusDirection.Up)
			}
		}
	}

	/**
	 * The Home rows. Focus may leave them in [exitDirection] only: Up from the first row to the classic
	 * toolbar, or Left from a row's first card to the new Home's side menu.
	 */
	@Composable
	private fun HomeRows(
		focusRequester: FocusRequester,
		fragment: MutableState<HomeRowsFragment?>,
		exitDirection: FocusDirection,
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
							requestedFocusDirection != exitDirection -> cancelFocusChange()
							exitDirection == FocusDirection.Up && !isFirstRowSelected -> cancelFocusChange()
							exitDirection == FocusDirection.Left && !isFirstCardSelected -> cancelFocusChange()
							exitDirection == FocusDirection.Up -> {
								fragment.value?.selectedPosition = 0
								fragment.value?.verticalGridView?.clearFocus()
							}
						}
					}
				}
				.fillMaxSize(),
			onUpdate = { rows -> fragment.value = rows }
		)
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
