package org.jellyfin.androidtv.data.model

import org.jellyfin.androidtv.R

/** Width groups also recognize widescreen movies whose black bars were cropped. */
enum class LibraryResolution(val minWidth: Int?, val maxWidth: Int?, val labelRes: Int) {
	ANY(null, null, R.string.library_filter_any),
	SD(1, 1199, R.string.library_resolution_sd),
	HD(1200, 1899, R.string.library_resolution_720),
	FULL_HD(1900, 3799, R.string.library_resolution_1080),
	UHD(3800, null, R.string.library_resolution_4k),
}
