package org.jellyfin.androidtv.playback

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.description
import org.jellyfin.androidtv.ui.playback.diagnosticSeconds
import org.jellyfin.playback.core.model.PlaybackDiagnostics

class PlaybackDiagnosticsTests : FunSpec({
	test("unknown or invalid buffer durations remain unavailable instead of looking like an empty buffer") {
		diagnosticSeconds(null) shouldBe null
		diagnosticSeconds(-1) shouldBe null
		diagnosticSeconds(Long.MIN_VALUE) shouldBe null
		diagnosticSeconds(0) shouldBe 0.0
		diagnosticSeconds(1250) shouldBe 1.25
	}
	test("unsupported counters are distinct from a measured zero and descriptions contain no stream information") {
		val context = mockk<Context>()
		every { context.getString(any()) } answers { when (firstArg<Int>()) {
			R.string.diagnostics_unavailable -> "unavailable"
			else -> "label"
		} }
		every { context.getString(any(), *anyVararg()) } answers {
			val values = secondArg<Array<out Any>>()
			"${firstArg<Int>()}:${values.joinToString()}"
		}
		val unavailable = PlaybackDiagnostics().description(context)
		unavailable.contains("${R.string.diagnostics_dropped}:unavailable") shouldBe true
		val measured = PlaybackDiagnostics(bufferedDurationMs = 0, droppedVideoFrames = 0, renderedVideoFrames = 10,
			buffering = false).description(context)
		measured.contains("${R.string.diagnostics_dropped}:0") shouldBe true
		measured.contains("${R.string.diagnostics_rendered}:10") shouldBe true
		measured.contains("http") shouldBe false
	}
})
