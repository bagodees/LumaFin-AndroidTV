package org.jellyfin.androidtv.ui.settings

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.BackdropBehavior
import org.jellyfin.androidtv.preference.constant.CardLongPressAction
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.MediaCardSize
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.preference.PreferenceEnum

/** Names and readable values for portable settings, shared by backup review and sync conflicts. */
object SettingsValueLabels {
	val labels: Map<String, Int> = mapOf(
		"card_long_press_action" to R.string.remote_shortcuts,
		"homeSectionOrder" to R.string.home_prefs,
		"home_latest_movies_limit" to R.string.backup_latest_movies, "home_latest_shows_limit" to R.string.backup_latest_shows,
		"home_recently_added_movies_limit" to R.string.backup_recent_movies, "home_recently_added_shows_limit" to R.string.backup_recent_shows,
		"home_latest_movies_hide_watched" to R.string.backup_hide_latest, "home_recently_added_movies_hide_watched" to R.string.backup_hide_recent,
		"app_theme" to R.string.pref_app_theme, "backdrop_behavior" to R.string.lbl_show_backdrop,
		"media_card_size" to R.string.media_card_size, "pref_clock_behavior" to R.string.pref_clock_display,
		"pref_watched_indicator_behavior" to R.string.pref_watched_indicator, "reduced_motion" to R.string.reduced_motion,
		"hide_unwatched_episode_artwork" to R.string.hide_unwatched_episode_artwork,
		"hide_unwatched_episode_descriptions" to R.string.hide_unwatched_episode_descriptions,
		"pref_enable_series_thumbnails" to R.string.lbl_use_series_thumbnails, "pref_rating_badges_enabled" to R.string.pref_rating_badges,
		"pref_enable_tv_queuing" to R.string.pref_playback_autoplay, "pref_enable_cinema_mode" to R.string.pref_playback_prerolls,
		"skipBackLength" to R.string.skip_back_length, "skipForwardLength" to R.string.skip_forward_length,
		"subtitle_brightness" to R.string.subtitle_brightness, "subtitles_text_weight" to R.string.pref_subtitles_weight,
		"subtitles_text_size" to R.string.pref_subtitles_size, "subtitles_offset_position" to R.string.pref_subtitles_position,
		"subtitles_background_color" to R.string.lbl_subtitle_background_color, "subtitles_text_color" to R.string.lbl_subtitle_text_color,
		"subtitles_text_stroke_color" to R.string.lbl_subtitle_text_stroke_color,
	)

	fun display(context: Context, key: String, value: Any?): String {
		if (value is Boolean) return context.getString(if (value) R.string.settings_search_on else R.string.settings_search_off)
		if (key == "homeSectionOrder") return value.toString().split(',').mapNotNull { row ->
			HomeSectionType.entries.find { it.serializedName == row }?.let { context.getString(it.nameRes) }
		}.joinToString(", ")
		if (key.endsWith("color")) return String.format(java.util.Locale.ROOT, "#%08X", value)
		val enum: PreferenceEnum? = when (key) {
			"card_long_press_action" -> CardLongPressAction.entries.find { it.name == value }
			"app_theme" -> AppTheme.entries.find { it.name == value }
			"backdrop_behavior" -> BackdropBehavior.entries.find { it.name == value }
			"media_card_size" -> MediaCardSize.entries.find { it.name == value }
			"pref_clock_behavior" -> ClockBehavior.entries.find { it.name == value }
			"pref_watched_indicator_behavior" -> WatchedIndicatorBehavior.entries.find { it.name == value }
			else -> null
		}
		return enum?.let { context.getString(it.nameRes) } ?: value.toString()
	}
}
