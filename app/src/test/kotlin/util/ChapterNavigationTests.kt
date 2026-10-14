package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChapterNavigationTests : FunSpec({
	test("chapter focus includes first chapter and exact boundaries") {
		val starts = longArrayOf(0, 100, 200)
		ChapterNavigation.indexAt(starts, 0) shouldBe 0
		ChapterNavigation.indexAt(starts, 99) shouldBe 0
		ChapterNavigation.indexAt(starts, 100) shouldBe 1
		ChapterNavigation.indexAt(starts, 999) shouldBe 2
	}
	test("missing chapters and a position before the first chapter are safe") {
		ChapterNavigation.indexAt(longArrayOf(), 0) shouldBe -1
		ChapterNavigation.indexAt(longArrayOf(100, 200), 0) shouldBe 0
	}
	test("server chapter order does not change the current chapter calculation") {
		ChapterNavigation.indexAt(longArrayOf(200, 0, 100), 150) shouldBe 2
	}
})
