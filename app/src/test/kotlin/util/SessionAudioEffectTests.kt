package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.playback.SessionAudioEffect

class SessionAudioEffectTests : FunSpec({
	test("replacing a session releases the old effect before creating the next") {
		val events = mutableListOf<String>()
		val owner = SessionAudioEffect { id -> events += "create $id"; AutoCloseable { events += "close $id" } }
		owner.attach(1); owner.attach(1); owner.attach(2); owner.close(); owner.close()
		events shouldBe listOf("create 1", "close 1", "create 2", "close 2")
	}
	test("unsupported devices can retry without interrupting playback") {
		var attempts = 0
		var closed = false
		val owner = SessionAudioEffect { if (++attempts == 1) error("unsupported"); AutoCloseable { closed = true } }
		owner.attach(1); owner.attach(1); owner.close()
		attempts shouldBe 2
		closed shouldBe true
	}
	test("invalid sessions release the old effect and never create a global effect") {
		val ids = mutableListOf<Int>()
		var closes = 0
		val owner = SessionAudioEffect { ids += it; AutoCloseable { closes++ } }
		owner.attach(0); owner.attach(-1); owner.attach(1); owner.attach(0)
		ids shouldBe listOf(1)
		closes shouldBe 1
	}
	test("player owners are independent and a failing release cannot retain ownership") {
		var closes = 0
		val first = SessionAudioEffect { AutoCloseable { closes++; error("device release failure") } }
		val second = SessionAudioEffect { AutoCloseable { closes++ } }
		first.attach(1); second.attach(2); first.close(); first.close()
		closes shouldBe 1
		second.close()
		closes shouldBe 2
	}
})
