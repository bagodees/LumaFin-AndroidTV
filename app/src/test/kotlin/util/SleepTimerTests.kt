package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class SleepTimerTests : FunSpec({
	test("timer expires at elapsed deadline independently of media position") {
		val timer = SleepTimerState()
		timer.set(15, 1000, null)
		timer.expired(900999) shouldBe false
		timer.expired(901000) shouldBe true
	}
	test("changing duration replaces deadline and off cancels it") {
		val timer = SleepTimerState()
		timer.set(15, 0, null)
		timer.set(30, 5000, null)
		timer.expired(900000) shouldBe false
		timer.deadline shouldBe 1805000L
		timer.cancel()
		timer.expired(Long.MAX_VALUE) shouldBe false
	}
	test("episode end targets the selected episode without a timed deadline") {
		val id = UUID.randomUUID()
		val timer = SleepTimerState()
		timer.set(-1, 0, id)
		timer.endsWithEpisode(id) shouldBe true
		timer.endsWithEpisode(UUID.randomUUID()) shouldBe false
		timer.expired(Long.MAX_VALUE) shouldBe false
		timer.cancel()
		timer.endsWithEpisode(id) shouldBe false
	}
})
