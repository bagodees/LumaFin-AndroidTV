package org.jellyfin.androidtv.util.profile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.StreamingQuality
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.ProfileConditionType
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

class StreamingQualityProfileTests : FunSpec({
	val existing = buildDeviceProfile {
		codecProfile {
			type = CodecType.VIDEO
			codec = "h264"
			conditions { ProfileConditionValue.WIDTH lowerThanOrEquals 1920 }
		}
	}
	test("every resolution preset caps width and height for all video codecs without replacing device restrictions") {
		for (quality in listOf(StreamingQuality.UHD, StreamingQuality.FULL_HD, StreamingQuality.HD, StreamingQuality.SD)) {
			val result = existing.withStreamingQuality(quality)
			result.codecProfiles.drop(1) shouldBe existing.codecProfiles
			val cap = result.codecProfiles.first()
			cap.type shouldBe CodecType.VIDEO
			cap.codec shouldBe null
			cap.conditions.map { it.property to it.value } shouldBe listOf(
				ProfileConditionValue.WIDTH to quality.maxWidth.toString(),
				ProfileConditionValue.HEIGHT to quality.maxHeight.toString(),
			)
			cap.conditions.all { it.condition == ProfileConditionType.LESS_THAN_EQUAL } shouldBe true
		}
	}
	test("original and custom keep device compatibility restrictions without adding a resolution limit") {
		existing.withStreamingQuality(StreamingQuality.ORIGINAL) shouldBe existing
		existing.withStreamingQuality(StreamingQuality.CUSTOM) shouldBe existing
	}
	test("original has no quality bitrate cap and resolution presets use decreasing bandwidth budgets") {
		StreamingQuality.ORIGINAL.bitrate shouldBe Int.MAX_VALUE
		StreamingQuality.CUSTOM.bitrate shouldBe null
		listOf(StreamingQuality.UHD, StreamingQuality.FULL_HD, StreamingQuality.HD, StreamingQuality.SD).map { it.bitrate } shouldBe listOf(40_000_000, 12_000_000, 6_000_000, 2_000_000)
	}
})
