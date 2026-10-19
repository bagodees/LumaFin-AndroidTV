package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

internal fun individualWatchTarget(item: BaseItemDto) = item.type == BaseItemKind.MOVIE || item.type == BaseItemKind.EPISODE

internal data class BulkWatchResult(
	val changed: List<UUID>, val unchanged: List<UUID>, val failed: List<UUID>,
	val unconfirmed: List<UUID>, val notAttempted: List<UUID>,
)

/** Explicit leaf-item IDs only. Never recursively mark a series, season or folder. */
internal suspend fun applyBulkWatch(
	selection: List<BaseItemDto>, watched: Boolean,
	getItem: suspend (UUID) -> BaseItemDto,
	setPlayed: suspend (UUID, Boolean) -> Unit,
	isCurrent: () -> Boolean,
	shouldStop: () -> Boolean,
	mayHaveChanged: (BaseItemDto) -> Unit,
): BulkWatchResult {
	require(selection.isNotEmpty() && selection.size <= 100 && selection.all(::individualWatchTarget))
	val targets = selection.distinctBy { it.id }
	val changed = mutableListOf<UUID>()
	val unchanged = mutableListOf<UUID>()
	val failed = mutableListOf<UUID>()
	val unconfirmed = mutableListOf<UUID>()
	val attempted = mutableSetOf<UUID>()
	var consecutiveReadFailures = 0
	for (target in targets) {
		if (!isCurrent() || shouldStop()) break
		val fresh = try {
			withTimeout(15_000) { getItem(target.id) }
		} catch (_: TimeoutCancellationException) {
			failed += target.id; attempted += target.id
			if (++consecutiveReadFailures >= 3) break
			continue
		}
		catch (error: CancellationException) { throw error }
		catch (_: Exception) {
			failed += target.id; attempted += target.id
			if (++consecutiveReadFailures >= 3) break
			continue
		}
		consecutiveReadFailures = 0
		if (!isCurrent() || shouldStop()) break
		if (fresh.id != target.id || !individualWatchTarget(fresh) || fresh.type != target.type) {
			failed += target.id; attempted += target.id; continue
		}
		attempted += target.id
		val data = fresh.userData
		if (data != null && data.played == watched && data.playbackPositionTicks == 0L &&
			(watched || (data.playCount == 0 && data.lastPlayedDate == null && (data.playedPercentage ?: 0.0) == 0.0))) {
			unchanged += target.id
			continue
		}
		try {
			withTimeout(15_000) { setPlayed(target.id, watched) }
			changed += target.id
		} catch (_: TimeoutCancellationException) {
			unconfirmed += target.id
			mayHaveChanged(fresh)
			break
		} catch (error: CancellationException) {
			// A cancelled HTTP write can still finish on the server. Invalidate the old view.
			mayHaveChanged(fresh)
			throw error
		} catch (_: Exception) {
			// Never retry a possibly completed write or continue blindly after one fails.
			unconfirmed += target.id
			mayHaveChanged(fresh)
			break
		}
		mayHaveChanged(fresh)
	}
	return BulkWatchResult(changed, unchanged, failed, unconfirmed, targets.map { it.id }.filter { it !in attempted })
}
