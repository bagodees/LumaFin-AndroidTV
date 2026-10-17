package org.jellyfin.androidtv.ui.playback.overlay

import androidx.leanback.widget.DirectionalSeek
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.util.SeekAcceleration

class SeekAccelerationTests : FunSpec({
	test("fresh presses never inherit acceleration") {
		SeekAcceleration.multiplier(0, 6000) shouldBe 1
		SeekAcceleration.multiplier(0, 0) shouldBe 1
	}
	test("holds accelerate at elapsed-time boundaries independently of repeat rate") {
		for (repeats in listOf(1, 10, 100)) {
			SeekAcceleration.multiplier(repeats, 999) shouldBe 1
			SeekAcceleration.multiplier(repeats, 1000) shouldBe 2
			SeekAcceleration.multiplier(repeats, 2999) shouldBe 2
			SeekAcceleration.multiplier(repeats, 3000) shouldBe 4
			SeekAcceleration.multiplier(repeats, 4999) shouldBe 4
			SeekAcceleration.multiplier(repeats, 5000) shouldBe 8
		}
	}
	test("accelerated seeking remains bounded at both ends") {
		val multiplier = SeekAcceleration.multiplier(50, 8000)
		DirectionalSeek.targetPosition(170_000, 180_000, 30_000L * multiplier, true) shouldBe 180_000L
		DirectionalSeek.targetPosition(10_000, 180_000, 10_000L * multiplier, false) shouldBe 0L
	}
})
