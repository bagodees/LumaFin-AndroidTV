package org.jellyfin.androidtv.ui.settings.search

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.preference.constant.HdrFormat
import org.jellyfin.androidtv.preference.constant.BitstreamAudioFormat
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.nameRes
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.preference.PreferenceEnum

/** Read a fresh snapshot on entering/resuming search; opening a result never changes a value. */
fun settingsSearchCatalog(context: Context, prefs: UserPreferences, liveTvEnabled: Boolean, userSettings: UserSettingPreferences, segments: MediaSegmentRepository): List<SettingsSearchEntry> = buildList {
	fun text(id: Int) = context.getString(id)
	fun name(value: PreferenceEnum) = text(value.nameRes)
	fun enabled(value: Boolean) = text(if (value) R.string.settings_search_on else R.string.settings_search_off)
	fun addSetting(route: String, title: Int, section: Int, description: Int, value: String? = null, parameters: Map<String, String> = emptyMap()) {
		add(SettingsSearchEntry("$route:$title:${parameters.toSortedMap()}", route, text(title), text(section), text(description), value, parameters))
	}
	fun seconds(value: Long) = context.getString(R.string.settings_value_seconds, java.text.DecimalFormat("0.##").format(value / 1000.0))
	fun color(value: Long) = String.format(java.util.Locale.ROOT, "#%08X", value)
	val appearance = R.string.settings_group_appearance
	val playback = R.string.pref_playback
	val subtitles = R.string.pref_subtitles
	addSetting(Routes.SETUP_GUIDE, R.string.setup_guide_title, R.string.settings_group_help, R.string.setup_guide_revisit)
	addSetting(Routes.AUTHENTICATION, R.string.settings_group_accounts, R.string.settings, R.string.settings_search_login)
	addSetting(Routes.CUSTOMIZATION, appearance, R.string.settings, R.string.settings_search_appearance)
	addSetting(Routes.CUSTOMIZATION_THEME, R.string.pref_app_theme, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.appTheme]))
	addSetting(Routes.CUSTOMIZATION_CLOCK, R.string.pref_clock_display, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.clockBehavior]))
	addSetting(Routes.CUSTOMIZATION_WATCHED_INDICATOR, R.string.pref_watched_indicator, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.watchedIndicatorBehavior]))
	addSetting(Routes.CUSTOMIZATION_BACKDROP, R.string.lbl_show_backdrop, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.backdropBehavior]))
	addSetting(Routes.CUSTOMIZATION, R.string.media_card_size, appearance, R.string.settings_search_appearance, name(prefs[UserPreferences.mediaCardSize]))
	addSetting(Routes.CUSTOMIZATION, R.string.lbl_use_series_thumbnails, appearance, R.string.settings_search_appearance, enabled(prefs[UserPreferences.seriesThumbnailsEnabled]))
	addSetting(Routes.CUSTOMIZATION, R.string.pref_rating_badges, appearance, R.string.settings_search_appearance, enabled(prefs[UserPreferences.ratingBadgesEnabled]))
	addSetting(Routes.CUSTOMIZATION, R.string.reduced_motion, appearance, R.string.settings_search_motion, enabled(prefs[UserPreferences.reducedMotion]))
	addSetting(Routes.CUSTOMIZATION, R.string.text_scale, appearance, R.string.text_scale_description, name(prefs[UserPreferences.textScale]))
	addSetting(Routes.CUSTOMIZATION, R.string.high_contrast, appearance, R.string.high_contrast_description, enabled(prefs[UserPreferences.highContrast]))
	addSetting(Routes.CUSTOMIZATION, R.string.strong_focus, appearance, R.string.strong_focus_description, enabled(prefs[UserPreferences.strongFocus]))
	addSetting(Routes.CUSTOMIZATION, R.string.hide_unwatched_episode_artwork, appearance, R.string.settings_search_spoilers, enabled(prefs[UserPreferences.hideUnwatchedEpisodeArtwork]))
	addSetting(Routes.CUSTOMIZATION, R.string.hide_unwatched_episode_descriptions, appearance, R.string.settings_search_spoilers, enabled(prefs[UserPreferences.hideUnwatchedEpisodeDescriptions]))
	addSetting(Routes.HOME, R.string.home_prefs, appearance, R.string.settings_search_home)
	addSetting(Routes.HOME, R.string.new_home_title, R.string.home_prefs, R.string.new_home_description, enabled(prefs[UserPreferences.newHomeEnabled]))
	if (prefs[UserPreferences.newHomeEnabled]) addSetting(Routes.HOME, R.string.new_home_card_titles, R.string.home_prefs, R.string.new_home_card_titles_description, enabled(prefs[UserPreferences.newHomeCardTitles]))
	addSetting(Routes.REMOTE_SHORTCUTS, R.string.remote_shortcuts, appearance, R.string.remote_shortcuts_help, text(prefs[UserPreferences.cardLongPressAction].nameRes))
	addSetting(Routes.HOME, R.string.home_item_limits, R.string.home_prefs, R.string.settings_search_home)
	addSetting(Routes.HOME_SHORTCUTS, R.string.home_shortcuts, R.string.home_prefs, R.string.settings_search_shortcuts)
	addSetting(Routes.HOME_RECOMMENDATIONS, R.string.recommendation_settings, R.string.home_prefs, R.string.settings_search_recommendations)
	addSetting(Routes.LIBRARIES, R.string.pref_libraries, appearance, R.string.settings_search_libraries)
	addSetting(Routes.LIBRARIES, R.string.library_compact_list, appearance, R.string.library_compact_list_description)
	addSetting(Routes.CUSTOMIZATION_SCREENSAVER, R.string.pref_screensaver, appearance, R.string.settings_search_screensaver, enabled(prefs[UserPreferences.screensaverInAppEnabled]))
	addSetting(Routes.CUSTOMIZATION_SCREENSAVER_TIMEOUT, R.string.pref_screensaver_inapp_timeout, R.string.pref_screensaver, R.string.settings_search_screensaver, seconds(prefs[UserPreferences.screensaverInAppTimeout]))
	addSetting(Routes.CUSTOMIZATION_SCREENSAVER_AGE_RATING, R.string.pref_screensaver_ageratingmax, R.string.pref_screensaver, R.string.settings_search_screensaver, when (val age = prefs[UserPreferences.screensaverAgeRatingMax]) { -1 -> text(R.string.pref_screensaver_ageratingmax_unlimited); 0 -> text(R.string.pref_screensaver_ageratingmax_zero); else -> context.getString(R.string.pref_screensaver_ageratingmax_entry, age) })
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, subtitles, playback, R.string.settings_search_subtitles)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.subtitle_brightness, subtitles, R.string.settings_search_subtitles, context.getString(R.string.settings_search_percent, prefs[UserPreferences.subtitleBrightness]))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.pref_subtitles_size, subtitles, R.string.settings_search_subtitles, context.getString(R.string.settings_search_points, prefs[UserPreferences.subtitlesTextSize].toString()))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.pref_subtitles_weight, subtitles, R.string.settings_search_subtitles, prefs[UserPreferences.subtitlesTextWeight].toString())
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.pref_subtitles_position, subtitles, R.string.settings_subtitle_position_description, context.getString(R.string.settings_search_percent, kotlin.math.round(prefs[UserPreferences.subtitlesOffsetPosition] * 100).toInt()))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.settings_subtitle_reset_appearance, subtitles, R.string.settings_subtitle_reset_appearance_scope)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.settings_subtitle_reset_position, subtitles, R.string.settings_subtitle_reset_position_scope)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES, R.string.settings_subtitle_timing_help, subtitles, R.string.settings_subtitle_timing_help_description)
	addSetting(Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR, R.string.lbl_subtitle_text_color, subtitles, R.string.settings_search_subtitle_colors, color(prefs[UserPreferences.subtitlesTextColor]))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR, R.string.lbl_subtitle_background_color, subtitles, R.string.settings_search_subtitle_colors, color(prefs[UserPreferences.subtitlesBackgroundColor]))
	addSetting(Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR, R.string.lbl_subtitle_text_stroke_color, subtitles, R.string.settings_search_subtitle_colors, color(prefs[UserPreferences.subtitleTextStrokeColor]))
	addSetting(Routes.PLAYBACK, playback, R.string.settings, R.string.settings_search_advanced)
	addSetting(Routes.PLAYBACK_PLAYER, R.string.playback_video_player, playback, R.string.settings_search_player)
	addSetting(Routes.PLAYBACK_PHOTO_PLAYER, R.string.photo_player, playback, R.string.settings_search_photos)
	addSetting(Routes.PLAYBACK_AUTOPLAY, R.string.pref_playback_autoplay, playback, R.string.settings_search_autoplay, enabled(prefs[UserPreferences.mediaQueuingEnabled]))
	addSetting(Routes.PLAYBACK_NEXT_UP, R.string.pref_playback_next_up, playback, R.string.settings_search_nextup, name(prefs[UserPreferences.nextUpBehavior]))
	addSetting(Routes.PLAYBACK_NEXT_UP_BEHAVIOR, R.string.pref_next_up_behavior_title, playback, R.string.settings_search_nextup, name(prefs[UserPreferences.nextUpBehavior]))
	addSetting(Routes.PLAYBACK_INACTIVITY_PROMPT, R.string.pref_playback_inactivity_prompt, playback, R.string.settings_search_inactivity, name(prefs[UserPreferences.stillWatchingBehavior]))
	addSetting(Routes.PLAYBACK_PREROLLS, R.string.pref_playback_prerolls, playback, R.string.settings_search_prerolls, enabled(prefs[UserPreferences.cinemaModeEnabled]))
	addSetting(Routes.PLAYBACK_MEDIA_SEGMENTS, R.string.settings_intro_recap_credits, playback, R.string.settings_search_segments)
	addSetting(Routes.PLAYBACK, R.string.playback_choices_clear, playback, R.string.playback_choices_clear_description)
	MediaSegmentRepository.SupportedTypes.forEach { type ->
		addSetting(Routes.PLAYBACK_MEDIA_SEGMENT, type.nameRes, R.string.settings_intro_recap_credits, R.string.settings_segment_requirement,
			name(segments.getDefaultSegmentTypeAction(type)), mapOf("segmentType" to type.toString()))
	}
	addSetting(Routes.PLAYBACK_QUALITY, R.string.pref_streaming_quality, playback, R.string.settings_search_quality, text(prefs[UserPreferences.streamingQuality].nameRes))
	addSetting(Routes.PLAYBACK_MAX_BITRATE, R.string.pref_max_bitrate_title, playback, R.string.settings_search_bitrate, context.getString(R.string.settings_search_mbps, prefs[UserPreferences.maxBitrate]))
	addSetting(Routes.PLAYBACK_AUDIO_BEHAVIOR, R.string.lbl_audio_output, playback, R.string.settings_search_audio, name(prefs[UserPreferences.audioBehaviour]))
	addSetting(Routes.PLAYBACK_CODEC, R.string.preference_codecs, playback, R.string.settings_search_codecs)
	if (AndroidVersion.isAtLeastQ) addSetting(Routes.PLAYBACK_CODEC, R.string.pref_software_codecs_enabled, playback, R.string.pref_software_codecs_enabled_description, enabled(prefs[UserPreferences.softwareCodecsEnabled]))
	addSetting(Routes.PLAYBACK_AVC_LEVEL, R.string.user_avc_level, playback, R.string.settings_search_codecs, name(prefs[UserPreferences.userAVCLevel]))
	addSetting(Routes.PLAYBACK_HEVC_LEVEL, R.string.user_hevc_level, playback, R.string.settings_search_codecs, name(prefs[UserPreferences.userHEVCLevel]))
	addSetting(Routes.PLAYBACK_HDR_OVERRIDES, R.string.preference_hdr_overrides, playback, R.string.settings_search_hdr)
	HdrFormat.entries.forEach { format ->
		addSetting(Routes.PLAYBACK_HDR_OVERRIDE, format.nameRes, R.string.preference_hdr_overrides, R.string.settings_hdr_guidance,
			name(prefs[format.preference]), mapOf("format" to format.name))
	}
	BitstreamAudioFormat.entries.forEach { format ->
		addSetting(Routes.PLAYBACK_BITSTREAM_AUDIO, format.nameRes, R.string.preference_codecs, R.string.settings_bitstream_guidance,
			name(prefs[format.preference]), mapOf("format" to format.name))
	}
	addSetting(Routes.PLAYBACK_SEEKING, R.string.skip_back_length, playback, R.string.skip_length_description, seconds(userSettings[UserSettingPreferences.skipBackLength].toLong()))
	addSetting(Routes.PLAYBACK_SEEKING, R.string.skip_forward_length, playback, R.string.skip_length_description, seconds(userSettings[UserSettingPreferences.skipForwardLength].toLong()))
	addSetting(Routes.PLAYBACK_SEEKING, R.string.reset_skip_lengths, playback, R.string.reset_skip_lengths_description)
	addSetting(Routes.PLAYBACK_NEXT_UP, R.string.next_up_timer_countdown, playback, R.string.settings_search_nextup, when (val timeout = prefs[UserPreferences.nextUpTimeout]) {
		org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED -> text(R.string.next_up_timer_manual)
		org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_IMMEDIATE -> text(R.string.next_up_timer_immediate)
		else -> seconds(timeout.toLong())
	})
	addSetting(Routes.PLAYBACK_NEXT_UP, R.string.next_up_timer_reset, playback, R.string.next_up_timer_reset_description)
	addSetting(Routes.PLAYBACK_MAX_BITRATE, R.string.settings_reset_bitrate, playback, R.string.settings_search_bitrate)
	addSetting(Routes.PLAYBACK_VIDEO, R.string.video_start_delay, playback, R.string.settings_video_delay_description, seconds(prefs[UserPreferences.videoStartDelay]))
	addSetting(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR, R.string.lbl_refresh_switching, playback, R.string.settings_search_refresh, name(prefs[UserPreferences.refreshRateSwitchingBehavior]))
	addSetting(Routes.PLAYBACK_ZOOM_MODE, R.string.default_video_zoom, playback, R.string.settings_search_zoom, name(prefs[UserPreferences.playerZoomMode]))
	addSetting(Routes.PLAYBACK_BUFFER_LENGTH, R.string.playback_buffer_length, playback, R.string.settings_search_buffer, name(prefs[UserPreferences.bufferLength]))
	addSetting(Routes.PLAYBACK_ADVANCED, R.string.pref_playback_advanced, playback, R.string.settings_search_advanced)
	addSetting(Routes.PLAYBACK_SEEKING, R.string.settings_seeking, playback, R.string.settings_seeking_description)
	addSetting(Routes.PLAYBACK_VIDEO, R.string.pref_video, playback, R.string.settings_video_description)
	addSetting(Routes.PLAYBACK_AUDIO, R.string.pref_audio, playback, R.string.settings_audio_description)
	addSetting(Routes.PLAYBACK_AUDIO, R.string.pref_audio_night_mode, R.string.pref_audio, R.string.settings_night_audio_description, enabled(prefs[UserPreferences.audioNightMode]))
	addSetting(Routes.PLAYBACK_AUDIO, R.string.prefer_exoplayer_ffmpeg, R.string.pref_audio, R.string.prefer_exoplayer_ffmpeg_content, enabled(prefs[UserPreferences.preferExoPlayerFfmpeg]))
	addSetting(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY, R.string.settings_subtitle_compatibility, subtitles, R.string.settings_subtitle_compatibility_description)
	addSetting(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY, R.string.preference_enable_pgs, subtitles, R.string.preference_enable_pgs_description, enabled(prefs[UserPreferences.pgsDirectPlay]))
	addSetting(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY, R.string.preference_enable_ass, subtitles, R.string.preference_enable_ass_description, enabled(prefs[UserPreferences.assDirectPlay]))
	addSetting(Routes.PLAYBACK_SUBTITLE_COMPATIBILITY, R.string.pref_burn_subtitles_when_transcoding, subtitles, R.string.pref_burn_subtitles_when_transcoding_description, enabled(prefs[UserPreferences.subtitlesBurnDuringTranscode]))
	addSetting(Routes.PLAYBACK_TROUBLESHOOTING, R.string.pref_report_device_profile_title, playback, R.string.settings_troubleshooting_description)
	addSetting(Routes.PLAYBACK_RESUME_SUBTRACT_DURATION, R.string.settings_resume_rewind, playback, R.string.settings_resume_description, prefs[UserPreferences.resumeSubtractDuration].toIntOrNull()?.let { if (it == 0) text(R.string.lbl_none) else TimeUtils.formatSeconds(context, it) })
	if (liveTvEnabled) {
		addSetting(Routes.PLAYBACK_LIVE_TV, R.string.lbl_direct_stream_live, playback, R.string.settings_live_direct_description, enabled(prefs[UserPreferences.liveTvDirectPlayEnabled]))
		addSetting(Routes.LIVETV_GUIDE_OPTIONS, R.string.pref_live_tv_cat, playback, R.string.settings_search_libraries)
	}
	addSetting(Routes.HELP, R.string.settings_group_help, R.string.settings, R.string.settings_group_help_description)
	addSetting(Routes.STREAMING_SPEED, R.string.speed_test_title, R.string.settings_group_help, R.string.speed_test_description)
	addSetting(Routes.DIAGNOSTIC_EXPORT, R.string.export_title, R.string.settings_group_help, R.string.export_scope)
	addSetting(Routes.SETTINGS_BACKUP, R.string.backup_title, R.string.settings_group_help, R.string.backup_scope)
	addSetting(Routes.SETTINGS_SYNC, R.string.settings_sync_title, R.string.settings_group_help, R.string.settings_sync_description)
	addSetting(Routes.MAINTENANCE, R.string.maintenance_title, R.string.settings, R.string.settings_search_cache)
	addSetting(Routes.TELEMETRY, R.string.pref_telemetry_category, R.string.settings, R.string.settings_search_telemetry)
	addSetting(Routes.ABOUT, R.string.pref_about_title, R.string.settings, R.string.settings_search_about)
	addSetting(Routes.LICENSES, R.string.licenses_link, R.string.pref_about_title, R.string.settings_search_about)
}
