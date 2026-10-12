package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class NextUpBehavior(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Enable the Next Up screen - show the item's thumbnail
	 */
	EXTENDED(R.string.lbl_next_up_extended),

	/**
	 * Enable the Next Up screen - hide the item's thumbnail
	 */
	MINIMAL(R.string.lbl_next_up_minimal),

	/**
	 * Disable the Next Up screen
	 */
	DISABLED(R.string.lbl_never),
}

const val NEXTUP_TIMER_DISABLED: Int = 0

const val NEXTUP_TIMER_IMMEDIATE: Int = -1

/** Null means wait for user confirmation; zero means play immediately. */
internal fun nextUpCountdownDuration(timeout: Int): Int? = when (timeout) {
	NEXTUP_TIMER_DISABLED -> null
	NEXTUP_TIMER_IMMEDIATE -> 0
	else -> timeout.coerceIn(1_000, 30_000)
}
