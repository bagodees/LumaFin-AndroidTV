package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/** The outer layout of the LumaFin Home; the rows, cards and focus handling are the same in both. */
enum class HomeStyle(override val nameRes: Int) : PreferenceEnum {
	/** A side menu with a preview of the focused title above the rows. */
	SIDE_MENU(R.string.home_style_side_menu),

	/** A top bar with the focused title's artwork as a full backdrop and an info block above the rows. */
	TOP_BAR(R.string.home_style_top_bar),
}
