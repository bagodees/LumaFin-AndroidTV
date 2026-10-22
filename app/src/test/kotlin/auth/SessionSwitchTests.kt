package org.jellyfin.androidtv.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryImpl
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.auth.repository.SessionTarget
import org.jellyfin.androidtv.auth.repository.UserRepositoryImpl
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.preference.AccountPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.operations.ClientLogApi
import org.jellyfin.sdk.api.client.exception.TimeoutException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior

class SessionSwitchTests : FunSpec({
	val server = Server(UUID(0, 1), "Server", "http://localhost:8096", "10.11.0")
	val first = UUID(0, 2)
	val second = UUID(0, 3)

	test("backgrounding retained auto-login session never invalidates Home requests") {
		val preferences = mockk<AuthenticationPreferences>()
		every { preferences[AuthenticationPreferences.alwaysAuthenticate] } returns false
		every { preferences[AuthenticationPreferences.autoLoginUserBehavior] } returns UserSelectBehavior.LAST_USER
		val sessions = SessionRepositoryImpl(preferences, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
			mockk(relaxed = true), UserRepositoryImpl(), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
			mockk(relaxed = true), {}, { _, _, _ -> error("Backgrounding must not authenticate") })
		val states = mutableListOf<SessionRepositoryState>()
		val collector = CoroutineScope(Dispatchers.Unconfined).launch(start = CoroutineStart.UNDISPATCHED) {
			sessions.state.collect { states += it }
		}
		try {
			repeat(3) { sessions.restoreSession(destroyOnly = true) }
			states shouldBe listOf(SessionRepositoryState.READY)
		} finally {
			collector.cancel()
		}
	}

	test("validation failure preserves active user preferences queues and session, then allows retry") {
		val users = UserRepositoryImpl()
		val store = mockk<AuthenticationStore>(relaxed = true)
		every { store.getUser(server.id, any()) } returns AuthenticationStoreUser("Saved", accessToken = "token")
		val servers = mockk<ServerRepository>(relaxed = true)
		coEvery { servers.getServer(server.id, true) } returns server
		val accounts = mockk<AccountPreferences>(relaxed = true)
		val video = mockk<VideoQueueManager>(relaxed = true)
		val display = mockk<PreferencesRepository>(relaxed = true)
		val api = mockk<ApiClient>(relaxed = true)
		every { api.getOrCreateApi(ClientLogApi::class, any()) } returns mockk<ClientLogApi>(relaxed = true)
		var fail = false
		var audioCleared = 0
		val sessions = SessionRepositoryImpl(mockk(relaxed = true), store, api, display,
			mockk(relaxed = true), users, servers, mockk(relaxed = true), accounts, video, { audioCleared++ },
			{ _, session, _ ->
				if (fail) throw TimeoutException()
				UserDto(id = session.userId, name = "Saved", hasPassword = false, hasConfiguredPassword = false, hasConfiguredEasyPassword = false)
			})
		sessions.switchCurrentSession(server.id, first) shouldBe true
		val originalSession = sessions.currentSession.value
		val originalUser = users.currentUser.value
		clearMocks(accounts, video, display, api, answers = false)
		fail = true
		sessions.switchCurrentSession(server.id, second) shouldBe false
		(sessions.lastSwitchError is TimeoutException) shouldBe true
		sessions.lastSwitchTarget shouldBe SessionTarget(server.id, second)
		sessions.state.value shouldBe SessionRepositoryState.READY
		sessions.currentSession.value shouldBe originalSession
		users.currentUser.value shouldBe originalUser
		audioCleared shouldBe 0
		verify(exactly = 0) { accounts.select(any(), any()) }
		verify(exactly = 0) { video.clearVideoQueue() }
		verify(exactly = 0) { display.clearSessionState() }
		verify(exactly = 0) { api.update(any(), any(), any(), any()) }
		fail = false
		coEvery { display.onSessionChanged() } throws TimeoutException()
		sessions.switchCurrentSession(server.id, second) shouldBe true
		sessions.lastSwitchError shouldBe null
		sessions.lastSwitchTarget shouldBe null
		sessions.currentSession.value?.userId shouldBe second
		users.currentUser.value?.id shouldBe second
		audioCleared shouldBe 1
		verify { accounts.select(server.id, second) }
	}

	test("a token resolving to the wrong identity cannot publish a session") {
		val store = mockk<AuthenticationStore>(relaxed = true)
		every { store.getUser(server.id, second) } returns AuthenticationStoreUser("Saved", accessToken = "token")
		val servers = mockk<ServerRepository>(relaxed = true)
		coEvery { servers.getServer(server.id, true) } returns server
		val accounts = mockk<AccountPreferences>(relaxed = true)
		val sessions = SessionRepositoryImpl(mockk(relaxed = true), store, mockk(relaxed = true), mockk(relaxed = true),
			mockk(relaxed = true), UserRepositoryImpl(), servers, mockk(relaxed = true), accounts, mockk(relaxed = true), {},
			{ _, _, _ -> UserDto(id = first, name = "Wrong user", hasPassword = false, hasConfiguredPassword = false, hasConfiguredEasyPassword = false) })
		sessions.switchCurrentSession(server.id, second) shouldBe false
		sessions.currentSession.value shouldBe null
		sessions.state.value shouldBe SessionRepositoryState.READY
		verify(exactly = 0) { accounts.select(any(), any()) }
	}
})
