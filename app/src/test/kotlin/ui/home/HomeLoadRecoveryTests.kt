package org.jellyfin.androidtv.ui.home

import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import java.io.IOException

class HomeLoadRecoveryTests : FunSpec({
	test("temporary Home failure recovers without reopening") {
		var calls = 0
		val waits = mutableListOf<Long>()
		recoverHomeLoad({ true }, { waits.add(it) }) {
			if (++calls < 3) throw IOException("Offline")
			"cards"
		} shouldBe "cards"
		calls shouldBe 3
		waits shouldBe listOf(2_000L, 5_000L)
	}
	test("healthy or legitimately empty Home does not retry") {
		var calls = 0
		recoverHomeLoad({ true }, { error("Unexpected retry") }) { calls++; emptyList<String>() } shouldBe emptyList()
		calls shouldBe 1
	}
	test("persistent failure stops after a bounded number of attempts") {
		var calls = 0
		shouldThrow<IOException> {
			recoverHomeLoad({ true }, {}) { calls++; throw IOException("Offline") }
		}
		calls shouldBe 4
	}
	test("leaving Home or switching accounts cancels a pending retry") {
		var current = true
		var calls = 0
		shouldThrow<CancellationException> {
			recoverHomeLoad({ current }, { current = false }) { calls++; throw IOException("Offline") }
		}
		calls shouldBe 1
	}
	test("a response arriving after the account changes is discarded") {
		var current = true
		shouldThrow<CancellationException> {
			recoverHomeLoad({ current }, { error("Unexpected retry") }) { current = false; "old account" }
		}
	}
	test("cancellation is never retried or shown as a network failure") {
		var calls = 0
		shouldThrow<CancellationException> {
			recoverHomeLoad({ true }, { error("Unexpected retry") }) { calls++; throw CancellationException() }
		}
		calls shouldBe 1
	}
})
