package org.jellyfin.androidtv.data.service

import org.jellyfin.androidtv.util.hideEpisodeArtwork
import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.BackdropBehavior
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.parentBackdropImages
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BackgroundService(
	private val context: Context,
	private val jellyfin: Jellyfin,
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val imageLoader: ImageLoader,
) {
	companion object {
		val SLIDESHOW_DURATION = 30.seconds
		val TRANSITION_DURATION = 800.milliseconds
	}

	// Async
	private val scope = MainScope()
	private var loadBackgroundsJob: Job? = null
	private var updateBackgroundTimerJob: Job? = null
	private var lastBackgroundTimerUpdate = 0L

	// Current background data
	private var _backgrounds = emptyList<ImageBitmap>()
	private var _currentIndex = 0
	private var _currentBackground = MutableStateFlow<ImageBitmap?>(null)
	private var _blurBackground = MutableStateFlow(false)
	private var _enabled = MutableStateFlow(true)
	val currentBackground get() = _currentBackground.asStateFlow()
	val blurBackground get() = _blurBackground.asStateFlow()
	val enabled get() = _enabled.asStateFlow()

	/**
	 * Use all available backdrops from [baseItem] as background.
	 */
	private var lastBackgroundItem: BaseItemDto? = null
	init {
		scope.launch {
			userPreferences.episodeArtworkChanges.collect {
				lastBackgroundItem?.let { item -> setBackground(item) }
			}
		}
	}

	fun setBackground(baseItem: BaseItemDto?) {
		lastBackgroundItem = baseItem
		val backdropBehavior = userPreferences[UserPreferences.backdropBehavior]

		// Check if item is set and backgrounds are enabled
		if (baseItem == null || backdropBehavior == BackdropBehavior.DISABLED)
			return clearBackgrounds()

		// Enable blur for backdrops
		_blurBackground.value = backdropBehavior == BackdropBehavior.BACKDROP_WITH_BLUR

		// Get all backdrop urls
		val backdropImages = if (hideEpisodeArtwork(baseItem))
			baseItem.parentBackdropImages.filter { it.item == baseItem.seriesId }
		else baseItem.itemBackdropImages + baseItem.parentBackdropImages
		val backdropUrls = backdropImages
			.map { it.getUrl(api) }
			.toSet()

		loadBackgrounds(backdropUrls)
	}

	/** A backdrop from an image address, for titles not in the library such as Seerr's (#100). */
	fun setBackgroundUrl(url: String?) {
		lastBackgroundItem = null
		val backdropBehavior = userPreferences[UserPreferences.backdropBehavior]
		if (url.isNullOrBlank() || backdropBehavior == BackdropBehavior.DISABLED) return clearBackgrounds()
		_blurBackground.value = backdropBehavior == BackdropBehavior.BACKDROP_WITH_BLUR
		loadBackgrounds(setOf(url))
	}

	/**
	 * Use splashscreen from [server] as background.
	 */
	fun setBackground(server: Server) {
		lastBackgroundItem = null
		// Check if item is set and backgrounds are enabled
		if (userPreferences[UserPreferences.backdropBehavior] == BackdropBehavior.DISABLED)
			return clearBackgrounds()

		// Check if splashscreen is enabled in (cached) branding options
		if (!server.splashscreenEnabled)
			return clearBackgrounds()

		// Disable blur on splashscreen
		_blurBackground.value = false

		// Manually grab the backdrop URL
		val api = jellyfin.createApi(baseUrl = server.address)
		val splashscreenUrl = api.imageApi.getSplashscreenUrl()

		loadBackgrounds(setOf(splashscreenUrl))
	}

	private fun loadBackgrounds(backdropUrls: Set<String>) {
		if (backdropUrls.isEmpty()) return clearBackgrounds()

		// Re-enable backgrounds if disabled
		_enabled.value = true

		// Cancel current loading job
		loadBackgroundsJob?.cancel()
		loadBackgroundsJob = scope.launch(Dispatchers.IO) {
			_backgrounds = backdropUrls.mapNotNull { url ->
				imageLoader.execute(
					request = ImageRequest.Builder(context).data(url).build()
				).image?.toBitmap()?.asImageBitmap()
			}

			// Go to first background
			_currentIndex = 0
			update()
		}
	}

	/**
	 * Drops the loaded backdrops and keeps backgrounds off, freeing their memory while a video
	 * plays on a low-memory device. They load again with the next page that sets one.
	 */
	fun releaseMemory() {
		loadBackgroundsJob?.cancel()
		_backgrounds = emptyList()
		_currentIndex = 0
		_currentBackground.value = null
		_enabled.value = false
	}

	fun clearBackgrounds() {
		loadBackgroundsJob?.cancel()

		// Re-enable backgrounds if disabled
		_enabled.value = true

		if (_backgrounds.isEmpty()) return

		_backgrounds = emptyList()
		update()
	}

	/**
	 * Disable the showing of backgrounds until any function manipulating the backgrounds is called.
	 */
	fun disable() {
		_enabled.value = false
	}

	internal fun update() {
		val now = Instant.now().toEpochMilli()
		if (lastBackgroundTimerUpdate > now - TRANSITION_DURATION.inWholeMilliseconds)
			return setTimer((lastBackgroundTimerUpdate - now).milliseconds + TRANSITION_DURATION, false)

		lastBackgroundTimerUpdate = now

		// Get next background to show
		if (_currentIndex >= _backgrounds.size) _currentIndex = 0

		// Set background
		_currentBackground.value = _backgrounds.getOrNull(_currentIndex)

		// Set timer for next background
		if (_backgrounds.size > 1) setTimer()
		else updateBackgroundTimerJob?.cancel()
	}

	private fun setTimer(updateDelay: Duration = SLIDESHOW_DURATION, increaseIndex: Boolean = true) {
		updateBackgroundTimerJob?.cancel()
		updateBackgroundTimerJob = scope.launch {
			delay(updateDelay)

			if (increaseIndex) _currentIndex++

			update()
		}
	}
}
