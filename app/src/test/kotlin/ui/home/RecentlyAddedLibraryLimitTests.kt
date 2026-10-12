package org.jellyfin.androidtv.ui.home

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.CollectionType

class RecentlyAddedLibraryLimitTests : FunSpec({
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
