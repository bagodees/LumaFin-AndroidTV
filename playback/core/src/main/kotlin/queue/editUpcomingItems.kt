package org.jellyfin.playback.core.queue

/** Session-only edit: previous/current entries are immutable. A null destination removes the source. */
fun <T> editUpcomingItems(items: List<T>, currentIndex: Int, fromIndex: Int, toIndex: Int?): List<T>? {
	if (currentIndex !in items.indices || fromIndex !in items.indices || fromIndex <= currentIndex) return null
	if (toIndex != null && (toIndex !in items.indices || toIndex <= currentIndex || toIndex == fromIndex)) return null
	return items.toMutableList().apply {
		val entry = removeAt(fromIndex)
		if (toIndex != null) add(toIndex, entry)
	}
}
