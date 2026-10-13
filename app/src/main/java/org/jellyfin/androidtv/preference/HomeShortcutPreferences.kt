package org.jellyfin.androidtv.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

@Serializable
data class HomeShortcut(val id: String, val name: String, val type: BaseItemKind)

class HomeShortcutPreferences internal constructor(private val preferences: SharedPreferences, private val servers: ServerRepository, private val users: UserRepository) {
	constructor(context: Context, servers: ServerRepository, users: UserRepository) : this(PreferenceManager.getDefaultSharedPreferences(context), servers, users)
	val scope: String?
		get() {
			val server = servers.currentServer.value?.id ?: return null
			val user = users.currentUser.value?.id ?: return null
			return "home_shortcuts_${server}_$user"
		}
	val shortcuts: List<HomeShortcut>
		get() = scope?.let { key ->
			runCatching { Json.decodeFromString<List<HomeShortcut>>(preferences.getString(key, "[]") ?: "[]") }
				.getOrDefault(emptyList())
				.filter { it.type in supportedTypes && runCatching { UUID.fromString(it.id) }.isSuccess }
				.distinctBy { it.id }
		}.orEmpty()
	fun canPin(item: BaseItemDto) = scope != null && item.type in supportedTypes
	fun isPinned(id: UUID) = shortcuts.any { it.id == id.toString() }
	fun pin(item: BaseItemDto) {
		if (!canPin(item) || isPinned(item.id)) return
		save(shortcuts + HomeShortcut(item.id.toString(), item.name.orEmpty(), requireNotNull(item.type)))
	}
	fun unpin(id: String) = save(shortcuts.filterNot { it.id == id })
	fun move(id: String, delta: Int) {
		val list = shortcuts.toMutableList()
		val index = list.indexOfFirst { it.id == id }
		val target = index + delta
		if (index < 0 || target !in list.indices) return
		list.add(target, list.removeAt(index))
		save(list)
	}
	private fun save(items: List<HomeShortcut>) {
		val key = scope ?: return
		preferences.edit().putString(key, Json.encodeToString(items)).apply()
	}
	companion object {
		private val supportedTypes = setOf(BaseItemKind.USER_VIEW, BaseItemKind.COLLECTION_FOLDER, BaseItemKind.BOX_SET, BaseItemKind.SERIES)
	}
}
