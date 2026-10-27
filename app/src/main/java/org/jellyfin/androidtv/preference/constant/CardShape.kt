package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/** Card shape for a group of cards (#123): wide artwork or portrait cover art. */
enum class CardShape(override val nameRes: Int) : PreferenceEnum {
	LANDSCAPE(R.string.card_shape_landscape),
	PORTRAIT(R.string.card_shape_portrait),
}
