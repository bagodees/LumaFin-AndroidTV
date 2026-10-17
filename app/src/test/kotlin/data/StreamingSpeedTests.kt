package org.jellyfin.androidtv.data

import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import okhttp3.Request
import org.jellyfin.androidtv.data.repository.StreamingSpeedTest
import org.jellyfin.androidtv.data.repository.measureTransfer
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class StreamingSpeedTests : FunSpec({
	test("byte bound does not consume the next byte and identifies short samples") {
		val input = ByteArrayInputStream(ByteArray(1000))
		val result = measureTransfer(input, maxBytes = 100, nanoTime = { 1 })
		result.bytes shouldBe 100
		input.available() shouldBe 900
		result.shortSample shouldBe true
	}
	test("deadline stops reads and reports correct average and interval variation") {
		var time = 0L
		val input = object : InputStream() {
			override fun read() = error("Unexpected read")
			override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
				time += 1_000_000_000
				return minOf(length, 1000)
			}
		}
		val result = measureTransfer(input, maxSeconds = 3.0, nanoTime = { time })
		result.bytes shouldBe 3000
		result.seconds shouldBe 3.0
		result.mbps shouldBe 0.008
		result.intervals shouldBe listOf(0.008, 0.008, 0.008)
	}
	test("empty transfers and stream failures do not produce successful speed results") {
		shouldThrow<IOException> { measureTransfer(ByteArrayInputStream(byteArrayOf())) }
		shouldThrow<IOException> { measureTransfer(object : InputStream() {
			override fun read(): Int = throw IOException("Unavailable")
		}) }
	}
	test("cancellation stops reading before a speed result is delivered to the UI") {
		var reads = 0
		val input = object : InputStream() {
			override fun read() = error("Unexpected read")
			override fun read(buffer: ByteArray, offset: Int, length: Int): Int { reads++; return length }
		}
		measureTransfer(input, isCancelled = { reads == 1 }).bytes shouldBe 65536
		reads shouldBe 1
	}
	test("HTTP failures HTML and redirects cannot be mistaken for media throughput") {
		val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
		var destinationCalls = 0
		server.createContext("/redirect") { exchange ->
			exchange.responseHeaders.add("Location", "/destination")
			exchange.sendResponseHeaders(302, -1); exchange.close()
		}
		server.createContext("/destination") { exchange -> destinationCalls++; exchange.close() }
		server.createContext("/html") { exchange ->
			exchange.responseHeaders.add("Content-Type", "text/html")
			exchange.sendResponseHeaders(200, 5)
			exchange.responseBody.use { it.write("hello".toByteArray()) }
		}
		server.createContext("/denied") { exchange -> exchange.sendResponseHeaders(403, -1); exchange.close() }
		server.start()
		try {
			for (path in listOf("redirect", "html", "denied")) shouldThrow<IOException> {
				StreamingSpeedTest().transfer(Request.Builder().url("http://127.0.0.1:${server.address.port}/$path").build())
			}
			destinationCalls shouldBe 0
		} finally { server.stop(0) }
	}
	test("cancelling an in-flight HTTP request releases the caller promptly") {
		val connected = CompletableDeferred<Unit>()
		val executor = Executors.newSingleThreadExecutor()
		val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { this.executor = executor }
		server.createContext("/slow") { exchange ->
			connected.complete(Unit)
			try { Thread.sleep(5000) } catch (_: InterruptedException) { }
			exchange.close()
		}
		server.start()
		try {
			coroutineScope {
				val attempt = async { StreamingSpeedTest().transfer(Request.Builder()
					.url("http://127.0.0.1:${server.address.port}/slow").build()) }
				withTimeout(2000) { connected.await(); attempt.cancel(); attempt.join() }
				attempt.isCancelled shouldBe true
			}
		} finally { server.stop(0); executor.shutdownNow() }
	}
})
