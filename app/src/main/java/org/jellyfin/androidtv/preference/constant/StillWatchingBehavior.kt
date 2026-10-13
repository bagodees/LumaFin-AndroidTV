package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class StillWatchingBehavior(
	override val nameRes: Int,
	val enabled: Boolean,
	val episodeCount: Int,
) : PreferenceEnum {
	/**
	 * Shorter than default. Show screen at 2 completed episodes.
	 */
	SHORT(
		nameRes = R.string.still_watching_after_two,
		enabled = true,
		episodeCount = 2,
	),

	/**
	 * Default behavior for still watching. Show screen at 3 completed episodes.
	 */
	DEFAULT(
		nameRes = R.string.still_watching_after_three,
		enabled = true,
		episodeCount = 3,
	),

	/**
	 * Longer than default. Show screen at 5 completed episodes.
	 */
	LONG(
		nameRes = R.string.still_watching_after_five,
		enabled = true,
		episodeCount = 5,
	),

	/**
	 * Much longer than default. Show screen at 8 completed episodes (retained for existing saved preferences).
	 */
	VERY_LONG(
		nameRes = R.string.lbl_still_watching_very_long,
		enabled = true,
		episodeCount = 8,
	),

	/**
	 * Disables still watching screen.
	 */
	DISABLED(
		nameRes = R.string.still_watching_off,
		enabled = false,
		episodeCount = 0,
	)
}
