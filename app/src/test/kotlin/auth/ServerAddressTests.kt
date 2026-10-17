package org.jellyfin.androidtv.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import io.kotest.assertions.throwables.shouldThrow
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.repository.BackupAddressResult
import org.jellyfin.androidtv.auth.repository.ServerAddressRepository
import org.jellyfin.androidtv.auth.repository.normalizeServerAddress
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.sdk.api.client.exception.TimeoutException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class ServerAddressTests : FunSpec({
	val serverId = UUID(0, 1)
	val userId = UUID(0, 2)
	test("backup changes publish through server state flows") {
		val server = org.jellyfin.androidtv.auth.model.Server(serverId, "Server", "https://remote.example")
		val state = kotlinx.coroutines.flow.MutableStateFlow(listOf(server))
		val updated = server.copy(backupAddress = "https://home.example")
		state.value = listOf(updated)
		state.value.single().backupAddress shouldBe "https://home.example"
		state.value = listOf(updated.copy(backupAddress = null))
		state.value.single().backupAddress shouldBe null
	}
	val original = AuthenticationStoreServer("Server", "https://remote.example/jellyfin", version = "10.11.0",
		users = mapOf(userId to AuthenticationStoreUser("Saved", accessToken = "must-preserve")))

	test("addresses retain proxy paths and reject embedded credentials and token parameters") {
		normalizeServerAddress(" https://home.example/jellyfin/ ") shouldBe "https://home.example/jellyfin"
		normalizeServerAddress("HTTPS://HOME.EXAMPLE/a%2Fb/") shouldBe "https://home.example/a%2Fb"
		for (address in listOf("file:///tmp/a", "https://name:secret@home.example", "https://home.example?api_key=secret",
			"https://home.example/#token", "home.example", "http://home.example:99999", "http://home.example:0")) {
			normalizeServerAddress(address) shouldBe null
		}
	}
	test("only a matching public server identity can save a backup and saved users are preserved") {
		val store = mockk<AuthenticationStore>()
		every { store.getServer(serverId) } returns original
		every { store.putServer(serverId, any()) } returns true
		val repository = ServerAddressRepository(store) { serverId }
		repository.saveBackup(serverId, "https://home.example/") shouldBe BackupAddressResult.Saved("https://home.example")
		verify { store.putServer(serverId, original.copy(backupAddress = "https://home.example")) }
	}
	test("a different or missing identity cannot modify the saved server") {
		for (identity in listOf(UUID(0, 99), null)) {
			val store = mockk<AuthenticationStore>()
			every { store.getServer(serverId) } returns original
			val repository = ServerAddressRepository(store) { identity }
			repository.saveBackup(serverId, "https://home.example") shouldBe
				if (identity == null) BackupAddressResult.MissingServerIdentity else BackupAddressResult.DifferentServer
			verify(exactly = 0) { store.putServer(any(), any()) }
		}
	}
	test("an unreachable address leaves the current address users and backup untouched") {
		val store = mockk<AuthenticationStore>()
		every { store.getServer(serverId) } returns original
		val repository = ServerAddressRepository(store) { throw TimeoutException() }
		repository.saveBackup(serverId, "https://home.example") shouldBe BackupAddressResult.Unavailable(ConnectionFailure.UNREACHABLE)
		verify(exactly = 0) { store.putServer(any(), any()) }
	}
	test("a concurrent login is preserved and a removed server is never recreated") {
		for (latest in listOf(original.copy(users = emptyMap()), null)) {
			val store = mockk<AuthenticationStore>()
			every { store.getServer(serverId) } returnsMany listOf(original, latest)
			every { store.putServer(serverId, any()) } returns true
			val repository = ServerAddressRepository(store) { serverId }
			val result = repository.saveBackup(serverId, "https://home.example")
			if (latest == null) {
				result shouldBe BackupAddressResult.ServerNotFound
				verify(exactly = 0) { store.putServer(any(), any()) }
			} else verify { store.putServer(serverId, latest.copy(backupAddress = "https://home.example")) }
		}
	}
	test("cancelling verification cannot save an address") {
		val store = mockk<AuthenticationStore>()
		every { store.getServer(serverId) } returns original
		val repository = ServerAddressRepository(store) { throw CancellationException() }
		shouldThrow<CancellationException> { repository.saveBackup(serverId, "https://home.example") }
		verify(exactly = 0) { store.putServer(any(), any()) }
	}
	test("a saved backup is reverified rather than trusting an earlier match") {
		val store = mockk<AuthenticationStore>()
		every { store.getServer(serverId) } returns original.copy(backupAddress = "https://home.example")
		var identity = serverId
		val repository = ServerAddressRepository(store) { identity }
		repository.verifyBackup(serverId) shouldBe BackupAddressResult.Saved("https://home.example")
		identity = UUID(0, 99)
		repository.verifyBackup(serverId) shouldBe BackupAddressResult.DifferentServer
		verify(exactly = 0) { store.putServer(any(), any()) }
	}
	test("existing saved servers load with no backup and preserve positional version arguments") {
		val old = Json.decodeFromString<AuthenticationStoreServer>("""{"name":"Server","address":"https://remote.example"}""")
		old.backupAddress shouldBe null
		AuthenticationStoreServer("Server", "https://remote.example", "10.11.0").version shouldBe "10.11.0"
	}
})
