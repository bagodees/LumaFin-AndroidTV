package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class MediaCardSize(override val nameRes: Int, val scale: Float) : PreferenceEnum {
	SMALL(R.string.media_card_size_small, 0.8f),
	DEFAULT(R.string.media_card_size_default, 1f),
	LARGE(R.string.media_card_size_large, 1.2f),
}
