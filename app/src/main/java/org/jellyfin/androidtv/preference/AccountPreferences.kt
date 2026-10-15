package org.jellyfin.androidtv.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

/** One active local preference file per Jellyfin server/user pair. No credentials are stored here. */
class AccountPreferences internal constructor(
	private val legacy: SharedPreferences,
	private val open: (String) -> SharedPreferences,
	private val initialize: (SharedPreferences) -> Unit = {},
) {
	constructor(context: Context) : this(
		PreferenceManager.getDefaultSharedPreferences(context).also {
			// Upgrade the legacy format before claiming its user settings.
			UserPreferences(context)
			UserSettingPreferences(context).activeHomesections
		},
		{ context.getSharedPreferences(it, Context.MODE_PRIVATE) },
		{ UserPreferences(context, it) },
	)

	internal val preferences = SwitchingAccountPreferences(legacy, open("account_guest"), deviceKeys)
	private var identity: String? = null

	@Synchronized
	fun select(serverId: UUID?, userId: UUID?) {
		val next = if (serverId != null && userId != null) "${serverId}_$userId" else null
		if (identity == next) return
		val target = open(next?.let { "account_$it" } ?: "account_guest")
		if (next != null) {
			// Claim legacy settings exactly once. Other accounts start with defaults.
			val owner = legacy.getString(MIGRATION_OWNER, null)
			if (owner == null) {
				check(legacy.edit().putString(MIGRATION_OWNER, next).commit()) { "Unable to claim legacy preferences" }
			}
			if ((owner == null || owner == next) && !target.getBoolean(MIGRATED, false)) {
				val editor = target.edit()
				legacy.all.filterKeys { it in migrationKeys && it !in deviceKeys }.forEach { (key, value) ->
					editor.putValue(key, value)
				}
				check(editor.putBoolean(MIGRATED, true).commit()) { "Unable to migrate account preferences" }
			}
			initialize(target)
		}
		identity = next
		preferences.select(target)
	}

	companion object {
		private const val MIGRATION_OWNER = "account_preferences_legacy_owner_v1"
		private const val MIGRATED = "account_preferences_migrated_v1"
		// Device/decoder capability overrides and diagnostics describe this TV, not a Jellyfin user.
		internal val deviceKeys = setOf(
			"bitstream_ac3_mode", "bitstream_eac3_mode", "bitstream_dts_mode", "bitstream_truehd_mode",
			"hdr10_override", "hdr10_plus_override", "dovi_profile_5_override", "dovi_profile_7_override", "dovi_profile_8_override",
			"user_avc_level", "user_hevc_level", "software_codecs_enabled", "exoplayer_prefer_ffmpeg",
			"libass_enabled", "pgs_enabled", "playback_new", "pref_enable_debug", "refresh_rate_switching_behavior", "video_start_delay", "buffer_length",
		)
		internal val migrationKeys: Set<String> = setOf(
			"app_theme",
			"audio_behavior",
			"audio_night_mode",
			"backdrop_behavior",
			"bitstream_ac3_mode",
			"bitstream_dts_mode",
			"bitstream_eac3_mode",
			"bitstream_truehd_mode",
			"buffer_length",
			"dovi_profile_5_override",
			"dovi_profile_7_override",
			"dovi_profile_8_override",
			"enable_still_watching",
			"exoplayer_prefer_ffmpeg",
			"external_player",
			"external_player_component",
			"hdr10_override",
			"hdr10_plus_override",
			"hide_unwatched_episode_artwork",
			"hide_unwatched_episode_descriptions",
			"home_latest_movies_hide_watched",
			"home_latest_movies_limit",
			"home_latest_shows_limit",
			"home_recently_added_movies_hide_watched",
			"home_recently_added_movies_limit",
			"home_recently_added_shows_limit",
			"homesection0",
			"homesection1",
			"homesection10",
			"homesection11",
			"homesection12",
			"homesection13",
			"homesection14",
			"homesection15",
			"homesection2",
			"homesection3",
			"homesection4",
			"homesection5",
			"homesection6",
			"homesection7",
			"homesection8",
			"homesection9",
			"homeSectionOrder",
			"libass_enabled",
			"media_card_size",
			"media_segment_actions",
			"next_up_behavior",
			"next_up_timeout",
			"pgs_enabled",
			"photo_player_presentation_delay",
			"playback_new",
			"player_zoom_mode",
			"pref_clock_behavior",
			"pref_enable_cinema_mode",
			"pref_enable_debug",
			"pref_enable_series_thumbnails",
			"pref_enable_tv_queuing",
			"pref_live_direct",
			"pref_max_bitrate",
			"pref_rating_badges_enabled",
			"pref_resume_preroll",
			"pref_watched_indicator_behavior",
			"reduced_motion",
			"refresh_rate_switching_behavior",
			"screensaver_agerating_max",
			"screensaver_agerating_required",
			"screensaver_inapp_enabled",
			"screensaver_inapp_timeout",
			"skipBackLength",
			"skipForwardLength",
			"software_codecs_enabled",
			"store_version",
			"streaming_quality",
			"subtitle_brightness",
			"subtitles_background_color",
			"subtitles_burn_during_transcode",
			"subtitles_offset_position",
			"subtitles_text_color",
			"subtitles_text_size",
			"subtitles_text_stroke_color",
			"subtitles_text_weight",
			"user_avc_level",
			"user_hevc_level",
			"video_start_delay",
		)
	}
}

