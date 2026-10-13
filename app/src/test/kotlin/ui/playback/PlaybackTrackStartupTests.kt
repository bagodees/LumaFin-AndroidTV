package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class PlaybackTrackStartupTests : FunSpec({
	test("details choice survives repeated startup attempts until prepared") {
		val id = UUID.randomUUID()
		val startup = PlaybackTrackStartup()
		PlaybackTrackSelection.setSubtitle(id, 0)
		PlaybackTrackSelection.setAudio(id, 2)
		startup.get(id) shouldBe PlaybackTrackSelection.Selection(2, 0)
		PlaybackTrackSelection.get(id) shouldBe null
		startup.get(id) shouldBe PlaybackTrackSelection.Selection(2, 0)
		startup.prepared()
		startup.get(id) shouldBe null
	}
	test("an explicit off choice survives retries without leaking to another episode") {
		val id = UUID.randomUUID()
		val startup = PlaybackTrackStartup()
		PlaybackTrackSelection.setSubtitle(id, PlaybackTrackSelection.SUBTITLES_OFF)
		startup.get(id)?.subtitleIndex shouldBe -1
		startup.get(id)?.subtitleIndex shouldBe -1
		startup.get(UUID.randomUUID()) shouldBe null
	}
})
