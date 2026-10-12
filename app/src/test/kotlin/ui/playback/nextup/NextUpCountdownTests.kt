package org.jellyfin.androidtv.ui.playback.nextup

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.nextUpCountdownDuration

class NextUpCountdownTests : FunSpec({
	test("existing zero timeout keeps waiting for Watch now") {
		nextUpCountdownDuration(0) shouldBe null
	}
	test("immediate mode starts without a countdown") {
		nextUpCountdownDuration(-1) shouldBe 0
	}
	test("countdown preserves default and selected durations within supported bounds") {
		nextUpCountdownDuration(7_000) shouldBe 7_000
		nextUpCountdownDuration(10_000) shouldBe 10_000
		nextUpCountdownDuration(1) shouldBe 1_000
		nextUpCountdownDuration(60_000) shouldBe 30_000
	}
})
