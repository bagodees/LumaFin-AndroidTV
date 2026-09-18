package org.jellyfin.androidtv.preference

import android.content.Context
import androidx.preference.PreferenceManager
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.stringPreference
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

		// Ordered, comma separated list of enabled HomeSectionType#serializedName values.
		// Types not present in the list are hidden. Replaces the old fixed homesection0..27
		// slot-based layout, which required navigating into a numbered slot to change what it
		// showed instead of just reordering/toggling sections directly.
		private val homeSectionOrderRaw = stringPreference("homeSectionOrder", "")

		// Legacy slots, kept only so an existing configured order can be migrated once into
		// homeSectionOrderRaw. Do not add new slots here.
		private val legacyHomeSectionSlots = listOf(
			enumPreference("homesection0", HomeSectionType.RESUME),
			enumPreference("homesection1", HomeSectionType.NEXT_UP),
			enumPreference("homesection2", HomeSectionType.LATEST_MOVIES),
			enumPreference("homesection3", HomeSectionType.LATEST_SHOWS),
			enumPreference("homesection4", HomeSectionType.BECAUSE_YOU_WATCHED),
			enumPreference("homesection5", HomeSectionType.LIBRARY_TILES_SMALL),
			enumPreference("homesection6", HomeSectionType.LATEST_MEDIA),
			enumPreference("homesection7", HomeSectionType.WATCH_AGAIN),
			enumPreference("homesection8", HomeSectionType.COLLECTIONS),
			enumPreference("homesection9", HomeSectionType.GENRES),
			enumPreference("homesection10", HomeSectionType.NONE),
			enumPreference("homesection11", HomeSectionType.NONE),
			enumPreference("homesection12", HomeSectionType.NONE),
			enumPreference("homesection13", HomeSectionType.NONE),
			enumPreference("homesection14", HomeSectionType.NONE),
			enumPreference("homesection15", HomeSectionType.NONE),
		)

		// Default order for a fresh install (no legacy slots configured either)
		private val defaultHomeSectionOrder = listOf(
			HomeSectionType.RESUME,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_MOVIES,
			HomeSectionType.LATEST_SHOWS,
			HomeSectionType.BECAUSE_YOU_WATCHED,
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.LATEST_MEDIA,
			HomeSectionType.WATCH_AGAIN,
			HomeSectionType.COLLECTIONS,
			HomeSectionType.GENRES,
		)
	}

	/**
	 * All section types that can be shown on Home, in their configured order. Every type not
	 * present is hidden. Reading this before anything has been saved migrates the old
	 * homesection0..15 slots (if configured) or falls back to [defaultHomeSectionOrder].
	 */
	var activeHomesections: List<HomeSectionType>
		get() {
			val raw = this[homeSectionOrderRaw]
			if (raw.isNotBlank()) {
				return raw.split(',').mapNotNull { name -> HomeSectionType.entries.find { it.serializedName == name } }
			}

			val legacyOrder = legacyHomeSectionSlots.map(::get).filterNot { it == HomeSectionType.NONE }
			val migratedOrder = legacyOrder.ifEmpty { defaultHomeSectionOrder }
			activeHomesections = migratedOrder
			return migratedOrder
		}
		set(value) {
			this[homeSectionOrderRaw] = value.joinToString(",") { it.serializedName }
		}
}
