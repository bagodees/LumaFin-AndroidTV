package org.jellyfin.androidtv.ui.browsing

/** Fill the viewport plus one row/column, without making every TV request 100 cards. */
fun browsePageSize(configured: Int, visible: Int, stride: Int): Int {
	if (configured <= 0) return 0 // Non-paged queries retain their existing behavior.
	return maxOf(configured, visible.coerceAtLeast(0) + stride.coerceAtLeast(0)).coerceIn(25, 150)
}
