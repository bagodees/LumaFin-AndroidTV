package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/** App text size (#23), multiplied with the TV's own text-size setting. */
enum class TextScale(override val nameRes: Int, val factor: Float) : PreferenceEnum {
	DEFAULT(R.string.text_scale_default, 1f),
	LARGE(R.string.text_scale_large, 1.15f),
	LARGER(R.string.text_scale_larger, 1.3f),
}
