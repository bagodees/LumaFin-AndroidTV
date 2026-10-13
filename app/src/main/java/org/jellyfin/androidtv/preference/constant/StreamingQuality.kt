package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R

enum class StreamingQuality(val nameRes: Int, val descriptionRes: Int, val maxWidth: Int? = null, val maxHeight: Int? = null, val bitrate: Int? = null) {
	ORIGINAL(R.string.quality_original, R.string.quality_original_description, bitrate = Int.MAX_VALUE),
	UHD(R.string.quality_4k, R.string.quality_4k_description, 3840, 2160, 40_000_000),
	FULL_HD(R.string.quality_1080p, R.string.quality_1080p_description, 1920, 1080, 12_000_000),
	HD(R.string.quality_720p, R.string.quality_720p_description, 1280, 720, 6_000_000),
	SD(R.string.quality_480p, R.string.quality_480p_description, 854, 480, 2_000_000),
	CUSTOM(R.string.quality_custom, R.string.quality_custom_description),
}