private fun SharedPreferences.Editor.putValue(key: String, value: Any?) {
	when (value) {
		is String -> putString(key, value)
		is Int -> putInt(key, value)
		is Long -> putLong(key, value)
		is Float -> putFloat(key, value)
		is Boolean -> putBoolean(key, value)
		is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
	}
}

/** Editors capture their account at creation, so a delayed apply cannot write into the next user. */
internal class SwitchingAccountPreferences(
	private val device: SharedPreferences,
	initial: SharedPreferences,
	private val deviceKeys: Set<String>,
) : SharedPreferences {
	var revision by mutableIntStateOf(0)
		private set
	@Volatile private var account = initial
	private val listeners = CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()
	private val accountListener = SharedPreferences.OnSharedPreferenceChangeListener { source, key ->
		if (source === account) notifyChanged(key)
	}
	private val deviceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if (key == null || key in deviceKeys) notifyChanged(key)
	}
	init {
		account.registerOnSharedPreferenceChangeListener(accountListener)
		device.registerOnSharedPreferenceChangeListener(deviceListener)
	}
	@Synchronized fun select(next: SharedPreferences) {
		if (account === next) return
		account.unregisterOnSharedPreferenceChangeListener(accountListener)
		account = next
		revision++
		next.registerOnSharedPreferenceChangeListener(accountListener)
		notifyChanged(null)
	}
	private fun notifyChanged(key: String?) = listeners.forEach { it.onSharedPreferenceChanged(this, key) }
	private fun source(key: String?) = if (key in deviceKeys) device else account
	override fun getAll(): Map<String, *> = account.all + device.all.filterKeys { it in deviceKeys }
	override fun contains(key: String?) = source(key).contains(key)
	override fun getString(key: String?, defValue: String?) = source(key).getString(key, defValue)
	override fun getStringSet(key: String?, defValues: Set<String>?) = source(key).getStringSet(key, defValues)
	override fun getInt(key: String?, defValue: Int) = source(key).getInt(key, defValue)
	override fun getLong(key: String?, defValue: Long) = source(key).getLong(key, defValue)
	override fun getFloat(key: String?, defValue: Float) = source(key).getFloat(key, defValue)
	override fun getBoolean(key: String?, defValue: Boolean) = source(key).getBoolean(key, defValue)
	override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.add(listener) }
	override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.remove(listener) }
	override fun edit(): SharedPreferences.Editor {
		val local = account.edit()
		val global = device.edit()
		return object : SharedPreferences.Editor {
			private fun editor(key: String?) = if (key in deviceKeys) global else local
			override fun putString(key: String?, value: String?) = apply { editor(key).putString(key, value) }
			override fun putStringSet(key: String?, values: Set<String>?) = apply { editor(key).putStringSet(key, values) }
			override fun putInt(key: String?, value: Int) = apply { editor(key).putInt(key, value) }
			override fun putLong(key: String?, value: Long) = apply { editor(key).putLong(key, value) }
			override fun putFloat(key: String?, value: Float) = apply { editor(key).putFloat(key, value) }
			override fun putBoolean(key: String?, value: Boolean) = apply { editor(key).putBoolean(key, value) }
			override fun remove(key: String?) = apply { editor(key).remove(key) }
			override fun clear() = apply { local.clear() }
			override fun commit(): Boolean { val saved = local.commit(); return global.commit() && saved }
			override fun apply() { local.apply(); global.apply() }
		}
	}
}
