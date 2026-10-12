package org.jellyfin.androidtv.util

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
