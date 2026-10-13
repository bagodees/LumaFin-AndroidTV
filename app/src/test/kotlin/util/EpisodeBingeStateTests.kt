package org.jellyfin.androidtv.ui

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EpisodeBingeStateTests : FunSpec({
	test("asks after exactly the selected episode count") {
		for (limit in listOf(2, 3, 5)) {
			val state = EpisodeBingeState()
			repeat(limit - 1) { state.completeEpisode() }
			state.shouldAsk(limit) shouldBe false
			state.completeEpisode()
			state.shouldAsk(limit) shouldBe true
		}
	}
	test("off never interrupts a binge") {
		val state = EpisodeBingeState()
		repeat(10) { state.completeEpisode() }
		state.shouldAsk(0) shouldBe false
	}
	test("confirmation or a new session starts a fresh count") {
		val state = EpisodeBingeState()
		repeat(3) { state.completeEpisode() }
		state.reset()
		state.completeEpisode()
		state.shouldAsk(2) shouldBe false
		state.completeEpisode()
		state.shouldAsk(2) shouldBe true
	}
})
