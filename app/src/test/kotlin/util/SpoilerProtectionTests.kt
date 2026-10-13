package org.jellyfin.androidtv.util

import io.mockk.mockk
import io.mockk.every
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class SpoilerProtectionTests : FunSpec({
	fun item(type: BaseItemKind, played: Boolean?) = BaseItemDto(
		id = UUID.randomUUID(), type = type,
		userData = played?.let { value -> mockk<UserItemDataDto> { every { this@mockk.played } returns value } },
	)
	test("disabled protection shows unwatched episode descriptions") {
		shouldHideEpisodeDescription(item(BaseItemKind.EPISODE, false), false) shouldBe false
	}
	test("enabled protection hides unwatched and unknown watch states") {
		shouldHideEpisodeDescription(item(BaseItemKind.EPISODE, false), true) shouldBe true
		shouldHideEpisodeDescription(item(BaseItemKind.EPISODE, null), true) shouldBe true
	}
	test("watched episodes show their description") {
		shouldHideEpisodeDescription(item(BaseItemKind.EPISODE, true), true) shouldBe false
	}
	test("movies and series descriptions are unaffected") {
		shouldHideEpisodeDescription(item(BaseItemKind.MOVIE, false), true) shouldBe false
		shouldHideEpisodeDescription(item(BaseItemKind.SERIES, false), true) shouldBe false
	}
	test("artwork protection covers unwatched and unknown episodes only") {
		shouldHideEpisodeArtwork(item(BaseItemKind.EPISODE, false), true) shouldBe true
		shouldHideEpisodeArtwork(item(BaseItemKind.EPISODE, null), true) shouldBe true
		shouldHideEpisodeArtwork(item(BaseItemKind.EPISODE, true), true) shouldBe false
		shouldHideEpisodeArtwork(item(BaseItemKind.EPISODE, false), false) shouldBe false
		shouldHideEpisodeArtwork(item(BaseItemKind.MOVIE, false), true) shouldBe false
		shouldHideEpisodeArtwork(item(BaseItemKind.SERIES, false), true) shouldBe false
	}
	test("protected artwork prefers series thumbnail over poster") {
		val series = UUID.randomUUID()
		val episode = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE,
			seriesId = series, seriesThumbImageTag = "thumb", seriesPrimaryImageTag = "poster")
		safeSeriesArtwork(episode)?.item shouldBe series
		safeSeriesArtwork(episode)?.type shouldBe org.jellyfin.sdk.model.api.ImageType.THUMB
	}
	test("protected artwork falls back to series poster and never episode artwork") {
		val series = UUID.randomUUID()
		val episode = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE,
			seriesId = series, seriesPrimaryImageTag = "poster")
		safeSeriesArtwork(episode)?.item shouldBe series
		safeSeriesArtwork(episode)?.type shouldBe org.jellyfin.sdk.model.api.ImageType.PRIMARY
		safeSeriesArtwork(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE,
			imageTags = mapOf(org.jellyfin.sdk.model.api.ImageType.PRIMARY to "episode"))) shouldBe null
	}
})
