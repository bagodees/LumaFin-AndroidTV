package org.jellyfin.androidtv.util.profile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.Codec

class PassthroughAudioCodecTests : FunSpec({
	test("Dolby passthrough formats come first so TrueHD converts to surround, not AAC") {
		preferPassthroughAudioCodecs(listOf(Codec.Audio.AAC, Codec.Audio.AC3, Codec.Audio.EAC3, Codec.Audio.MP3)).toList() shouldBe
			listOf(Codec.Audio.EAC3, Codec.Audio.AC3, Codec.Audio.AAC, Codec.Audio.MP3)
	}
	test("AC3 leads when it is the only Dolby passthrough format") {
		preferPassthroughAudioCodecs(listOf(Codec.Audio.AAC, Codec.Audio.AC3, Codec.Audio.MP3)).toList() shouldBe
			listOf(Codec.Audio.AC3, Codec.Audio.AAC, Codec.Audio.MP3)
	}
	test("without Dolby passthrough the original order is kept") {
		preferPassthroughAudioCodecs(listOf(Codec.Audio.AAC, Codec.Audio.MP3, Codec.Audio.FLAC, Codec.Audio.DTS)).toList() shouldBe
			listOf(Codec.Audio.AAC, Codec.Audio.MP3, Codec.Audio.FLAC, Codec.Audio.DTS)
	}
})
