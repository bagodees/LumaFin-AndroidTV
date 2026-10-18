package org.jellyfin.androidtv.preference

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.BackdropBehavior
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.MediaCardSize
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior

/** Portable, explicitly supported settings only. Never serialize the preference store wholesale. */
internal object SettingsBackup {
	const val MAX_BYTES = 64 * 1024
	private const val FORMAT = "lumafin-settings"
	private val json = Json { prettyPrint = true }
	data class Field(val key: String, val default: Any, val valid: (Any) -> Boolean = { true }) {
		fun decode(value: JsonElement): Any {
			val p = value as? JsonPrimitive ?: error("Invalid setting")
			val decoded: Any = when (default) {
				is Boolean -> { require(!p.isString); requireNotNull(p.booleanOrNull) }
				is Int -> { require(!p.isString); requireNotNull(p.intOrNull) }
				is Long -> { require(!p.isString); requireNotNull(p.longOrNull) }
				is Float -> { require(!p.isString); requireNotNull(p.floatOrNull).also { require(it.isFinite()) } }
				else -> { require(p.isString); p.content }
			}
			require(valid(decoded)) { "Unsupported setting value" }
			return decoded
		}
	}
	private fun enumeration(key: String, default: String, values: List<String>) = Field(key, default) { it in values }
	private fun number(key: String, default: Int, range: IntRange) = Field(key, default) { it is Int && it in range }
	val fields = listOf(
		Field("homeSectionOrder", UserSettingPreferences.defaultHomeSectionOrder.joinToString(",") { it.serializedName }) {
			val rows = (it as String).split(',')
			rows.size <= HomeSectionType.entries.size && rows.distinct().size == rows.size && rows.all { row ->
				HomeSectionType.entries.any { section -> section != HomeSectionType.NONE && section.serializedName == row }
			}
		},
		number("home_latest_movies_limit", 50, 5..50), number("home_latest_shows_limit", 50, 5..50),
		number("home_recently_added_movies_limit", 50, 5..50), number("home_recently_added_shows_limit", 50, 5..50),
		Field("home_latest_movies_hide_watched", false), Field("home_recently_added_movies_hide_watched", false),
		enumeration("app_theme", AppTheme.DARK.name, AppTheme.entries.map { it.name }),
		enumeration("backdrop_behavior", BackdropBehavior.BACKDROP_WITH_BLUR.name, BackdropBehavior.entries.map { it.name }),
		enumeration("media_card_size", MediaCardSize.DEFAULT.name, MediaCardSize.entries.map { it.name }),
		enumeration("pref_clock_behavior", ClockBehavior.ALWAYS.name, ClockBehavior.entries.map { it.name }),
		enumeration("pref_watched_indicator_behavior", WatchedIndicatorBehavior.ALWAYS.name, WatchedIndicatorBehavior.entries.map { it.name }),
		Field("reduced_motion", false), Field("hide_unwatched_episode_artwork", false), Field("hide_unwatched_episode_descriptions", false),
		Field("pref_enable_series_thumbnails", true), Field("pref_rating_badges_enabled", true),
		Field("pref_enable_tv_queuing", true), Field("pref_enable_cinema_mode", true),
		number("skipBackLength", 10000, 5000..30000), number("skipForwardLength", 30000, 5000..30000),
		number("subtitle_brightness", 100, 25..100), number("subtitles_text_weight", 400, 100..900),
		Field("subtitles_text_size", 24f) { it is Float && it in 8f..32f },
		Field("subtitles_offset_position", 0.08f) { it is Float && it in 0f..0.8f },
		Field("subtitles_background_color", 0x00FFFFFFL) { it is Long && it in 0..0xFFFFFFFFL },
		Field("subtitles_text_color", 0xFFFFFFFFL) { it is Long && it in 0..0xFFFFFFFFL },
		Field("subtitles_text_stroke_color", 0xFF000000L) { it is Long && it in 0..0xFFFFFFFFL },
	)
	data class Review(val values: Map<String, Any>, val skipped: Int)
	private fun primitive(value: Any): JsonPrimitive = when (value) {
		is Boolean -> JsonPrimitive(value)
		is Number -> JsonPrimitive(value)
		else -> JsonPrimitive(value.toString())
	}
	fun snapshot(read: (String) -> Any?): Map<String, Any> = fields.associate { field ->
		val stored = read(field.key)
		val value = if (field.key == "homeSectionOrder" && stored == "") field.default else stored ?: field.default
		// Validate exports too, so older/corrupt local values cannot create an unusable backup.
		field.key to field.decode(primitive(value))
	}
	fun encode(values: Map<String, Any>): String = json.encodeToString(JsonObject.serializer(), buildJsonObject {
		put("format", FORMAT); put("version", 1)
		put("settings", buildJsonObject { fields.forEach { field ->
			values[field.key]?.let { put(field.key, primitive(field.decode(primitive(it)))) }
		} })
	})
	fun review(text: String): Review {
		require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup too large" }
		// Bound nesting before the parser, including within unsupported fields.
		var depth = 0
		var quoted = false
		var escaped = false
		text.forEach { char ->
			if (quoted) {
				if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') quoted = false
			} else when (char) {
				'"' -> quoted = true
				'{', '[' -> { depth++; require(depth <= 8) { "Backup nesting too deep" } }
				'}', ']' -> depth--
			}
		}
		val root = Json.parseToJsonElement(text).jsonObject
		require(root["format"] == JsonPrimitive(FORMAT) && root["version"] == JsonPrimitive(1)) { "Unsupported backup format" }
		val settings = root["settings"]?.jsonObject ?: error("Missing settings")
		val values = fields.mapNotNull { field -> settings[field.key]?.let { field.key to field.decode(it) } }.toMap()
		require(values.isNotEmpty()) { "No supported settings" }
		return Review(values, settings.size - values.size)
	}
}
