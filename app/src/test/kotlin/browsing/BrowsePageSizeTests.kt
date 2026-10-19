package org.jellyfin.androidtv.browsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.browsing.browsePageSize

class BrowsePageSizeTests : FunSpec({
	test("ordinary grids fetch a viewport and a spare row instead of a hundred cards") {
		browsePageSize(25, 18, 6) shouldBe 25
		browsePageSize(25, 24, 6) shouldBe 30
		browsePageSize(25, 32, 4) shouldBe 36
	}
	test("dense grids remain bounded and nonpaged queries stay nonpaged") {
		browsePageSize(25, 200, 10) shouldBe 150
		browsePageSize(25, 0, 0) shouldBe 25
		browsePageSize(0, 30, 5) shouldBe 0
		browsePageSize(50, 20, 5) shouldBe 50
	}
})
