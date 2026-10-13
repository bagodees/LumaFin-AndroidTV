package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.Locale

class TrackLabelsTests : FunSpec({
	val text = TrackLabelText("Unknown Language", "Mono", "Stereo", "%1\$d Channels", "Forced Only", "Hearing Impaired (SDH)", "Default", "Track %1\$d")
	fun stream(index: Int = 1, language: String? = "eng", codec: String? = "aac", channels: Int? = 2,
		type: MediaStreamType = MediaStreamType.AUDIO, title: String? = null, layout: String? = null,
		forced: Boolean = false, sdh: Boolean = false, default: Boolean = false, display: String? = null,
	) = mockk<MediaStream> {
		every { this@mockk.index } returns index
		every { this@mockk.language } returns language
		every { this@mockk.codec } returns codec
		every { this@mockk.channels } returns channels
		every { this@mockk.type } returns type
		every { this@mockk.title } returns title
		every { channelLayout } returns layout
		every { isForced } returns forced
		every { isHearingImpaired } returns sdh
		every { isDefault } returns default
		every { displayTitle } returns display
	}
	test("languages and audio formats are readable and commentary is preserved") {
		stream(title = "Director Commentary").readableTrackLabel(Locale.ENGLISH, text) shouldBe
			"English · Director Commentary · Stereo · AAC"
		stream(language = "fre", codec = "eac3", channels = 6, layout = "5.1").readableTrackLabel(Locale.ENGLISH, text) shouldBe
			"French · 5.1 · Dolby Digital Plus"
	}
	test("channel count does not invent a surround layout") {
		stream(channels = 6).readableTrackLabel(Locale.ENGLISH, text) shouldBe "English · 6 Channels · AAC"
	}
	test("subtitle flags reflect server metadata") {
		stream(type = MediaStreamType.SUBTITLE, codec = "subrip", forced = true, sdh = true, default = true)
			.readableTrackLabel(Locale.ENGLISH, text) shouldBe "English · SRT · Forced Only · Hearing Impaired (SDH) · Default"
		stream(type = MediaStreamType.SUBTITLE, codec = "subrip").readableTrackLabel(Locale.ENGLISH, text) shouldBe "English · SRT"
	}
	test("missing metadata falls back to server label") {
		stream(language = null, codec = null, channels = null, display = "Special Track")
			.readableTrackLabel(Locale.ENGLISH, text) shouldBe "Special Track"
	}
	test("identical labels remain distinguishable and preserve stream indices") {
		listOf(stream(index = 2), stream(index = 7)).readableTrackLabels(Locale.ENGLISH, text) shouldBe
			mapOf(2 to "English · Stereo · AAC · Track 3", 7 to "English · Stereo · AAC · Track 8")
	}
})
