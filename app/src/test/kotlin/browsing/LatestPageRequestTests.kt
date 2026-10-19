package org.jellyfin.androidtv.browsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import org.jellyfin.androidtv.ui.itemhandling.LatestPageRequest
import java.io.IOException

class LatestPageRequestTests : FunSpec({
	test("a retry with a changed account does not send the old query under the new identity") { coroutineScope {
		val request = LatestPageRequest()
		var identity = 1
		var attempts = 0
		request.load<Int>(this, { attempts++; throw IOException("offline") }, { identity == 1 }, {}, {})
		yield()
		identity = 2
		request.retry(); yield()
		attempts shouldBe 1
		request.canRetry shouldBe false
	} }
	test("closing a failed query discards its retry action") { coroutineScope {
		val request = LatestPageRequest()
		var attempts = 0
		request.load<Int>(this, { attempts++; throw IOException("offline") }, { true }, {}, {})
		yield()
		request.canRetry shouldBe true
		request.cancel()
		request.canRetry shouldBe false
		request.retry(); yield()
		attempts shouldBe 1
	} }
	test("a slow superseded page cannot overwrite the next query or clear its busy state") { coroutineScope {
		val request = LatestPageRequest()
		val waiting = CompletableDeferred<Int>()
		val applied = mutableListOf<Int>()
		val finished = mutableListOf<Exception?>()
		request.load(this, { try { waiting.await() } catch (_: CancellationException) { 1 } }, { true }, applied::add, finished::add)
		yield()
		request.load(this, { 2 }, { true }, applied::add, finished::add)
		yield()
		applied shouldBe listOf(2)
		finished shouldBe listOf(null)
	} }
	test("leaving the view cancels pending work without publishing a result") { coroutineScope {
		val request = LatestPageRequest()
		val waiting = CompletableDeferred<Int>()
		var published = false
		request.load(this, { waiting.await() }, { true }, { published = true }, { published = true })
		yield(); request.cancel(); yield()
		published shouldBe false
	} }
	test("account or server identity changes reject a delayed page") { coroutineScope {
		val request = LatestPageRequest()
		val waiting = CompletableDeferred<Int>()
		var identity = 1
		var published = false
		var failure: Exception? = null
		request.load(this, { waiting.await() }, { identity == 1 }, { published = true }, { failure = it })
		yield(); identity = 2; waiting.complete(1); yield()
		published shouldBe false
		(failure is CancellationException) shouldBe true
	} }
	test("a failed later page retains earlier results and the same offset can be retried") { coroutineScope {
		val request = LatestPageRequest()
		val rows = mutableListOf(0, 1, 2)
		var failures = 0
		var attempts = 0
		var starts = 0
		request.load(this, { if (++attempts == 1) throw IOException("offline") else listOf(3, 4) },
			{ true }, rows::addAll, { if (it != null) failures++ }, { starts++ })
		yield()
		rows shouldBe listOf(0, 1, 2)
		failures shouldBe 1
		request.retry()
		yield()
		rows shouldBe listOf(0, 1, 2, 3, 4)
		attempts shouldBe 2
		starts shouldBe 2
		failures shouldBe 1
	} }
})
