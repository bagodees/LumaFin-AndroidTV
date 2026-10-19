package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType

fun supportsCompactList(type: CollectionType?) = type == CollectionType.MOVIES || type == CollectionType.TVSHOWS

/** Missing metadata stays missing; never fabricate duration or treat unknown progress as watched. */
internal fun compactDurationMinutes(item: BaseItemDto): Long? = item.runTimeTicks?.takeIf { it > 0 }
	?.div(600_000_000L)?.coerceAtLeast(1)

internal fun compactProgress(item: BaseItemDto): Float? {
	if (item.userData?.played == true) return 1f
	val percentage = item.userData?.playedPercentage?.takeIf { it.isFinite() && it > 0 }
	if (percentage != null) return (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
	val position = item.userData?.playbackPositionTicks?.takeIf { it > 0 } ?: return null
	val duration = item.runTimeTicks?.takeIf { it > 0 } ?: return null
	return (position.toDouble() / duration).coerceIn(0.0, 1.0).toFloat()
}
