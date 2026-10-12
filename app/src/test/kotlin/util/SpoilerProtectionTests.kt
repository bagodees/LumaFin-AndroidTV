package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class SpoilerProtectionTests : FunSpec({
	fun item(type: BaseItemKind, played: Boolean?) = BaseItemDto(
		id = UUID.randomUUID(), type = type,
		userData = played?.let { UserItemDataDto(played = it) },
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
})
