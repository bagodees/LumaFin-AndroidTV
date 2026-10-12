package org.jellyfin.androidtv.ui.playback.overlay

import androidx.leanback.widget.DirectionalSeek
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DirectionalSeekTests : FunSpec({
	test("default forward and backward intervals stay independent when changing direction") {
		val forward = DirectionalSeek.targetPosition(60_000, 180_000, 30_000, true)
		forward shouldBe 90_000L
		val backward = DirectionalSeek.targetPosition(forward, 180_000, 10_000, false)
		backward shouldBe 80_000L
		DirectionalSeek.targetPosition(backward, 180_000, 30_000, true) shouldBe 110_000L
	}

	test("custom intervals are relative to the current position rather than a shared grid") {
		DirectionalSeek.targetPosition(62_500, 180_000, 5_000, false) shouldBe 57_500L
		DirectionalSeek.targetPosition(62_500, 180_000, 20_000, true) shouldBe 82_500L
	}

	test("seeking stops at the beginning and end") {
		DirectionalSeek.targetPosition(5_000, 180_000, 10_000, false) shouldBe 0L
		DirectionalSeek.targetPosition(175_000, 180_000, 30_000, true) shouldBe 180_000L
		DirectionalSeek.targetPosition(0, 180_000, 10_000, false) shouldBe 0L
		DirectionalSeek.targetPosition(180_000, 180_000, 30_000, true) shouldBe 180_000L
	}

	test("preview positions cover both intervals even when neither divides the other") {
		DirectionalSeek.previewInterval(10_000, 30_000) shouldBe 10_000L
		DirectionalSeek.previewInterval(10_000, 15_000) shouldBe 5_000L
		DirectionalSeek.previewInterval(30_000, 10_000) shouldBe 10_000L
	}
})
