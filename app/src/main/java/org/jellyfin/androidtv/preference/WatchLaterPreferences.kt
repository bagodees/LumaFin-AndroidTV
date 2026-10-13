package org.jellyfin.androidtv.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class WatchLaterPreferences internal constructor(private val preferences: SharedPreferences, private val servers: ServerRepository, private val users: UserRepository) {
	constructor(context: Context, servers: ServerRepository, users: UserRepository) : this(PreferenceManager.getDefaultSharedPreferences(context), servers, users)
	val scope: String?
		get() {
			val server = servers.currentServer.value?.id ?: return null
			val user = users.currentUser.value?.id ?: return null
			return "watch_later_${server}_$user"
		}
	val items: List<HomeShortcut>
		get() = scope?.let { key ->
			runCatching { Json.decodeFromString<List<HomeShortcut>>(preferences.getString(key, "[]") ?: "[]") }
				.getOrDefault(emptyList())
				.filter { it.type in supportedTypes && runCatching { UUID.fromString(it.id) }.isSuccess }
				.distinctBy { it.id }
		}.orEmpty()
	fun canSave(item: BaseItemDto) = scope != null && item.type in supportedTypes
	fun contains(id: UUID) = items.any { it.id == id.toString() }
    val initialized: Boolean get() = scope?.let { preferences.getString(it + "_initialized", null) != null } ?: false
    fun markInitialized() { scope?.let { preferences.edit().putString(it + "_initialized", "true").apply() } }
	fun add(item: BaseItemDto) {
		if (!canSave(item) || contains(item.id)) return
		save(items + HomeShortcut(item.id.toString(), item.name.orEmpty(), requireNotNull(item.type)))
	}
	fun remove(id: String) = save(items.filterNot { it.id == id })
	fun move(id: String, delta: Int) {
		val list = items.toMutableList()
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
		private val supportedTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
	}
}
