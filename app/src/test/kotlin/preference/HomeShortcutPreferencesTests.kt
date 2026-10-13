package org.jellyfin.androidtv.preference

import android.content.SharedPreferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserDto
import java.util.UUID

class HomeShortcutPreferencesTests : FunSpec({
	fun storage(values: MutableMap<String, String>): SharedPreferences {
		val preferences = mockk<SharedPreferences>()
		val editor = mockk<SharedPreferences.Editor>()
		every { preferences.getString(any(), any()) } answers { values[firstArg()] ?: secondArg() }
		every { preferences.edit() } returns editor
		every { editor.putString(any(), any()) } answers { values[firstArg()] = secondArg(); editor }
		every { editor.apply() } returns Unit
		return preferences
	}
	fun server() = mockk<Server>().also { every { it.id } returns UUID.randomUUID() }
	fun user() = mockk<UserDto>().also { every { it.id } returns UUID.randomUUID() }
	fun item(type: BaseItemKind) = BaseItemDto(id = UUID.randomUUID(), name = type.name, type = type)

	test("pins are isolated between Jellyfin users and servers and persist across recreation") {
		val values = mutableMapOf<String, String>()
		val shared = storage(values)
		val servers = mockk<ServerRepository>()
		val users = mockk<UserRepository>()
		val serverA = server()
		val serverB = server()
		val userA = user()
		val userB = user()
		val activeServer = MutableStateFlow<Server?>(serverA)
		val activeUser = MutableStateFlow<UserDto?>(userA)
		every { servers.currentServer } returns activeServer
		every { users.currentUser } returns activeUser
		val prefs = HomeShortcutPreferences(shared, servers, users)
		val show = item(BaseItemKind.SERIES)
		prefs.pin(show)
		activeUser.value = userB
		prefs.shortcuts shouldBe emptyList()
		activeUser.value = userA
		activeServer.value = serverB
		prefs.shortcuts shouldBe emptyList()
		activeServer.value = serverA
		HomeShortcutPreferences(shared, servers, users).shortcuts.map { it.id } shouldBe listOf(show.id.toString())
		activeUser.value = null
		prefs.pin(item(BaseItemKind.BOX_SET))
		prefs.shortcuts shouldBe emptyList()
		activeUser.value = userA
		prefs.shortcuts.size shouldBe 1
	}

	test("duplicate pins, ordering boundaries and unpinning preserve other shortcuts") {
		val servers = mockk<ServerRepository>()
		val users = mockk<UserRepository>()
		every { servers.currentServer } returns MutableStateFlow(server())
		every { users.currentUser } returns MutableStateFlow(user())
		val prefs = HomeShortcutPreferences(storage(mutableMapOf()), servers, users)
		val library = item(BaseItemKind.COLLECTION_FOLDER)
		val collection = item(BaseItemKind.BOX_SET)
		val show = item(BaseItemKind.SERIES)
		listOf(library, collection, show, library, item(BaseItemKind.MOVIE)).forEach(prefs::pin)
		prefs.shortcuts.map { it.id } shouldBe listOf(library, collection, show).map { it.id.toString() }
		prefs.move(library.id.toString(), -1)
		prefs.move(show.id.toString(), 1)
		prefs.move(show.id.toString(), -1)
		prefs.unpin(collection.id.toString())
		prefs.shortcuts.map { it.id } shouldBe listOf(library, show).map { it.id.toString() }
	}

	test("malformed storage and invalid targets cannot break Home") {
		val servers = mockk<ServerRepository>()
		val users = mockk<UserRepository>()
		every { servers.currentServer } returns MutableStateFlow(server())
		every { users.currentUser } returns MutableStateFlow(user())
		val values = mutableMapOf<String, String>()
		val prefs = HomeShortcutPreferences(storage(values), servers, users)
		values[requireNotNull(prefs.scope)] = "not JSON"
		prefs.shortcuts shouldBe emptyList()
		values[requireNotNull(prefs.scope)] = """[{"id":"invalid","name":"Gone","type":"Series"}]"""
		prefs.shortcuts shouldBe emptyList()
	}
})
