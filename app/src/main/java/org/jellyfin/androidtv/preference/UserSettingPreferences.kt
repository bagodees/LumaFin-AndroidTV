package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.store.DisplayPreferencesStore
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.sdk.api.client.ApiClient

class UserSettingPreferences(
	api: ApiClient,
) : DisplayPreferencesStore(
	displayPreferencesId = "usersettings",
	api = api,
	app = "emby",
) {
	companion object {
		val skipBackLength = intPreference("skipBackLength", 10_000)
		val skipForwardLength = intPreference("skipForwardLength", 30_000)

		val homesection0 = enumPreference("homesection0", HomeSectionType.LIBRARY_TILES_SMALL)
		val homesection1 = enumPreference("homesection1", HomeSectionType.RESUME)
		val homesection2 = enumPreference("homesection2", HomeSectionType.RESUME_AUDIO)
		val homesection3 = enumPreference("homesection3", HomeSectionType.RESUME_BOOK)
		val homesection4 = enumPreference("homesection4", HomeSectionType.LIVE_TV)
		val homesection5 = enumPreference("homesection5", HomeSectionType.NEXT_UP)
		val homesection6 = enumPreference("homesection6", HomeSectionType.LATEST_MEDIA)
		val homesection7 = enumPreference("homesection7", HomeSectionType.NONE)
		val homesection8 = enumPreference("homesection8", HomeSectionType.NONE)
		val homesection9 = enumPreference("homesection9", HomeSectionType.NONE)
		val homesection10 = enumPreference("homesection10", HomeSectionType.LATEST_MOVIES)
		val homesection11 = enumPreference("homesection11", HomeSectionType.LATEST_SHOWS)
		val homesection12 = enumPreference("homesection12", HomeSectionType.BECAUSE_YOU_WATCHED)
		val homesection13 = enumPreference("homesection13", HomeSectionType.COLLECTIONS)
		val homesection14 = enumPreference("homesection14", HomeSectionType.WATCH_AGAIN)
		val homesection15 = enumPreference("homesection15", HomeSectionType.GENRES)
	}

	val homesections = listOf(
		homesection0,
		homesection1,
		homesection2,
		homesection3,
		homesection4,
		homesection5,
		homesection6,
		homesection7,
		homesection8,
		homesection9,
		homesection10,
		homesection11,
		homesection12,
		homesection13,
		homesection14,
		homesection15,
	)

	val activeHomesections
		get() = homesections
			.map(::get)
			.filterNot { it == HomeSectionType.NONE }
}
