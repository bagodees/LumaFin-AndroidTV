package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

class AvailableAlphabetTests : FunSpec({
	test("only matching choices remain in alphabet order, without duplicate probes") {
		val calls = mutableListOf<Char>()
		availableAlphabet("#AKZKA") { letter ->
			calls += letter
			letter in "#K"
		} shouldBe "#K"
		calls.sorted() shouldBe listOf('#', 'A', 'K', 'Z')
	}
	test("no matches produces an empty alphabet, including no numeric choice") {
		availableAlphabet("#AZЖ") { false } shouldBe ""
	}
	test("failed lookups are reported rather than hiding titles as empty") {
		val result = runCatching { availableAlphabet("AZ") { error("Server unavailable") } }
		result.isFailure shouldBe true
	}
	test("server requests are bounded to four concurrent probes") {
		val active = AtomicInteger()
		val maximum = AtomicInteger()
		availableAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ") {
			val count = active.incrementAndGet()
			maximum.updateAndGet { maxOf(it, count) }
			delay(5)
			active.decrementAndGet()
			true
		} shouldBe "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
		(maximum.get() <= 4) shouldBe true
	}
})
