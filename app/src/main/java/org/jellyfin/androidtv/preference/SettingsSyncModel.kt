package org.jellyfin.androidtv.preference

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Settings sync (#27): which settings travel between TVs, the server envelope, and the three-way merge.
 * Values reuse the settings backup (#7) field definitions so types and ranges are validated identically.
 */
internal object SettingsSyncModel {
	private const val FORMAT = "lumafin-settings-sync"

	/** Personal preferences. Layout (Home rows/limits, card size, theme, backdrop, clock, motion, subtitle size/position) stays per TV. */
	val syncedKeys = listOf(
		"hide_unwatched_episode_artwork", "hide_unwatched_episode_descriptions",
		"home_latest_movies_hide_watched", "home_recently_added_movies_hide_watched",
		"pref_watched_indicator_behavior", "pref_rating_badges_enabled", "pref_enable_series_thumbnails",
		"card_long_press_action", "pref_enable_tv_queuing", "pref_enable_cinema_mode",
		"skipBackLength", "skipForwardLength",
		"subtitle_brightness", "subtitles_text_weight",
		"subtitles_text_color", "subtitles_background_color", "subtitles_text_stroke_color",
		"hidden_next_up",
	)

	val fields = SettingsBackup.fields.filter { it.key in syncedKeys }.also { check(it.size == syncedKeys.size) }

	data class Envelope(val revision: Int, val writer: String, val settings: Map<String, Any>)

	/** Current local values for the synced settings, validated like a backup export. */
	fun snapshot(read: (String) -> Any?): Map<String, Any> = SettingsBackup.snapshot(read).filterKeys { it in syncedKeys }

	fun encodeSettings(values: Map<String, Any>) = buildJsonObject {
		fields.forEach { field -> values[field.key]?.let { put(field.key, SettingsBackup.primitive(field.decode(SettingsBackup.primitive(it)))) } }
	}

	fun decodeSettings(settings: JsonObject): Map<String, Any> =
		fields.mapNotNull { field -> settings[field.key]?.let { field.key to field.decode(it) } }.toMap()

	fun encode(envelope: Envelope): String = Json.encodeToString(JsonObject.serializer(), buildJsonObject {
		put("format", FORMAT); put("version", 1)
		put("revision", envelope.revision); put("writer", envelope.writer)
		put("settings", encodeSettings(envelope.settings))
	})

	/** Rejects the whole copy on a malformed known value; unknown keys from newer versions are ignored. */
	fun decode(text: String): Envelope {
		SettingsBackup.checkLimits(text)
		val root = Json.parseToJsonElement(text).jsonObject
		require(root["format"] == JsonPrimitive(FORMAT) && root["version"] == JsonPrimitive(1)) { "Unsupported sync format" }
		val revision = requireNotNull(root["revision"]?.jsonPrimitive?.takeIf { !it.isString }?.intOrNull) { "Missing revision" }
		require(revision >= 0) { "Invalid revision" }
		val writer = root["writer"]?.jsonPrimitive?.takeIf { it.isString }?.content.orEmpty().take(64)
		val settings = root["settings"]?.jsonObject ?: error("Missing settings")
		return Envelope(revision, writer, decodeSettings(settings))
	}

	data class Merge(
		/** Remote values to write into this TV's preferences. */
		val toLocal: Map<String, Any>,
		/** Complete settings to store on the server, or null when the server copy is already correct. */
		val toRemote: Map<String, Any>?,
		/** Settings changed differently on both sides since the last sync: key to (this TV, synced). */
		val conflicts: Map<String, Pair<Any, Any>>,
		/** What both sides agree on after this sync; conflicting keys keep their previous base. */
		val base: Map<String, Any>,
	)

	/**
	 * Three-way merge per setting. A missing [base] (never synced) treats every difference as a conflict;
	 * a missing [remote] uploads this TV's values.
	 */
	fun merge(base: Map<String, Any>?, local: Map<String, Any>, remote: Map<String, Any>?): Merge {
		val toLocal = mutableMapOf<String, Any>()
		val merged = mutableMapOf<String, Any>()
		val conflicts = mutableMapOf<String, Pair<Any, Any>>()
		val newBase = mutableMapOf<String, Any>()
		for (key in syncedKeys) {
			val l = local[key] ?: continue
			val r = remote?.get(key)
			val b = base?.get(key)
			when {
				r == null -> { merged[key] = l; newBase[key] = l }
				l == r -> { merged[key] = l; newBase[key] = l }
				b != null && l == b -> { toLocal[key] = r; merged[key] = r; newBase[key] = r }
				b != null && r == b -> { merged[key] = l; newBase[key] = l }
				else -> {
					conflicts[key] = l to r
					merged[key] = r // leave the server's value until the viewer decides
					b?.let { newBase[key] = it }
				}
			}
		}
		val upload = remote == null || syncedKeys.any { key -> merged[key] != null && merged[key] != remote[key] }
		return Merge(toLocal, merged.takeIf { upload }, conflicts, newBase)
	}

	enum class Resolution { KEEP_THIS_TV, USE_SYNCED }

	/** Applies a conflict decision on top of [merge]: what to write locally, the server copy, and the new base. */
	fun resolve(merge: Merge, remote: Map<String, Any>, resolution: Resolution): Merge {
		if (merge.conflicts.isEmpty()) return merge
		val server = (merge.toRemote ?: remote).toMutableMap()
		val toLocal = merge.toLocal.toMutableMap()
		val base = merge.base.toMutableMap()
		merge.conflicts.forEach { (key, values) ->
			val chosen = if (resolution == Resolution.KEEP_THIS_TV) values.first else values.second
			if (resolution == Resolution.USE_SYNCED) toLocal[key] = chosen
			server[key] = chosen
			base[key] = chosen
		}
		val upload = server != remote
		return Merge(toLocal, server.takeIf { upload }, emptyMap(), base)
	}
}
