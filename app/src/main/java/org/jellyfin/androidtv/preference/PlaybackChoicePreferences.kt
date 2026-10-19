package org.jellyfin.androidtv.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceKeys
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceMatcher
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceScope
import org.jellyfin.androidtv.ui.playback.PlaybackChoices
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream

/**
 * Automatically remembered audio, subtitle and version choices (#3), kept per Jellyfin server and user.
 * Stores only content IDs and track descriptions; nothing is sent to the server.
 */
class PlaybackChoicePreferences internal constructor(
	private val preferences: SharedPreferences,
	private val servers: ServerRepository,
	private val users: UserRepository,
) {
	constructor(context: Context, servers: ServerRepository, users: UserRepository) :
		this(PreferenceManager.getDefaultSharedPreferences(context), servers, users)

	private val json = Json { ignoreUnknownKeys = true }

	private val key: String?
		get() {
			val server = servers.currentServer.value?.id ?: return null
			val user = users.currentUser.value?.id ?: return null
			return "playback_choices_${server}_$user"
		}

	private fun load(key: String): LinkedHashMap<String, PlaybackChoices> = runCatching {
		LinkedHashMap(json.decodeFromString<Map<String, PlaybackChoices>>(preferences.getString(key, null) ?: "{}"))
	}.getOrDefault(LinkedHashMap())

	private fun save(key: String, values: LinkedHashMap<String, PlaybackChoices>) {
		values.entries.removeAll { it.value.isEmpty }
		// Keep the most recently changed titles; older entries simply fall back to normal defaults.
		while (values.size > MAX_ENTRIES) values.remove(values.keys.first())
		preferences.edit().putString(key, json.encodeToString<Map<String, PlaybackChoices>>(values)).apply()
	}

	/** Remembered choices for [item], most specific level first. */
	fun levels(item: BaseItemDto?): List<PlaybackChoices> {
		if (!PlaybackChoiceKeys.supports(item)) return emptyList()
		val values = key?.let(::load) ?: return emptyList()
		return PlaybackChoiceKeys.forItem(item!!).mapNotNull { (_, levelKey) -> values[levelKey] }
	}

	/** The level new choices for [item] are saved to: the most specific one already in use, else the default. */
	fun scope(item: BaseItemDto): PlaybackChoiceScope? {
		if (!PlaybackChoiceKeys.supports(item)) return null
		val values = key?.let(::load) ?: return null
		val levels = PlaybackChoiceKeys.forItem(item)
		return levels.firstOrNull { (_, levelKey) -> values.containsKey(levelKey) }?.first ?: levels.last().first
	}

	/** Whether anything is remembered for [item] at any level. */
	fun hasChoices(item: BaseItemDto?) = levels(item).isNotEmpty()

	private fun record(item: BaseItemDto?, update: (PlaybackChoices) -> PlaybackChoices) {
		if (item == null) return
		val storeKey = key ?: return
		val scope = scope(item) ?: return
		val levelKey = PlaybackChoiceKeys.forItem(item).first { it.first == scope }.second
		val values = load(storeKey)
		val updated = update(values.remove(levelKey) ?: PlaybackChoices())
		values[levelKey] = updated
		save(storeKey, values)
	}

	fun recordAudio(item: BaseItemDto?, stream: MediaStream?) {
		val choice = stream?.let(PlaybackChoiceMatcher::audioChoice) ?: return
		record(item) { it.copy(audio = choice) }
	}

	/** A null [stream] remembers subtitles as off. */
	fun recordSubtitle(item: BaseItemDto?, stream: MediaStream?) {
		val choice = PlaybackChoiceMatcher.subtitleChoice(stream) ?: return
		record(item) { it.copy(subtitle = choice) }
	}

	fun recordVersion(item: BaseItemDto?, source: MediaSourceInfo?) {
		if ((item?.mediaSources?.size ?: 0) < 2) return
		val choice = source?.let(PlaybackChoiceMatcher::versionChoice) ?: return
		record(item) { it.copy(version = choice) }
	}

	/**
	 * Moves what currently applies to [item] to [scope] only, matching "Remember these choices for".
	 * The other levels for this title are removed, so e.g. "This episode only" stops the series using it.
	 */
	fun moveTo(item: BaseItemDto, scope: PlaybackChoiceScope) {
		val storeKey = key ?: return
		val levels = PlaybackChoiceKeys.forItem(item)
		val target = levels.firstOrNull { it.first == scope } ?: return
		val values = load(storeKey)
		val merged = levels.mapNotNull { values[it.second] }.reduceOrNull { specific, broader -> specific over broader } ?: PlaybackChoices()
		levels.forEach { values.remove(it.second) }
		values[target.second] = merged
		save(storeKey, values)
	}

	/** Forgets every level that applies to [item], including its season and series. */
	fun reset(item: BaseItemDto) {
		val storeKey = key ?: return
		val values = load(storeKey)
		PlaybackChoiceKeys.forItem(item).forEach { values.remove(it.second) }
		save(storeKey, values)
	}

	/** Forgets all remembered choices for the signed-in account. */
	fun clearAll() {
		val storeKey = key ?: return
		preferences.edit().remove(storeKey).apply()
	}

	fun isEmpty() = key?.let { load(it).isEmpty() } ?: true

	companion object {
		internal const val MAX_ENTRIES = 1000
	}
}
