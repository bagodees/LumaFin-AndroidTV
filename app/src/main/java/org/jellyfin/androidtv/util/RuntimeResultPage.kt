package org.jellyfin.androidtv.util

import java.util.UUID

/** Bound URL size and advance through the checked index, including near its final page. */
fun runtimePageIds(ids: List<UUID>, offset: Int, limit: Int): List<UUID> {
	require(offset >= 0 && limit >= 0)
	if (offset >= ids.size || limit == 0) return emptyList()
	return ids.subList(offset, offset + minOf(limit, 100, ids.size - offset))
}

/** Jellyfin may return requested IDs in a different order; missing IDs invalidate the index. */
fun <T> orderRuntimePage(ids: List<UUID>, items: List<T>, id: (T) -> UUID): List<T> {
	val mapped = items.associateBy(id)
	if (items.size != ids.size || mapped.size != ids.size || ids.any { it !in mapped }) throw RuntimeLibraryChanged()
	return ids.map(mapped::getValue)
}
