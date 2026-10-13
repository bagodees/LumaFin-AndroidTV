package org.jellyfin.androidtv.ui.itemhandling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class SeriesBrowseFocusTests : FunSpec({
	fun episode(played: Boolean = false) = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE,
		userData = mockk<UserItemDataDto> { every { this@mockk.played } returns played })
	test("return focus follows stable episode ID after server reorders results") {
		val first = episode()
		val chosen = episode()
		seriesFocusIndex(listOf(chosen, first), chosen.id, 1, null) shouldBe 0
	}
	test("autoplay restores the last played episode in the available season") {
		val first = episode(true)
		val next = episode()
		seriesFocusIndex(listOf(first, next), first.id, 0, next) shouldBe 1
	}
	test("initial entry selects first unwatched; all watched and empty lists are safe") {
		seriesFocusIndex(listOf(episode(true), episode()), null, 0, null) shouldBe 1
		seriesFocusIndex(listOf(episode(true), episode(true)), null, 0, null) shouldBe 0
		seriesFocusIndex(emptyList(), null, 0, null) shouldBe null
	}
	test("deleted episode falls back near the old selection and unrelated playback does not change it") {
		val first = episode()
		val second = episode()
		seriesFocusIndex(listOf(first, second), UUID.randomUUID(), 6, episode()) shouldBe 1
		seriesFocusIndex(listOf(first, second), first.id, 0, episode()) shouldBe 0
	}
	test("series selection follows the season of the last played episode") {
		val season = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.SEASON)
		val otherSeason = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.SEASON)
		val played = episode().copy(seasonId = season.id)
		seriesFocusIndex(listOf(otherSeason, season), otherSeason.id, 0, played) shouldBe 1
	}
})
