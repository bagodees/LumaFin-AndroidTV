package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify

@OptIn(UnstableApi::class)
class SubtitleTimingRendererTests : FunSpec({
	test("later subtitles use an earlier subtitle clock without shifting wall time") {
		val delegate = mockk<Renderer>(relaxed = true)
		SubtitleTimingRenderer(delegate) { 1500 }.render(10_000_000, 1234)
		verify { delegate.render(8_500_000, 1234) }
	}
	test("earlier subtitles and seeking use the same adjusted clock") {
		val delegate = mockk<Renderer>(relaxed = true)
		val renderer = SubtitleTimingRenderer(delegate) { -500 }
		renderer.render(10_000_000, 1234)
		renderer.resetPosition(10_000_000, true)
		verify { delegate.render(10_500_000, 1234) }
		verify { delegate.resetPosition(10_500_000, true) }
	}
	test("changing and resetting timing is reflected on the next render") {
		val delegate = mockk<Renderer>(relaxed = true)
		var delay = 1000L
		val renderer = SubtitleTimingRenderer(delegate) { delay }
		renderer.render(5_000_000, 0)
		delay = 0
		renderer.render(5_000_000, 1)
		verify { delegate.render(4_000_000, 0) }
		verify { delegate.render(5_000_000, 1) }
	}
})
