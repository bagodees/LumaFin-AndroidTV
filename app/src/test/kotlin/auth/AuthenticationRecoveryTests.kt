package org.jellyfin.androidtv.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.auth.model.AutomaticAuthenticateMethod
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.AuthenticationRepositoryImpl
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.jellyfin.sdk.model.api.UserDto
import java.util.UUID

class AuthenticationRecoveryTests : FunSpec({
	val server = Server(UUID(0, 1), "Server", "http://localhost:8096", "10.11.0")
	val user = PrivateUser(UUID(0, 2), server.id, "Saved", "token", null, 0)
	test("saved login preserves timeout and permission errors instead of asking for credentials") {
		for (error in listOf(TimeoutException(), InvalidStatusException(401), InvalidStatusException(403))) {
			val sessions = mockk<SessionRepository>()
			coEvery { sessions.switchCurrentSession(server.id, user.id) } returns false
			every { sessions.lastSwitchError } returns error
			val preferences = mockk<AuthenticationPreferences>()
			every { preferences[AuthenticationPreferences.alwaysAuthenticate] } returns false
			val store = mockk<AuthenticationStore>()
			every { store.getUser(server.id, user.id) } returns AuthenticationStoreUser("Saved", accessToken = "token")
			val repository = AuthenticationRepositoryImpl(mockk(), sessions, store, mockk(), preferences, mockk())
			val result = repository.authenticate(server, AutomaticAuthenticateMethod(user)).toList().last()
			(result as ApiClientErrorLoginState).error shouldBe error
		}
	}
	test("successful preflight uses its validated user without a second network authentication request") {
		val sessions = mockk<SessionRepository>()
		coEvery { sessions.switchCurrentSession(server.id, user.id) } returns true
		val preferences = mockk<AuthenticationPreferences>()
		every { preferences[AuthenticationPreferences.alwaysAuthenticate] } returns false
		val store = mockk<AuthenticationStore>(relaxed = true)
		every { store.getUser(server.id, user.id) } returns AuthenticationStoreUser("Saved", accessToken = "token")
		every { store.getServer(server.id) } returns null
		val users = mockk<UserRepository>()
		every { users.currentUser } returns MutableStateFlow(UserDto(id = user.id, name = "Saved", hasPassword = true,
			hasConfiguredPassword = true, hasConfiguredEasyPassword = false))
		val jellyfin = mockk<Jellyfin>()
		val repository = AuthenticationRepositoryImpl(jellyfin, sessions, store, users, preferences, mockk())
		repository.authenticate(server, AutomaticAuthenticateMethod(user)).toList().last() shouldBe AuthenticatedState
		verify { jellyfin wasNot Called }
	}
})
