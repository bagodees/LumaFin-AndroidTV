package org.jellyfin.androidtv.preference

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.context.GlobalContext
import java.time.Instant
import java.util.UUID

/** A show hidden from Next Up (#97): its name for the settings list and when it was hidden. */
@Serializable
data class HiddenNextUpShow(val name: String, val hiddenAt: Long)

/**
 * Shows hidden from Next Up, per user. Stored as one user preference so it travels with settings
 * backup and sync. Watching a hidden show doesn't unhide it; the settings list points it out instead.
 */
object HiddenNextUp {
	private val json = Json { ignoreUnknownKeys = true }

	fun decode(value: String): Map<String, HiddenNextUpShow> =
		runCatching { json.decodeFromString<Map<String, HiddenNextUpShow>>(value) }.getOrDefault(emptyMap())
			.filterKeys { runCatching { UUID.fromString(it) }.isSuccess }

	fun encode(shows: Map<String, HiddenNextUpShow>): String = json.encodeToString(shows)

	/** Whether [value] is a valid stored list, for settings backup and sync. */
	fun isValid(value: Any?): Boolean = value is String && runCatching { json.decodeFromString<Map<String, HiddenNextUpShow>>(value) }.isSuccess

	private val preferences get() = GlobalContext.get().get<UserPreferences>()

	val shows: Map<String, HiddenNextUpShow> get() = decode(preferences[UserPreferences.hiddenNextUp])

	fun isHidden(seriesId: UUID?) = seriesId != null && seriesId.toString() in shows

	fun hide(seriesId: UUID, name: String) {
		preferences[UserPreferences.hiddenNextUp] = encode(shows + (seriesId.toString() to HiddenNextUpShow(name, Instant.now().toEpochMilli())))
	}

	fun unhide(seriesId: String) {
		preferences[UserPreferences.hiddenNextUp] = encode(shows - seriesId)
	}

	/** Next Up episodes without the hidden shows'. */
	fun filter(items: List<BaseItemDto>): List<BaseItemDto> {
		val hidden = shows.keys
		if (hidden.isEmpty()) return items
		return items.filterNot { it.seriesId?.toString() in hidden }
	}
}
