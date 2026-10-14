package org.jellyfin.androidtv.util

object ChapterNavigation {
	@JvmStatic
	fun indexAt(starts: LongArray, position: Long): Int {
		if (starts.isEmpty()) return -1
		return starts.indices.filter { starts[it] <= position }
			.maxByOrNull { starts[it] } ?: 0
	}
}
