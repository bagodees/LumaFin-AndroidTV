package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.model.classifyConnectionFailure
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.shared.toolbar.QuickUserSwitcher
import org.jellyfin.androidtv.ui.startup.AccountSelectionActivity
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.androidtv.util.createBundle
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ServerFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ID = "server_id"
	}

	private val startupViewModel: StartupViewModel by activityViewModel()
	private val authenticationRepository: AuthenticationRepository by inject()
	private val serverUserRepository: ServerUserRepository by inject()
	private val sessionRepository: SessionRepository by inject()
	private val markdownRenderer: MarkdownRenderer by inject()
	private var authenticationJob: Job? = null
	private var authenticating by mutableStateOf(false)
	private var selectedServer by mutableStateOf<Server?>(null)
	private var toolbarView: View? = null
	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.toUUIDOrNull()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val server = serverIdArgument?.let(startupViewModel::getServer)
		if (server == null) {
			navigateFragment<SelectServerFragment>(keepToolbar = true, keepHistory = false)
			return null
		}
		selectedServer = server
		return ComposeView(requireContext()).apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
			setContent {
				val users by startupViewModel.users.collectAsState()
				val currentServer = selectedServer ?: server
				JellyfinTheme {
					QuickUserSwitcher(
						onDismissRequest = ::cancelSelection,
						serverOverride = currentServer,
						usersOverride = users.filter { it.serverId == currentServer.id },
						onSelectUser = { selectUser(currentServer, it) },
						onAddUser = { openLogin(currentServer, null) },
						onSelectServer = { navigateFragment<SelectServerFragment>(keepToolbar = true) },
						onUserLongClick = { showUserActions(currentServer, it) },
						embedded = true,
						busyOverride = authenticating,
						notice = serverNotice(currentServer),
					)
				}
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		// The shared profile screen fills the safe area instead of sitting below
		// the legacy startup toolbar. Restore that toolbar when returning to servers.
		toolbarView = parentFragmentManager.fragments.filterIsInstance<StartupToolbarFragment>().firstOrNull()?.view
		toolbarView?.isVisible = false
		view.post { if (this.view === view) view.requestFocus() }
		val server = selectedServer ?: return
		startupViewModel.loadUsers(server)
		viewLifecycleOwner.lifecycleScope.launch {
			if (startupViewModel.updateServer(server)) selectedServer = startupViewModel.getServer(server.id) ?: server
		}
		val failedTarget = sessionRepository.lastSwitchTarget
		if (sessionRepository.currentSession.value == null && failedTarget?.serverId == server.id) {
			sessionRepository.lastSwitchError?.let { error ->
				val user = serverUserRepository.getStoredServerUsers(server).firstOrNull { it.id == failedTarget.userId }
				view.post {
					if (this.view != null && isAdded) showConnectionRecovery(classifyConnectionFailure(error),
						onRetry = user?.let { { selectUser(server, it) } },
						onReviewSignIn = user?.let { { openLogin(server, it.name) } })
				}
			}
		}
	}

	private fun selectUser(server: Server, user: User) {
		if (authenticationJob?.isActive == true) return
		if (activity is AccountSelectionActivity && sessionRepository.currentSession.value?.let {
			it.serverId == server.id && it.userId == user.id
		} == true) {
			requireActivity().finish()
			return
		}
		authenticationJob = startupViewModel.authenticate(server, user).onEach { state ->
			authenticating = state == AuthenticatingState
			when (state) {
				AuthenticatingState, AuthenticatedState -> Unit
				RequireSignInState -> openLogin(server, user.name)
				ServerUnavailableState, is ApiClientErrorLoginState -> showConnectionRecovery(
					if (state is ApiClientErrorLoginState) classifyConnectionFailure(state.error) else ConnectionFailure.UNREACHABLE,
					onRetry = { selectUser(server, user) }, onReviewSignIn = { openLogin(server, user.name) })
				is ServerVersionNotSupported -> AlertDialog.Builder(requireContext())
					.setMessage(serverNotice(state.server)).setPositiveButton(android.R.string.ok, null).show()
			}
		}.launchIn(viewLifecycleOwner.lifecycleScope)
		authenticationJob?.invokeOnCompletion { authenticating = false }
	}

	private fun openLogin(server: Server, name: String?) = navigateFragment<UserLoginFragment>(createBundle {
		putString(UserLoginFragment.ARG_SERVER_ID, server.id.toString())
		putString(UserLoginFragment.ARG_USERNAME, name)
	})

	private fun serverNotice(server: Server): String? = when {
		!server.versionSupported -> getString(R.string.server_unsupported_notification, server.version,
			ServerRepository.recommendedServerVersion.toString())
		!server.setupCompleted -> getString(R.string.server_setup_incomplete)
		else -> server.loginDisclaimer?.takeIf { it.isNotBlank() }?.let { markdownRenderer.toMarkdownSpanned(it).toString() }
	}

	private fun showUserActions(server: Server, user: User) {
		if (user !is PrivateUser || authenticating) return
		val actions = mutableListOf<Pair<String, () -> Unit>>()
		if (user.accessToken != null) actions += getString(R.string.lbl_sign_out) to {
			authenticationRepository.logout(user)
			startupViewModel.loadUsers(server)
		}
		actions += getString(R.string.lbl_remove) to {
			serverUserRepository.deleteStoredUser(user)
			startupViewModel.loadUsers(server)
		}
		AlertDialog.Builder(requireContext()).setTitle(user.name)
			.setItems(actions.map { it.first }.toTypedArray()) { _, index -> actions[index].second() }.show()
	}

	private fun cancelSelection() {
		if (parentFragmentManager.backStackEntryCount > 0) parentFragmentManager.popBackStack()
		else requireActivity().finish()
	}

	override fun onDestroyView() {
		toolbarView?.isVisible = true
		toolbarView = null
		super.onDestroyView()
	}

	private inline fun <reified F : Fragment> navigateFragment(
		args: Bundle = createBundle(), keepToolbar: Boolean = false, keepHistory: Boolean = true,
	) {
		parentFragmentManager.commit {
			if (keepToolbar) {
				replace<StartupToolbarFragment>(R.id.content_view)
				add<F>(R.id.content_view, null, args)
			} else replace<F>(R.id.content_view, args = args)
			if (keepHistory) addToBackStack(null)
		}
	}
}
