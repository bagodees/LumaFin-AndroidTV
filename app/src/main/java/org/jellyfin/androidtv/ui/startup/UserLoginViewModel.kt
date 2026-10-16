package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.model.classifyConnectionFailure
import org.jellyfin.androidtv.auth.model.AutomaticAuthenticateMethod
import org.jellyfin.androidtv.auth.model.ConnectedQuickConnectState
import org.jellyfin.androidtv.auth.model.CredentialAuthenticateMethod
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.PendingQuickConnectState
import org.jellyfin.androidtv.auth.model.QuickConnectAuthenticateMethod
import org.jellyfin.androidtv.auth.model.QuickConnectState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.UnavailableQuickConnectState
import org.jellyfin.androidtv.auth.model.UnknownQuickConnectState
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.util.sdk.forUser
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.model.DeviceInfo
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class UserLoginViewModel(
	jellyfin: Jellyfin,
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
	private val defaultDeviceInfo: DeviceInfo,
) : ViewModel() {
	private val _loginState = MutableStateFlow<LoginState?>(null)
	val loginState = _loginState.asStateFlow()
	private var loginJob: Job? = null
	private var quickConnectJob: Job? = null
	private var quickConnectGeneration = 0

	var forcedUsername: String? = null

	private val _server = MutableStateFlow<Server?>(null)
	val server = _server.asStateFlow()

	private val quickConnectApi = jellyfin.createApi()
	private var quickConnectSecret: String? = null
	private val _quickConnectState = MutableStateFlow<QuickConnectState>(UnknownQuickConnectState)
	val quickConnectState = _quickConnectState.asStateFlow()
	fun authenticate(server: Server, user: User): Flow<LoginState> =
		authenticationRepository.authenticate(server, AutomaticAuthenticateMethod(user))

	fun login(username: String, password: String) {
		if (_loginState.value == AuthenticatingState) return
		val server = server.value ?: return
		_loginState.value = AuthenticatingState
		loginJob = authenticationRepository.authenticate(server, CredentialAuthenticateMethod(username, password)).onEach {
			_loginState.value = it
		}.launchIn(viewModelScope)
	}

	fun clearLoginState() {
		cancelCredentialLogin()
		stopQuickConnect()
		_loginState.value = null
		_quickConnectState.value = UnknownQuickConnectState
	}

	fun cancelCredentialLogin() {
		loginJob?.cancel()
		if (_loginState.value == AuthenticatingState) _loginState.value = null
	}

	fun stopQuickConnect() {
		quickConnectJob?.cancel()
		quickConnectGeneration++
		quickConnectSecret = null
	}

	/**
	 * Start a new Quick Connect flow. Does nothing when already active.
	 */
	suspend fun initiateQuickconnect() {
		// Already initialized
		if (quickConnectState.value != UnknownQuickConnectState) return

		val server = server.value ?: return
		val generation = ++quickConnectGeneration
		quickConnectJob?.cancel()
		_quickConnectState.emit(UnknownQuickConnectState)
		quickConnectSecret = null

		try {
			val response = withContext(Dispatchers.IO) {
				quickConnectApi.update(
					baseUrl = server.address,
					deviceInfo = defaultDeviceInfo.forUser(UUID.randomUUID()),
				)

				quickConnectApi.quickConnectApi.initiateQuickConnect().content
			}

			if (generation != quickConnectGeneration) return
			quickConnectSecret = response.secret
			_quickConnectState.emit(PendingQuickConnectState(response.code))
		} catch (err: ApiClientException) {
			if (generation != quickConnectGeneration) return
			Timber.e(err, "Unable to initiate QuickConnect")
			if (classifyConnectionFailure(err) in listOf(ConnectionFailure.UNREACHABLE, ConnectionFailure.SERVER,
				ConnectionFailure.SECURE_CONNECTION, ConnectionFailure.RATE_LIMIT)) _loginState.value = ApiClientErrorLoginState(err)
			_quickConnectState.emit(UnavailableQuickConnectState)
		}

		// Update every 5 seconds until QuickConnect is inactive or the view model is cancelled
		quickConnectJob = viewModelScope.launch {
			while (isActive && generation == quickConnectGeneration) {
				delay(5.seconds)
				if (!updateQuickConnectState()) break
			}
		}
	}

	/**
	 * Update the Quick Connect state.
	 *
	 * @return true when Quick Connect is active, false when inactive.
	 */
	private suspend fun updateQuickConnectState(): Boolean {
		val server = server.value ?: return false
		val secret = quickConnectSecret ?: return false

		try {
			val state = withContext(Dispatchers.IO) {
				quickConnectApi.quickConnectApi.getQuickConnectState(secret = secret).content
			}

			if (state.authenticated) {
				_quickConnectState.emit(ConnectedQuickConnectState)

				authenticationRepository.authenticate(server, QuickConnectAuthenticateMethod(state.secret)).collect {
					_loginState.emit(it)
				}

				return false
			} else {
				_quickConnectState.emit(PendingQuickConnectState(state.code))
				return true
			}
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to initiate QuickConnect")
			_loginState.value = ApiClientErrorLoginState(err)
			_quickConnectState.emit(UnavailableQuickConnectState)
			return false
		}
	}

	fun setServer(id: UUID?) {
		if (_server.value?.id != id) clearLoginState()
		_server.value = serverRepository.storedServers.value
			.firstOrNull { it.id == id }
	}
}
