package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.preference.constant.StreamingQuality
import org.jellyfin.androidtv.preference.constant.MediaCardSize
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jellyfin.androidtv.preference.UserPreferences.Companion.screensaverInAppEnabled
import org.jellyfin.androidtv.preference.constant.AVCLevel
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.BackdropBehavior
import org.jellyfin.androidtv.preference.constant.BitstreamAudioMode
import org.jellyfin.androidtv.preference.constant.BufferLength
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.HEVCLevel
import org.jellyfin.androidtv.preference.constant.HdrOverrideMode
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import org.jellyfin.androidtv.preference.constant.StillWatchingBehavior
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.toMediaSegmentActionsString
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.floatPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.longPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference
import org.jellyfin.sdk.model.api.MediaSegmentType
import kotlin.time.Duration.Companion.minutes

/**
 * User preferences are configurable by the user and change behavior of the application.
 * When changing preferences migration should be added to the init function.
 *
 * @param context Context to get the SharedPreferences from
 */
class UserPreferences(context: Context, storage: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)) : SharedPreferenceStore(
	sharedPreferences = storage
) {
	val accountRevision get() = (sharedPreferences as? SwitchingAccountPreferences)?.revision ?: 0
	init {
		if (!sharedPreferences.contains(streamingQuality.key) && sharedPreferences.contains(maxBitrate.key)) {
			this[streamingQuality] = StreamingQuality.CUSTOM
		}
	}

	/** Keeps visible cards in sync when the badge setting changes in the settings dialog. */
	val ratingBadgeChanges = callbackFlow {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			if (key == ratingBadgesEnabled.key || key == null) trySend(this@UserPreferences[ratingBadgesEnabled])
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		trySend(this@UserPreferences[ratingBadgesEnabled])
		awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
	}.distinctUntilChanged()

	/** True when Home cards should show artwork only: the new Home is on and titles are not turned back on. */
	val homeCardTitlesHiddenChanges = callbackFlow {
		fun current() = this@UserPreferences[newHomeEnabled] && !this@UserPreferences[newHomeCardTitles]
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			if (key == newHomeEnabled.key || key == newHomeCardTitles.key || key == null) trySend(current())
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		trySend(current())
		awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
	}.distinctUntilChanged()

	/** Updates already-visible browsing effects when the setting changes. */
	val reducedMotionChanges = callbackFlow {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			if (key == reducedMotion.key || key == null) trySend(this@UserPreferences[reducedMotion])
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		trySend(this@UserPreferences[reducedMotion])
		awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
	}.distinctUntilChanged()

	/** High contrast and stronger focus, for visible screens to restyle without being reopened. */
	val accessibilityChanges = callbackFlow {
		fun current() = this@UserPreferences[highContrast] to this@UserPreferences[strongFocus]
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			if (key == highContrast.key || key == strongFocus.key || key == null) trySend(current())
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		trySend(current())
		awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
	}.distinctUntilChanged()

	val mediaCardSizeChanges = callbackFlow {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			if (key == mediaCardSize.key || key == null) trySend(this@UserPreferences[mediaCardSize])
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		trySend(this@UserPreferences[mediaCardSize])
		awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
	}.distinctUntilChanged()

	val episodeArtworkChanges = callbackFlow {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			if (key == hideUnwatchedEpisodeArtwork.key || key == null) trySend(this@UserPreferences[hideUnwatchedEpisodeArtwork])
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		trySend(this@UserPreferences[hideUnwatchedEpisodeArtwork])
		awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
	}.distinctUntilChanged()

	companion object {
		val setupGuideSeen = booleanPreference("setup_guide_seen_v1", false)
		val cardLongPressAction = enumPreference("card_long_press_action", org.jellyfin.androidtv.preference.constant.CardLongPressAction.MENU)
		/* Display */
		/**
		 * Select the app theme
		 */
		var appTheme = enumPreference("app_theme", AppTheme.DARK)

		/**
		 * Behavior of app background while browsing
		 */
		var backdropBehavior = enumPreference("backdrop_behavior", BackdropBehavior.BACKDROP_WITH_BLUR)

		/* Playback - General*/
		/**
		 * Maximum bitrate in megabit for playback.
		 */
		var maxBitrate = stringPreference("pref_max_bitrate", "100")
		val streamingQuality = enumPreference("streaming_quality", StreamingQuality.ORIGINAL)

		/**
		 * Auto-play next item
		 */
		var mediaQueuingEnabled = booleanPreference("pref_enable_tv_queuing", true)

		val mediaCardSize = enumPreference("media_card_size", MediaCardSize.DEFAULT)
		val reducedMotion = booleanPreference("reduced_motion", false)
		/** The LumaFin layout (side menu, preview of the focused title); off is the classic Jellyfin style. */
		val newHomeEnabled = booleanPreference("new_home_enabled", true)
		/** The new Home's outer layout: side menu (default) or top bar with a full backdrop. */
		val homeStyle = enumPreference("home_style", org.jellyfin.androidtv.preference.constant.HomeStyle.SIDE_MENU)
		/** Card shapes (#123) for Continue Watching, the other Home rows and library grids. */
		val continueWatchingCardShape = enumPreference("card_shape_continue_watching", org.jellyfin.androidtv.preference.constant.CardShape.LANDSCAPE)
		val homeRowsCardShape = enumPreference("card_shape_home_rows", org.jellyfin.androidtv.preference.constant.CardShape.LANDSCAPE)
		val libraryCardShape = enumPreference("card_shape_libraries", org.jellyfin.androidtv.preference.constant.CardShape.LANDSCAPE)
		/** Titles under Home cards on the new Home; off by default since the preview shows the focused title. */
		val newHomeCardTitles = booleanPreference("new_home_card_titles", false)
		val textScale = enumPreference("accessibility_text_scale", org.jellyfin.androidtv.preference.constant.TextScale.DEFAULT)
		val highContrast = booleanPreference("accessibility_high_contrast", false)
		val strongFocus = booleanPreference("accessibility_strong_focus", false)
		val hideUnwatchedEpisodeArtwork = booleanPreference("hide_unwatched_episode_artwork", false)
		val hideUnwatchedEpisodeDescriptions = booleanPreference("hide_unwatched_episode_descriptions", false)

		/**
		 * Enable the next up screen or not
		 */
		var nextUpBehavior = enumPreference("next_up_behavior", NextUpBehavior.EXTENDED)

		/**
		 * Next up timeout before playing next item
		 * Stored in milliseconds
		 */
		var nextUpTimeout = intPreference("next_up_timeout", 1000 * 7)

		/**
		 * Duration in seconds to subtract from resume time
		 */
		var resumeSubtractDuration = stringPreference("pref_resume_preroll", "0")

		/**
		 * Enable cinema mode
		 */
		var cinemaModeEnabled = booleanPreference("pref_enable_cinema_mode", true)

		/**
		 * Enable still watching
		 */
		var stillWatchingBehavior = enumPreference("enable_still_watching", StillWatchingBehavior.DISABLED)

		/* Playback - Video */
		/**
		 * Whether to use an external playback application or not.
		 */
		var useExternalPlayer = booleanPreference("external_player", false)

		/**
		 * Component name for the external playback application.
		 */
		var externalPlayerComponentName = stringPreference("external_player_component", "")

		/**
		 * Change refresh rate to match media when device supports it
		 */
		var refreshRateSwitchingBehavior = enumPreference("refresh_rate_switching_behavior", RefreshRateSwitchingBehavior.DISABLED)

		/**
		 * Whether ExoPlayer should prefer FFmpeg renderers to core ones.
		 */
		var preferExoPlayerFfmpeg = booleanPreference("exoplayer_prefer_ffmpeg", defaultValue = false)

		/**
		 * User defined AVC level override. AUTO uses device-reported capabilities.
		 */
		var userAVCLevel = enumPreference("user_avc_level", AVCLevel.AUTO)

		/**
		 * User defined HEVC level override. AUTO uses device-reported capabilities.
		 */
		var userHEVCLevel = enumPreference("user_hevc_level", HEVCLevel.AUTO)

		/**
		 * Playback buffer size preset.
		 */
		var bufferLength = enumPreference("buffer_length", BufferLength.AUTO)

		/* Playback - Audio related */
		/**
		 * Preferred behavior for audio streaming.
		 */
		var audioBehaviour = enumPreference("audio_behavior", AudioBehavior.DIRECT_STREAM)

		/**
		 * Preferred behavior for audio streaming.
		 */
		var audioNightMode = enumPreference("audio_night_mode", false)

		/**
		 * AC3 bitstream device profile override.
		 */
		var bitstreamAc3 = enumPreference("bitstream_ac3_mode", BitstreamAudioMode.AUTO)

		/**
		 * EAC3 bitstream device profile override.
		 */
		var bitstreamEac3 = enumPreference("bitstream_eac3_mode", BitstreamAudioMode.AUTO)

		/**
		 * DTS bitstream device profile override.
		 */
		var bitstreamDts = enumPreference("bitstream_dts_mode", BitstreamAudioMode.AUTO)

		/**
		 * TrueHD bitstream device profile override.
		 */
		var bitstreamTrueHd = enumPreference("bitstream_truehd_mode", BitstreamAudioMode.AUTO)

		/**
		 * HDR10 device profile override.
		 */
		var hdr10Override = enumPreference("hdr10_override", HdrOverrideMode.AUTO)

		/**
		 * HDR10+ device profile override.
		 */
		var hdr10PlusOverride = enumPreference("hdr10_plus_override", HdrOverrideMode.AUTO)

		/**
		 * Dolby Vision Profile 5 device profile override.
		 */
		var doviProfile5Override = enumPreference("dovi_profile_5_override", HdrOverrideMode.AUTO)

		/**
		 * Dolby Vision Profile 7 device profile override.
		 */
		var doviProfile7Override = enumPreference("dovi_profile_7_override", HdrOverrideMode.AUTO)

		/**
		 * Dolby Vision Profile 8 device profile override.
		 */
		var doviProfile8Override = enumPreference("dovi_profile_8_override", HdrOverrideMode.AUTO)

		/* Live TV */
		/**
		 * Use direct play
		 */
		var liveTvDirectPlayEnabled = booleanPreference("pref_live_direct", true)

		/* Developer options */
		/**
		 * Show additional debug information
		 */
		var debuggingEnabled = booleanPreference("pref_enable_debug", false)

		/**
		 * Use playback rewrite module for video
		 */
		var playbackRewriteVideoEnabled = booleanPreference("playback_new", false)

		/**
		 * When to show the clock.
		 */
		var clockBehavior = enumPreference("pref_clock_behavior", ClockBehavior.ALWAYS)

		/**
		 * Set when watched indicators should show on MyImageCardViews
		 */
		var watchedIndicatorBehavior = enumPreference("pref_watched_indicator_behavior", WatchedIndicatorBehavior.ALWAYS)

		/**
		 * Enable series thumbnails in home screen rows
		 */
		var seriesThumbnailsEnabled = booleanPreference("pref_enable_series_thumbnails", true)

		/** Show critic and community rating badges on movie and series cards. */
		val ratingBadgesEnabled = booleanPreference("pref_rating_badges_enabled", true)

		/**
		 * Subtitles foreground color
		 */
		var subtitlesBackgroundColor = longPreference("subtitles_background_color", 0x00FFFFFF)

		/**
		 * Subtitles bold text
		 */
		var subtitlesTextWeight = intPreference("subtitles_text_weight", 400)

		/**
		 * Subtitles foreground color
		 */
		val subtitleBrightness = intPreference("subtitle_brightness", 100)
		var subtitlesTextColor = longPreference("subtitles_text_color", 0xFFFFFFFF)

		/**
		 * Subtitles stroke color
		 */
		var subtitleTextStrokeColor = longPreference("subtitles_text_stroke_color", 0xFF000000)

		/**
		 * Subtitles font size
		 */
		var subtitlesTextSize = floatPreference("subtitles_text_size", 24f)

		/**
		 * Subtitles offset
		 */
		var subtitlesOffsetPosition = floatPreference("subtitles_offset_position", 0.08f)

		/**
		 * Show screensaver in app
		 */
		var screensaverInAppEnabled = booleanPreference("screensaver_inapp_enabled", true)

		/**
		 * Timeout before showing the screensaver in app, depends on [screensaverInAppEnabled].
		 */
		var screensaverInAppTimeout = longPreference("screensaver_inapp_timeout", 5.minutes.inWholeMilliseconds)

		/**
		 * Age rating used to filter items in the screensaver. Use -1 to disable (omits parameter from requests).
		 */
		var screensaverAgeRatingMax = intPreference("screensaver_agerating_max", 13)

		/**
		 * Whether items shown in the screensaver are required to have an age rating set.
		 */
		var screensaverAgeRatingRequired = booleanPreference("screensaver_agerating_required", true)

		/**
		 * Delay when starting video playback after loading the video player.
		 */
		var videoStartDelay = longPreference("video_start_delay", 0)

		/**
		 * The actions to take for each media segment type. Managed by the [MediaSegmentRepository].
		 */
		var mediaSegmentActions = stringPreference(
			key = "media_segment_actions",
			defaultValue = mapOf(
				MediaSegmentType.INTRO to MediaSegmentAction.ASK_TO_SKIP,
				MediaSegmentType.OUTRO to MediaSegmentAction.ASK_TO_SKIP,
			).toMediaSegmentActionsString()
		)

		/**
		 * Preferred behavior for player aspect ratio (zoom mode).
		 */
		var playerZoomMode = enumPreference("player_zoom_mode", ZoomMode.FIT)

		/**
		 * Enable libass.
		 */
		var assDirectPlay = booleanPreference("libass_enabled", false)

		/**
		 * Always burn in subtitles when transcoding.
		 */
		var subtitlesBurnDuringTranscode = booleanPreference("subtitles_burn_during_transcode", false)

		/**
		 * Enable PGS subtitle direct-play.
		 */
		var pgsDirectPlay = booleanPreference("pgs_enabled", true)

		/**
		 * Enable the use of software-based codecs.
		 */
		var softwareCodecsEnabled = booleanPreference("software_codecs_enabled", true)

		/**
		 * Stores the interval for the photo player.
		 */
		var photoPlayerPresentationDelay = longPreference("photo_player_presentation_delay", 8000)
	}

	init {
		// Note: Create a single migration per app version
		// Note: Migrations are never executed for fresh installs
		// Note: Old migrations are removed occasionally
		runMigrations {
			// v0.15.z to v0.16.0
			migration(toVersion = 7) {
				// Enable playback rewrite for music
				putBoolean("playback_new_audio", true)
			}

			// v0.17.z to v0.18.0
			migration(toVersion = 8) {
				// Set subtitle background color to black if it was enabled in a previous version
				val subtitlesBackgroundEnabled = it.getBoolean("subtitles_background_enabled", true)
				putLong("subtitles_background_color", if (subtitlesBackgroundEnabled) 0XFF000000L else 0X00FFFFFFL)

				// Set subtitle text stroke color to black if it was enabled in a previous version
				val subtitleStrokeSize = it.getInt("subtitles_stroke_size", 0)
				putLong("subtitles_text_stroke_color", if (subtitleStrokeSize > 0) 0XFF000000L else 0X00FFFFFFL)
			}

			// v0.19.0 to v0.20.0
			migration(toVersion = 9) {
				// Reset subtitle text size as we changed from fractional sizing to absolute sizing
				remove("subtitles_text_size")

				// Set the BackdropBehavior if it was enabled in a previous version
				val backdropEnabled = it.getBoolean("pref_show_backdrop", true)
				putString(
					"backdrop_behavior",
					if (backdropEnabled) BackdropBehavior.BACKDROP_WITH_BLUR.name else BackdropBehavior.DISABLED.name
				)
			}
		}
	}
}
