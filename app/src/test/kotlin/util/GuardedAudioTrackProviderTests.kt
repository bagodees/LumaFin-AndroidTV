package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuardedAudioTrackProviderTests : FunSpec({
	test("normal playback heads pass through") {
		guardPlaybackHead(0) shouldBe 0
		guardPlaybackHead(2_034_176) shouldBe 2_034_176
	}
	test("a bogus negative head at start reads as nothing played yet") {
		guardPlaybackHead(Int.MIN_VALUE + 2_560) shouldBe 0
		guardPlaybackHead(-1) shouldBe 0
	}
})
