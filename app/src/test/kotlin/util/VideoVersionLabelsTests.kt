package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType

class VideoVersionLabelsTests : FunSpec({
	fun source(width: Int?, height: Int?, range: VideoRangeType = VideoRangeType.UNKNOWN, fallback: VideoRange = VideoRange.UNKNOWN): MediaSourceInfo {
		val video = mockk<MediaStream> {
			every { type } returns MediaStreamType.VIDEO
			every { this@mockk.width } returns width
			every { this@mockk.height } returns height
			every { videoRangeType } returns range
			every { videoRange } returns fallback
		}
		return mockk { every { mediaStreams } returns listOf(video) }
	}
	test("cropped widescreen versions retain recognizable resolution labels") {
		source(3840, 1600, VideoRangeType.HDR10).versionVideoDetails() shouldBe "4K · HDR10"
		source(1920, 800, VideoRangeType.SDR).versionVideoDetails() shouldBe "1080p · SDR"
		source(1280, 536).versionVideoDetails() shouldBe "720p"
	}
	test("Dolby Vision and HDR10 plus are distinguished") {
		source(3840, 2160, VideoRangeType.DOVI_WITH_HDR10).versionVideoDetails() shouldBe "4K · Dolby Vision / HDR10"
		source(3840, 2160, VideoRangeType.HDR10_PLUS).versionVideoDetails() shouldBe "4K · HDR10+"
	}
	test("missing metadata does not invent resolution or dynamic range") {
		source(null, null).versionVideoDetails() shouldBe ""
		source(null, null, fallback = VideoRange.HDR).versionVideoDetails() shouldBe "HDR"
		mockk<MediaSourceInfo> { every { mediaStreams } returns null }.versionVideoDetails() shouldBe ""
	}
})
