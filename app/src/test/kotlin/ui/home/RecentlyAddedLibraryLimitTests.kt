package org.jellyfin.androidtv.ui.home

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.CollectionType

class RecentlyAddedLibraryLimitTests : FunSpec({
	test("movie library query includes both watch states when toggle is off") {
		val parent = java.util.UUID.randomUUID()
		val query = recentlyAddedMovieLibraryRequest(parent, 5, false)
		query.parentId shouldBe parent
		query.filters shouldBe emptySet()
		query.isPlayed shouldBe null
		query.limit shouldBe 5
		query.startIndex shouldBe 0
		query.enableTotalRecordCount shouldBe false
	}
	test("movie library query filters unwatched before applying the item limit") {
		val query = recentlyAddedMovieLibraryRequest(java.util.UUID.randomUUID(), 10, true)
		query.filters shouldBe setOf(org.jellyfin.sdk.model.api.ItemFilter.IS_UNPLAYED)
		query.limit shouldBe 10
		query.includeItemTypes shouldBe setOf(org.jellyfin.sdk.model.api.BaseItemKind.MOVIE)
	}
	test("movie and TV libraries use their independent home limits") {
		recentlyAddedLibraryLimit(CollectionType.MOVIES, 5, 15) shouldBe 5
		recentlyAddedLibraryLimit(CollectionType.TVSHOWS, 5, 15) shouldBe 15
	}
	test("other libraries keep their existing limit") {
		recentlyAddedLibraryLimit(CollectionType.MUSIC, 5, 15) shouldBe 50
		recentlyAddedLibraryLimit(null, 5, 15) shouldBe 50
	}
	test("out of range preferences stay within supported limits") {
		recentlyAddedLibraryLimit(CollectionType.MOVIES, 0, 15) shouldBe 5
		recentlyAddedLibraryLimit(CollectionType.TVSHOWS, 5, 100) shouldBe 50
	}
})
