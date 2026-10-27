package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuardedAudioTrackProviderTests : FunSpec({
	val tenSeconds = 480_000L

	test("normal playback heads pass through") {
		val guard = PlaybackHeadGuard(tenSeconds)
		guard.filter(0) shouldBe 0
		guard.filter(2_560) shouldBe 2_560
		guard.filter(115_200) shouldBe 115_200
	}
	test("the bogus near-2^31 reading seen on a Sony X90J is ignored") {
		val guard = PlaybackHeadGuard(tenSeconds)
		guard.filter(2_560) shouldBe 2_560
		guard.filter(2_147_484_945L.toInt()) shouldBe 2_560
		guard.filter(2_147_480_000) shouldBe 2_560
		guard.filter(5_120) shouldBe 5_120
	}
	test("a negative reading at start is ignored") {
		val guard = PlaybackHeadGuard(tenSeconds)
		guard.filter(-1) shouldBe 0
		guard.filter(1_000) shouldBe 1_000
	}
	test("the head may move back after a flush") {
		val guard = PlaybackHeadGuard(tenSeconds)
		guard.filter(400_000) shouldBe 400_000
		guard.filter(0) shouldBe 0
		guard.filter(48_000) shouldBe 48_000
	}
})
