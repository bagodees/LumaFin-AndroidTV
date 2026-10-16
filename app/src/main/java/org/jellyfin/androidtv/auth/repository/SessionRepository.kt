package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.preference.AccountPreferences
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.DISABLED
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.LAST_USER
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.SPECIFIC_USER
import org.jellyfin.androidtv.util.sdk.forUser
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID

data class Session(
	val userId: UUID,
	val serverId: UUID,
	val accessToken: String,
)

data class SessionTarget(val serverId: UUID, val userId: UUID)

enum class SessionRepositoryState {
	READY,
	RESTORING_SESSION,
	SWITCHING_SESSION,
}

interface SessionRepository {
	val lastSwitchError: ApiClientException?
	val lastSwitchTarget: SessionTarget?
	val currentSession: StateFlow<Session?>
	val state: StateFlow<SessionRepositoryState>

	suspend fun restoreSession(destroyOnly: Boolean)
	suspend fun switchCurrentSession(serverId: UUID, userId: UUID): Boolean
	fun destroyCurrentSession()
}

class SessionRepositoryImpl(
	private val authenticationPreferences: AuthenticationPreferences,
	private val authenticationStore: AuthenticationStore,
	private val userApiClient: ApiClient,
	private val preferencesRepository: PreferencesRepository,
	private val defaultDeviceInfo: DeviceInfo,
	private val userRepository: UserRepository,
	private val serverRepository: ServerRepository,
	private val telemetryPreferences: TelemetryPreferences,
	private val accountPreferences: AccountPreferences,
	private val videoQueue: VideoQueueManager,
	private val clearAudioQueue: () -> Unit,
	private val validateSession: suspend (Server, Session, DeviceInfo) -> UserDto,
) : SessionRepository {
	@Volatile
	override var lastSwitchError: ApiClientException? = null
		private set
	@Volatile
	override var lastSwitchTarget: SessionTarget? = null
		private set
	private val currentSessionMutex = Mutex()
	private val _currentSession = MutableStateFlow<Session?>(null)
	override val currentSession = _currentSession.asStateFlow()
	private val _state = MutableStateFlow(SessionRepositoryState.READY)
	override val state = _state.asStateFlow()

	override suspend fun restoreSession(destroyOnly: Boolean): Unit = withContext(NonCancellable) {
		currentSessionMutex.withLock {
			lastSwitchError = null
			lastSwitchTarget = null
			Timber.i("Restoring session")

			_state.value = SessionRepositoryState.RESTORING_SESSION

			val alwaysAuthenticate = authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]
			val autoLoginBehavior = authenticationPreferences[AuthenticationPreferences.autoLoginUserBehavior]

			when {
				alwaysAuthenticate -> destroyCurrentSession()
				autoLoginBehavior == DISABLED -> destroyCurrentSession()
				autoLoginBehavior == LAST_USER && !destroyOnly -> setCurrentSession(createLastUserSession())
				autoLoginBehavior == SPECIFIC_USER && !destroyOnly -> {
					val serverId = authenticationPreferences[AuthenticationPreferences.autoLoginServerId].toUUIDOrNull()
					val userId = authenticationPreferences[AuthenticationPreferences.autoLoginUserId].toUUIDOrNull()
					if (serverId != null && userId != null) setCurrentSession(createUserSession(serverId, userId))
				}
			}

			_state.value = SessionRepositoryState.READY
		}
	}

	override suspend fun switchCurrentSession(serverId: UUID, userId: UUID): Boolean = currentSessionMutex.withLock {
		lastSwitchError = null
		lastSwitchTarget = null
		// No change in user - don't switch
		if (currentSession.value?.let { it.userId == userId && it.serverId == serverId } == true) {
			Timber.d("Current session user is the same as the requested user")
			return@withLock false
		}

		_state.value = SessionRepositoryState.SWITCHING_SESSION
		Timber.i("Switching current session to user $userId")

		val session = createUserSession(serverId, userId)
		if (session == null) {
			Timber.w("Could not switch to non-existing session for user $userId")
			_state.value = SessionRepositoryState.READY
			return@withLock false
		}

		try {
			setCurrentSession(session)
		} finally {
			_state.value = SessionRepositoryState.READY
		}
	}

	override fun destroyCurrentSession() {
		lastSwitchError = null
		lastSwitchTarget = null
		Timber.i("Destroying current session")

		clearSessionState()
		accountPreferences.select(null, null)
		userRepository.setCurrentUser(null)
		serverRepository.setCurrentServer(null)
		_currentSession.value = null
		_state.value = SessionRepositoryState.READY
	}

	private suspend fun setCurrentSession(session: Session?): Boolean {
		var server: Server? = null

		if (session != null) {
			// No change in session - don't switch
			if (currentSession.value?.let { it.userId == session.userId && it.serverId == session.serverId } == true) return true

			// Check if server version is supported
			server = serverRepository.getServer(session.serverId, true)
			if (server == null || !server.versionSupported) return false
		}

		// Update session after binding the apiclient settings
		val deviceInfo = session?.let { defaultDeviceInfo.forUser(it.userId) } ?: defaultDeviceInfo
		// Validate on an isolated client. Failed authentication must leave the active
		// account, its queues, API client and preferences untouched.
		val user = try {
			if (session != null && server != null) withContext(Dispatchers.IO) {
				validateSession(server, session, deviceInfo)
			} else null
		} catch (err: ApiClientException) {
			lastSwitchError = err
			lastSwitchTarget = session?.let { SessionTarget(it.serverId, it.userId) }
			Timber.w(err, "Could not validate requested session")
			return false
		}
		if (session != null && user?.id != session.userId) return false
		Timber.i("Updating current session. userId=${session?.userId} server=${server?.serverVersion}")

		return withContext(NonCancellable) {
			clearSessionState()
			accountPreferences.select(null, null)
			userRepository.setCurrentUser(null)
			serverRepository.setCurrentServer(null)
			userApiClient.update(baseUrl = server?.address, accessToken = session?.accessToken, deviceInfo = deviceInfo)
			if (session != null) {
				authenticationPreferences[AuthenticationPreferences.lastServerId] = session.serverId.toString()
				authenticationPreferences[AuthenticationPreferences.lastUserId] = session.userId.toString()
				accountPreferences.select(session.serverId, session.userId)
				serverRepository.setCurrentServer(server)
				userRepository.setCurrentUser(user)

				// Update crash reporting URL
				val crashReportUrl = userApiClient.clientLogApi.logFileUrl()
				telemetryPreferences[TelemetryPreferences.crashReportUrl] = crashReportUrl
				telemetryPreferences[TelemetryPreferences.crashReportToken] = session.accessToken
			} else {
				userRepository.setCurrentUser(null)
				serverRepository.setCurrentServer(null)
			}
			_currentSession.value = session
			// Display preferences are optional; a failed refresh cannot undo a validated login.
			try {
				preferencesRepository.onSessionChanged()
			} catch (err: ApiClientException) {
				Timber.w(err, "Unable to refresh display preferences after switching user")
			}

			true
		}
	}

	private fun clearSessionState() {
		videoQueue.clearVideoQueue()
		if (_currentSession.value != null) clearAudioQueue()
		preferencesRepository.clearSessionState()
	}

	private fun createLastUserSession(): Session? {
		val lastUserId = authenticationPreferences[AuthenticationPreferences.lastUserId].toUUIDOrNull()
		val lastServerId = authenticationPreferences[AuthenticationPreferences.lastServerId].toUUIDOrNull()

		return if (lastUserId != null && lastServerId != null) createUserSession(lastServerId, lastUserId)
		else null
	}

	private fun createUserSession(serverId: UUID, userId: UUID): Session? {
		val account = authenticationStore.getUser(serverId, userId)
		if (account?.accessToken == null) return null

		return Session(
			userId = userId,
			serverId = serverId,
			accessToken = account.accessToken
		)
	}

}
