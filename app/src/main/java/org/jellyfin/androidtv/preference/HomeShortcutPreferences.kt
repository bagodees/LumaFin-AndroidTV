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

/**
 * A pinned Home shortcut. Genres and studios (#96) belong to a library: [libraryId] is that library,
 * [entryId] the genre or studio and [filterName] its own name; [id] combines library and entry so the
 * same genre can be pinned for two libraries. [favorites] pins a library's favorites (#96).
 */
@Serializable
data class HomeShortcut(
	val id: String,
	val name: String,
	val type: BaseItemKind,
	val libraryId: String? = null,
	val entryId: String? = null,
	val filterName: String? = null,
	val favorites: Boolean = false,
)

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
	fun canPin(item: BaseItemDto) = scope != null && item.type in itemTypes
	fun isPinned(id: UUID) = shortcuts.any { it.id == id.toString() }
	fun pin(item: BaseItemDto) {
		if (!canPin(item) || isPinned(item.id)) return
		save(shortcuts + HomeShortcut(item.id.toString(), item.name.orEmpty(), requireNotNull(item.type)))
	}
	fun unpin(id: String) = save(shortcuts.filterNot { it.id == id })

	/** A Movies or TV [library]'s favorites, opening that library with only favorites (#96). */
	fun favoritesId(library: BaseItemDto): UUID = UUID.nameUUIDFromBytes("${library.id}|favorites".toByteArray())
	fun pinFavorites(library: BaseItemDto, name: String) {
		val id = favoritesId(library)
		if (scope == null || isPinned(id)) return
		save(shortcuts + HomeShortcut(id.toString(), name, BaseItemKind.COLLECTION_FOLDER, library.id.toString(), favorites = true))
	}

	/** A genre or studio [entry] of [library], opening that library limited to it (#96). */
	fun filterId(library: BaseItemDto, entry: BaseItemDto): UUID = UUID.nameUUIDFromBytes("${library.id}|${entry.id}".toByteArray())
	fun pinFilter(library: BaseItemDto, entry: BaseItemDto) {
		val id = filterId(library, entry)
		if (scope == null || entry.type !in filterTypes || isPinned(id)) return
		save(shortcuts + HomeShortcut(id.toString(), "${entry.name.orEmpty()} · ${library.name.orEmpty()}", requireNotNull(entry.type),
			library.id.toString(), entry.id.toString(), entry.name.orEmpty()))
	}
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
		/** Items pinned from their own card menu. */
		private val itemTypes = setOf(BaseItemKind.USER_VIEW, BaseItemKind.COLLECTION_FOLDER, BaseItemKind.BOX_SET, BaseItemKind.SERIES, BaseItemKind.PLAYLIST)
		/** Pinned from a library's Genres or Studios tab, together with that library. */
		private val filterTypes = setOf(BaseItemKind.GENRE, BaseItemKind.STUDIO)
		private val supportedTypes = itemTypes + filterTypes
	}
}
