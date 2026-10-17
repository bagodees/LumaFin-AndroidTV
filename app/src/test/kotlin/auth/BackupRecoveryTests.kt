package org.jellyfin.androidtv.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.BackupAddressResult
import org.jellyfin.androidtv.auth.repository.ServerAddressRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryImpl
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.auth.repository.UserRepositoryImpl
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior.LAST_USER
import org.jellyfin.androidtv.preference.AccountPreferences
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.jellyfin.sdk.api.operations.ClientLogApi
import org.jellyfin.sdk.model.api.UserDto
import java.util.UUID

class BackupRecoveryTests : FunSpec({
	for (scenario in listOf("success", "both unreachable", "wrong server", "wrong user", "cancel", "changed address",
		"backup authentication timeout", "cancel authentication", "primary still down")) {
		test("backup recovery: $scenario") {
			val serverId = UUID(0, 1)
			val userId = UUID(0, 2)
			val activeUserId = UUID(0, 3)
			val original = AuthenticationStoreServer("Server", "https://primary.example", "10.11.0",
				backupAddress = "https://backup.example", users = mapOf(userId to AuthenticationStoreUser("User", accessToken = "token")))
			var stored = original
			val store = mockk<AuthenticationStore>()
			every { store.getServer(serverId) } answers { stored }
			every { store.getUser(serverId, userId) } answers { stored.users[userId] }
			every { store.getUser(serverId, activeUserId) } returns AuthenticationStoreUser("Active", accessToken = "active-token")
			every { store.putServer(serverId, any()) } answers { stored = secondArg(); true }
			var probes = 0
			val addresses = ServerAddressRepository(store) {
				probes++
				when (scenario) {
					"both unreachable" -> throw TimeoutException()
					"wrong server" -> UUID(0, 99)
					"cancel" -> throw CancellationException()
					else -> serverId
				}
			}
			val servers = mockk<ServerRepository>(relaxed = true)
			coEvery { servers.getServer(serverId, true) } returns Server(serverId, "Server", original.address, "10.11.0")
			val accounts = mockk<AccountPreferences>(relaxed = true)
			val video = mockk<VideoQueueManager>(relaxed = true)
			val api = mockk<ApiClient>(relaxed = true)
			every { api.getOrCreateApi(ClientLogApi::class, any()) } returns mockk<ClientLogApi>(relaxed = true)
			var authenticated = false
			var restarting = false
			val preferences = mockk<AuthenticationPreferences>(relaxed = true)
			every { preferences[AuthenticationPreferences.alwaysAuthenticate] } returns false
			every { preferences[AuthenticationPreferences.autoLoginUserBehavior] } returns LAST_USER
			every { preferences[AuthenticationPreferences.lastServerId] } returns serverId.toString()
			every { preferences[AuthenticationPreferences.lastUserId] } returns userId.toString()
			val sessions = SessionRepositoryImpl(preferences, store, api, mockk(relaxed = true),
				mockk(relaxed = true), UserRepositoryImpl(), servers, mockk(relaxed = true), accounts, video, {},
				{ server, session, _ ->
					if (session.userId == userId) {
						if (restarting && server.address == original.address && scenario == "primary still down") throw TimeoutException()
						if (!restarting) {
							probes shouldBe 1
							server.address shouldBe original.backupAddress
						}
						authenticated = true
						if (scenario == "backup authentication timeout") throw TimeoutException()
						if (scenario == "cancel authentication") throw CancellationException()
						if (scenario == "changed address") stored = stored.copy(address = "https://changed.example")
					}
					UserDto(id = if (scenario == "wrong user" && session.userId == userId) UUID(0, 99) else session.userId,
						name = "User", hasPassword = false, hasConfiguredPassword = false, hasConfiguredEasyPassword = false)
				}, addresses)
			sessions.switchCurrentSession(serverId, activeUserId) shouldBe true
			val activeSession = sessions.currentSession.value
			clearMocks(accounts, video, api, answers = false)
			if (scenario in listOf("cancel", "cancel authentication")) {
				shouldThrow<CancellationException> { sessions.tryBackupAddress(serverId, userId) }
			}
			else {
				val result = sessions.tryBackupAddress(serverId, userId)
				when (scenario) {
					"success", "primary still down" -> {
						result shouldBe BackupAddressResult.Saved(original.backupAddress!!)
						stored.address shouldBe original.address
						stored.backupAddress shouldBe original.backupAddress
						stored.usingBackup shouldBe true
						stored.users shouldBe original.users
						sessions.currentSession.value?.userId shouldBe userId
						verify { accounts.select(serverId, userId) }
						sessions.destroyCurrentSession()
						restarting = true
						sessions.restoreSession(false)
						stored.address shouldBe original.address
						stored.backupAddress shouldBe original.backupAddress
						stored.usingBackup shouldBe (scenario == "primary still down")
						sessions.currentSession.value?.userId shouldBe userId
						verify { api.update(baseUrl = if (scenario == "success") original.address else original.backupAddress,
							accessToken = "token", deviceInfo = any()) }
					}
					"both unreachable" -> result shouldBe BackupAddressResult.Unavailable(ConnectionFailure.UNREACHABLE)
					"wrong server" -> result shouldBe BackupAddressResult.DifferentServer
					else -> (result is BackupAddressResult.Unavailable) shouldBe true
				}
			}
			if (scenario !in listOf("success", "primary still down")) {
				verify(exactly = 0) { store.putServer(any(), any()) }
				verify(exactly = 0) { accounts.select(any(), any()) }
				verify(exactly = 0) { video.clearVideoQueue() }
				verify(exactly = 0) { api.update(any(), any(), any(), any()) }
				sessions.currentSession.value shouldBe activeSession
			}
			if (scenario in listOf("wrong server", "both unreachable", "cancel")) authenticated shouldBe false
			sessions.state.value shouldBe SessionRepositoryState.READY
		}
	}
})
