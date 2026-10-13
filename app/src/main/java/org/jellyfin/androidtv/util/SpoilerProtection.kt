package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.seriesThumbImage
import org.jellyfin.androidtv.util.apiclient.seriesPrimaryImage
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.androidtv.util.apiclient.parentBackdropImages
import org.jellyfin.sdk.model.api.ImageType
import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.java.KoinJavaComponent

internal fun shouldHideEpisodeDescription(item: BaseItemDto, enabled: Boolean): Boolean =
	enabled && item.type == BaseItemKind.EPISODE && item.userData?.played != true

fun descriptionForDisplay(context: Context, item: BaseItemDto): String? {
	val preferences = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)
	return if (shouldHideEpisodeDescription(item, preferences[UserPreferences.hideUnwatchedEpisodeDescriptions])) {
		context.getString(R.string.episode_description_hidden)
	} else item.overview
}

fun shouldHideEpisodeArtwork(item: BaseItemDto, enabled: Boolean): Boolean =
	enabled && item.type == BaseItemKind.EPISODE && item.userData?.played != true

fun hideEpisodeArtwork(item: BaseItemDto): Boolean {
	val preferences = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)
	return shouldHideEpisodeArtwork(item, preferences[UserPreferences.hideUnwatchedEpisodeArtwork])
}

/** Never fall back to the episode's own images when protecting unwatched artwork. */
fun safeSeriesArtwork(item: BaseItemDto): JellyfinImage? =
	item.seriesThumbImage
		?: item.parentImages[ImageType.THUMB]?.takeIf { it.item == item.seriesId }
		?: item.parentBackdropImages.firstOrNull { it.item == item.seriesId }
		?: item.seriesPrimaryImage
		?: item.parentImages[ImageType.PRIMARY]?.takeIf { it.item == item.seriesId }
