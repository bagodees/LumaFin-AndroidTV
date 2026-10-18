package org.jellyfin.androidtv.ui.search

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jellyfin.sdk.model.api.BaseItemKind

data class SearchState(
	val query: String = "",
	val loading: Boolean = false,
	val completed: Boolean = false,
	val groups: List<SearchResultGroup> = emptyList(),
	val failedGroups: List<Int> = emptyList(),
) {
	val hasResults get() = groups.any { it.items.isNotEmpty() }
}

/** Await one bounded request per category; retain ordered successes and identify failed categories. */
internal suspend fun searchGroups(
	query: String,
	groups: Map<Int, Set<BaseItemKind>>,
	repository: SearchRepository,
): SearchState = coroutineScope {
	val results = groups.map { (label, kinds) ->
		async { label to repository.search(query, kinds) }
	}.awaitAll()
	SearchState(
		query = query,
		completed = true,
		groups = results.mapNotNull { (label, result) ->
			result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { SearchResultGroup(label, it) }
		},
		failedGroups = results.filter { it.second.isFailure }.map { it.first },
	)
}
