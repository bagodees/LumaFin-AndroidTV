package org.jellyfin.androidtv.ui.settings.search

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.preference.PreferenceEnum

/** Read a fresh snapshot on entering/resuming search; opening a result never changes a value. */
fun settingsSearchCatalog(context: Context, prefs: UserPreferences, liveTvEnabled: Boolean): List<SettingsSearchEntry> = buildList {
	fun text(id: Int) = context.getString(id)
	fun name(value: PreferenceEnum) = text(value.nameRes)
	fun enabled(value: Boolean) = text(if (value) R.string.settings_search_on else R.string.settings_search_off)
	fun addSetting(route: String, title: Int, section: Int, description: Int, value: String? = null) {
		add(SettingsSearchEntry("$route:$title", route, text(title), text(section), text(description), value))
	}
	val appearance = R.string.pref_customization
	val playback = R.string.pref_playback
	val subtitles = R.string.pref_subtitles
	addSetting(Routes.AUTHENTICATION, R.string.pref_login, R.string.settings, R.string.settings_search_login)
	addSetting(Routes.CUSTOMIZATION, appearance, R.string.settings, R.string.settings_search_appearance)
	addSetting(Routes.CUSTOMIZATION_THEME, R.string.pref_app_theme, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.appTheme]))
	addSetting(Routes.CUSTOMIZATION_CLOCK, R.string.pref_clock_display, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.clockBehavior]))
	addSetting(Routes.CUSTOMIZATION_WATCHED_INDICATOR, R.string.pref_watched_indicator, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.watchedIndicatorBehavior]))
	addSetting(Routes.CUSTOMIZATION_BACKDROP, R.string.lbl_show_backdrop, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.backdropBehavior]))
	addSetting(Routes.CUSTOMIZATION, R.string.media_card_size, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.mediaCardSize]))
	addSetting(Routes.CUSTOMIZATION, R.string.lbl_use_series_thumbnails, appearance, R.string.settings_search_appearance, enabled(prefs[UserPreferences.seriesThumbnailsEnabled]))
	addSetting(Routes.CUSTOMIZATION, R.string.pref_rating_badges, appearance, R.string.settings_search_appearance, enabled(prefs[UserPreferences.ratingBadgesEnabled]))
	addSetting(Routes.CUSTOMIZATION, R.string.reduced_motion, appearance, R.string.settings_search_motion, enabled(prefs[UserPreferences.reducedMotion]))
	addSetting(Routes.CUSTOMIZATION, R.string.hide_unwatched_episode_artwork, appearance, R.string.settings_search_spoilers, enabled(prefs[UserPreferences.hideUnwatchedEpisodeArtwork]))
	addSetting(Routes.CUSTOMIZATION, R.string.hide_unwatched_episode_descriptions, appearance, R.string.settings_search_spoilers, enabled(prefs[UserPreferences.hideUnwatchedEpisodeDescriptions]))
	addSetting(Routes.HOME, R.string.home_prefs, appearance, R.string.settings_search_home)
	addSetting(Routes.HOME, R.string.home_item_limits, R.string.home_prefs, R.string.settings_search_home)
	addSetting(Routes.HOME_SHORTCUTS, R.string.home_shortcuts, R.string.home_prefs, R.string.settings_search_shortcuts)
	addSetting(Routes.HOME_RECOMMENDATIONS, R.string.recommendation_settings, R.string.home_prefs, R.string.settings_search_recommendations)
	addSetting(Routes.LIBRARIES, R.string.pref_libraries, appearance, R.string.settings_search_libraries)
	addSetting(Routes.CUSTOMIZATION_SCREENSAVER, R.string.pref_screensaver, appearance, R.string.settings_search_screensaver, enabled(prefs[UserPreferences.screensaverInAppEnabled]))
	addSetting(Routes.CUSTOMIZATION_SCREENSAVER_TIMEOUT, R.string.pref_screensaver_inapp_timeout, R.string.pref_screensaver, R.string.settings_search_screensaver)
	addSetting(Routes.CUSTOMIZATION_SCREENSAVER_AGE_RATING, R.string.pref_screensaver_ageratingmax, R.string.pref_screensaver, R.string.settings_search_screensaver)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, subtitles, playback, R.string.settings_search_subtitles)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.subtitle_brightness, subtitles, R.string.settings_search_subtitles, context.getString(R.string.settings_search_percent, prefs[UserPreferences.subtitleBrightness]))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.pref_subtitles_size, subtitles, R.string.settings_search_subtitles, context.getString(R.string.settings_search_points, prefs[UserPreferences.subtitlesTextSize].toString()))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR, R.string.lbl_subtitle_text_color, subtitles, R.string.settings_search_subtitle_colors)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR, R.string.lbl_subtitle_background_color, subtitles, R.string.settings_search_subtitle_colors)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR, R.string.lbl_subtitle_text_stroke_color, subtitles, R.string.settings_search_subtitle_colors)
	addSetting(Routes.PLAYBACK, playback, R.string.settings, R.string.settings_search_advanced)
	addSetting(Routes.PLAYBACK_PLAYER, R.string.playback_video_player, playback, R.string.settings_search_player)
	addSetting(Routes.PLAYBACK_PHOTO_PLAYER, R.string.photo_player, playback, R.string.settings_search_photos)
	addSetting(Routes.PLAYBACK_AUTOPLAY, R.string.pref_playback_autoplay, playback, R.string.settings_search_autoplay, enabled(prefs[UserPreferences.mediaQueuingEnabled]))
	addSetting(Routes.PLAYBACK_NEXT_UP, R.string.pref_playback_next_up, playback, R.string.settings_search_nextup, name(prefs[UserPreferences.nextUpBehavior]))
	addSetting(Routes.PLAYBACK_NEXT_UP_BEHAVIOR, R.string.pref_next_up_behavior_title, playback, R.string.settings_search_nextup, name(prefs[UserPreferences.nextUpBehavior]))
	addSetting(Routes.PLAYBACK_INACTIVITY_PROMPT, R.string.pref_playback_inactivity_prompt, playback, R.string.settings_search_inactivity, name(prefs[UserPreferences.stillWatchingBehavior]))
	addSetting(Routes.PLAYBACK_PREROLLS, R.string.pref_playback_prerolls, playback, R.string.settings_search_prerolls, enabled(prefs[UserPreferences.cinemaModeEnabled]))
	addSetting(Routes.PLAYBACK_MEDIA_SEGMENTS, R.string.pref_playback_media_segments, playback, R.string.settings_search_segments)
	addSetting(Routes.PLAYBACK_QUALITY, R.string.pref_streaming_quality, playback, R.string.settings_search_quality, text(prefs[UserPreferences.streamingQuality].nameRes))
	addSetting(Routes.PLAYBACK_MAX_BITRATE, R.string.pref_max_bitrate_title, playback, R.string.settings_search_quality, context.getString(R.string.settings_search_mbps, prefs[UserPreferences.maxBitrate]))
	addSetting(Routes.PLAYBACK_AUDIO_BEHAVIOR, R.string.lbl_audio_output, playback, R.string.settings_search_audio, name(prefs[UserPreferences.audioBehaviour]))
	addSetting(Routes.PLAYBACK_CODEC, R.string.preference_codecs, playback, R.string.settings_search_codecs)
	addSetting(Routes.PLAYBACK_AVC_LEVEL, R.string.user_avc_level, playback, R.string.settings_search_codecs, name(prefs[UserPreferences.userAVCLevel]))
	addSetting(Routes.PLAYBACK_HEVC_LEVEL, R.string.user_hevc_level, playback, R.string.settings_search_codecs, name(prefs[UserPreferences.userHEVCLevel]))
	addSetting(Routes.PLAYBACK_HDR_OVERRIDES, R.string.preference_hdr_overrides, playback, R.string.settings_search_hdr)
	addSetting(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR, R.string.lbl_refresh_switching, playback, R.string.settings_search_refresh, name(prefs[UserPreferences.refreshRateSwitchingBehavior]))
	addSetting(Routes.PLAYBACK_ZOOM_MODE, R.string.default_video_zoom, playback, R.string.settings_search_zoom, name(prefs[UserPreferences.playerZoomMode]))
	addSetting(Routes.PLAYBACK_BUFFER_LENGTH, R.string.playback_buffer_length, playback, R.string.settings_search_buffer, name(prefs[UserPreferences.bufferLength]))
	addSetting(Routes.PLAYBACK_ADVANCED, R.string.pref_playback_advanced, playback, R.string.settings_search_advanced)
	addSetting(Routes.PLAYBACK_RESUME_SUBTRACT_DURATION, R.string.lbl_resume_preroll, playback, R.string.settings_search_advanced)
	if (liveTvEnabled) {
		addSetting(Routes.LIVETV_GUIDE_OPTIONS, R.string.pref_live_tv_cat, playback, R.string.settings_search_libraries)
	}
	addSetting(Routes.MAINTENANCE, R.string.maintenance_title, R.string.settings, R.string.settings_search_cache)
	addSetting(Routes.TELEMETRY, R.string.pref_telemetry_category, R.string.settings, R.string.settings_search_telemetry)
	addSetting(Routes.ABOUT, R.string.pref_about_title, R.string.settings, R.string.settings_search_about)
	addSetting(Routes.LICENSES, R.string.licenses_link, R.string.pref_about_title, R.string.settings_search_about)
}
