package org.jellyfin.androidtv.ui.search

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jellyfin.androidtv.ui.home.SeerrItem
import org.jellyfin.sdk.model.api.BaseItemKind

data class SearchState(
	val query: String = "",
	val loading: Boolean = false,
	val completed: Boolean = false,
	val groups: List<SearchResultGroup> = emptyList(),
	val failedGroups: List<Int> = emptyList(),
	/** Titles Seerr found that aren't in the library, shown last so they can be requested. */
	val seerr: List<SeerrItem> = emptyList(),
) {
	val hasResults get() = groups.any { it.items.isNotEmpty() } || seerr.isNotEmpty()
}

/** Seerr's titles without the movies and shows the library search already found (same name, and year when both have one). */
internal fun notInLibrary(seerr: List<SeerrItem>, groups: List<SearchResultGroup>): List<SeerrItem> {
	val library = groups.flatMap { it.items }.filter { it.type == BaseItemKind.MOVIE || it.type == BaseItemKind.SERIES }
	return seerr.filter { title ->
		library.none { item ->
			item.name.equals(title.name, ignoreCase = true) &&
				(item.productionYear == null || title.year == null || item.productionYear == title.year)
		}
	}
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
