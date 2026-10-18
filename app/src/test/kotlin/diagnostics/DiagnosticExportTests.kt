package org.jellyfin.androidtv.diagnostics

import android.content.ContentResolver
import android.net.Uri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.BufferLength
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import org.jellyfin.androidtv.preference.constant.StreamingQuality
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

private fun snapshot() = DiagnosticSnapshot("0.0.0-dev.1", 35, "12.2.0", true, false, true,
	StreamingQuality.ORIGINAL, 100, BufferLength.AUTO, false, AudioBehavior.entries.first(),
	RefreshRateSwitchingBehavior.DISABLED, true, false, true, null, false)

class DiagnosticExportTests : FunSpec({
	test("unexpected version fields are dropped whole, including injected lines and secret URLs") {
		listOf("https://user:password@private.example/?api_key=secret", "12.2.0\nToken: secret", "name@example.com",
			"00000000-0000-0000-0000-000000000001", "12.2.0-token-secret", "C:\\private\\file", "1.2.3".repeat(30), "12.2.0 ").forEach { value ->
			diagnosticVersion(value) shouldBe "Unavailable (omitted)"
			val text = diagnosticReport(snapshot().copy(appVersion = value, serverVersion = value), DiagnosticProblem.CONNECTION, DiagnosticTrigger.START).diagnosticText()
			text.contains(value) shouldBe false
		}
	}
	test("numeric versions and known release suffixes remain useful without arbitrary text") {
		listOf("12.2.0", "0.0.0-dev.1", "1.0-beta.2", "1.0-rc.1").forEach { diagnosticVersion(it) shouldBe it }
		diagnosticVersion(null) shouldBe "Unavailable (omitted)"
	}
	test("unknown counters and negative network validation are not reported as success") {
		val text = diagnosticReport(snapshot(), DiagnosticProblem.BUFFERING, DiagnosticTrigger.SEEK).diagnosticText()
		text.contains("HEVC/H.265 advertised: Unavailable") shouldBe true
		text.contains("AV1 advertised: No") shouldBe true
		text.contains("Android reports internet validated: No") shouldBe true
		text.contains("Live buffer, frame and stream metrics are not captured") shouldBe true
		text.contains("Problem: BUFFERING\nOccurs during: SEEK") shouldBe true
	}
	test("custom bitrate is bounded and excluded when not in use") {
		fun text(limit: Int?, quality: StreamingQuality) = diagnosticReport(snapshot().copy(customBitrateMbps = limit, quality = quality),
			DiagnosticProblem.VIDEO, DiagnosticTrigger.PLAYING).diagnosticText()
		text(100, StreamingQuality.ORIGINAL).contains("Custom limit (Mbit/s): Not used") shouldBe true
		text(20, StreamingQuality.CUSTOM).contains("Custom limit (Mbit/s): 20") shouldBe true
		listOf(null, -1, 0, Int.MAX_VALUE).forEach { text(it, StreamingQuality.CUSTOM).contains("Custom limit (Mbit/s): Unavailable") shouldBe true }
	}
	test("saved UTF-8 text matches every reviewed section exactly and stream is closed") {
		val resolver = mockk<ContentResolver>()
		val uri = mockk<Uri>()
		var closed = false
		val output = object : ByteArrayOutputStream() { override fun close() { closed = true; super.close() } }
		every { resolver.openOutputStream(uri, "wt") } returns output
		val report = diagnosticReport(snapshot(), DiagnosticProblem.SUBTITLES, DiagnosticTrigger.RESUME)
		val text = report.diagnosticText()
		writeDiagnosticExport(resolver, uri, text)
		output.toString("UTF-8") shouldBe text
		report.forEach { text.contains(it.heading + "\n" + it.body) shouldBe true }
		closed shouldBe true
	}
	test("missing output and write failures fail instead of reporting a successful export") {
		val resolver = mockk<ContentResolver>()
		val uri = mockk<Uri>()
		every { resolver.openOutputStream(uri, "wt") } returns null
		shouldThrow<IOException> { writeDiagnosticExport(resolver, uri, "reviewed") }
		var closed = false
		every { resolver.openOutputStream(uri, "wt") } returns object : OutputStream() {
			override fun write(b: Int) { throw IOException("private URL must never reach UI") }
			override fun close() { closed = true }
		}
		shouldThrow<IOException> { writeDiagnosticExport(resolver, uri, "reviewed") }
		closed shouldBe true
	}
})
