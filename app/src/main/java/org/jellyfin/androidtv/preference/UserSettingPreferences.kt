package org.jellyfin.androidtv.preference

import android.content.Context
import androidx.preference.PreferenceManager
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.store.SharedPreferenceStore

/**
 * Stored locally on-device (not synced via the server's shared display preferences) so that
 * home layout changes can't be reverted by another client or a session refresh racing the save.
 */
class UserSettingPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {
	companion object {
		val skipBackLength = intPreference("skipBackLength", 10_000)
		val skipForwardLength = intPreference("skipForwardLength", 30_000)

		val homesection0 = enumPreference("homesection0", HomeSectionType.RESUME)
		val homesection1 = enumPreference("homesection1", HomeSectionType.NEXT_UP)
		val homesection2 = enumPreference("homesection2", HomeSectionType.LATEST_MOVIES)
		val homesection3 = enumPreference("homesection3", HomeSectionType.LATEST_SHOWS)
		val homesection4 = enumPreference("homesection4", HomeSectionType.BECAUSE_YOU_WATCHED)
		val homesection5 = enumPreference("homesection5", HomeSectionType.LIBRARY_TILES_SMALL)
		val homesection6 = enumPreference("homesection6", HomeSectionType.LATEST_MEDIA)
		val homesection7 = enumPreference("homesection7", HomeSectionType.WATCH_AGAIN)
		val homesection8 = enumPreference("homesection8", HomeSectionType.COLLECTIONS)
		val homesection9 = enumPreference("homesection9", HomeSectionType.GENRES)
		val homesection10 = enumPreference("homesection10", HomeSectionType.NONE)
		val homesection11 = enumPreference("homesection11", HomeSectionType.NONE)
		val homesection12 = enumPreference("homesection12", HomeSectionType.NONE)
		val homesection13 = enumPreference("homesection13", HomeSectionType.NONE)
		val homesection14 = enumPreference("homesection14", HomeSectionType.NONE)
		val homesection15 = enumPreference("homesection15", HomeSectionType.NONE)
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
