package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify

@OptIn(UnstableApi::class)
class AudioTimingRendererTests : FunSpec({
	test("later audio advances video presentation without changing wall clock") {
		val delegate = mockk<Renderer>(relaxed = true)
		AudioTimingRenderer(delegate) { 300 }.render(10_000_000, 1234)
		verify { delegate.render(10_300_000, 1234) }
	}
	test("earlier audio and seeking use the same relative picture offset") {
		val delegate = mockk<Renderer>(relaxed = true)
		val renderer = AudioTimingRenderer(delegate) { -500 }
		renderer.render(10_000_000, 1234)
		renderer.resetPosition(10_000_000, true)
		verify { delegate.render(9_500_000, 1234) }
		verify { delegate.resetPosition(9_500_000, true) }
	}
	test("reset restores normal video timing on the next render") {
		val delegate = mockk<Renderer>(relaxed = true)
		var delay = 1000L
		val renderer = AudioTimingRenderer(delegate) { delay }
		renderer.render(5_000_000, 0)
		delay = 0
		renderer.render(5_000_000, 1)
		verify { delegate.render(6_000_000, 0) }
		verify { delegate.render(5_000_000, 1) }
	}
	test("subtitle delay combines with audio timing to keep captions aligned with picture") {
		val delegate = mockk<Renderer>(relaxed = true)
		val subtitleDelay = 200L
		val audioDelay = 500L
		SubtitleTimingRenderer(delegate) { subtitleDelay - audioDelay }.render(5_000_000, 0)
		verify { delegate.render(5_300_000, 0) }
	}
})
