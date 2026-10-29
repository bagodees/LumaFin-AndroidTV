package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuardedAudioTrackProviderTests : FunSpec({
	fun guard() = PlaybackHeadGuard(maxJumpFrames = 480_000)

	test("normal playback heads pass through") {
		val guard = guard()
		guard.filter(0) shouldBe 0
		guard.filter(2_560) shouldBe 2_560
		guard.filter(115_200) shouldBe 115_200
		guard.corrected shouldBe false
	}
	test("a step back keeps the head moving forward so Media3 does not count a wrap-around") {
		val guard = guard()
		guard.filter(100_000) shouldBe 100_000
		guard.filter(99_000) shouldBe 100_000
		guard.corrected shouldBe true
		guard.filter(101_000) shouldBe 102_000
	}
	test("a platform reset to zero without a flush carries on from the last head") {
		val guard = guard()
		guard.filter(300_000) shouldBe 300_000
		guard.filter(0) shouldBe 300_000
		guard.filter(48_000) shouldBe 348_000
	}
	test("a bogus forward jump is ignored") {
		val guard = guard()
		guard.filter(2_560) shouldBe 2_560
		guard.filter(2_147_484_945L.toInt()) shouldBe 2_560
		guard.filter(5_120) shouldBe 5_120
	}
	test("a negative reading at start is ignored") {
		val guard = guard()
		guard.filter(-1) shouldBe 0
		guard.filter(1_000) shouldBe 1_000
	}
	test("a head that stays far ahead after a flush becomes the new start instead of freezing") {
		val guard = guard()
		guard.reset()
		// The platform still counts from before the seek
		guard.filter(100_002_000) shouldBe 0
		guard.filter(100_004_000) shouldBe 0
		guard.filter(100_006_000) shouldBe 0
		guard.filter(100_054_000) shouldBe 48_000
		guard.filter(100_102_000) shouldBe 96_000
		guard.corrected shouldBe false
	}
	test("a flush lets the head start again from zero") {
		val guard = guard()
		guard.filter(400_000) shouldBe 400_000
		guard.reset()
		guard.filter(0) shouldBe 0
		guard.filter(48_000) shouldBe 48_000
	}
})
