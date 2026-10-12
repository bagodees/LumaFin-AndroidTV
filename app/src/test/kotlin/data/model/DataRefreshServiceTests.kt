package org.jellyfin.androidtv.data.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.Instant
import java.util.UUID

class DataRefreshServiceTests : FunSpec({
	test("episode completion retains a refresh signal for Home while it is away") {
		val service = DataRefreshService()
		val now = Instant.parse("2026-10-12T12:00:00Z")
		service.playbackReportFinished(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE), now)
		service.lastTvPlayback shouldBe now
		service.lastPlayback shouldBe now
		service.lastMoviePlayback shouldBe null
		service.playbackUpdates.value shouldBe 1L
		service.playbackReportFinished(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE), now)
		service.playbackUpdates.value shouldBe 2L
	}
	test("movie completion leaves TV refresh timestamp unchanged") {
		val service = DataRefreshService()
		val now = Instant.parse("2026-10-12T12:00:00Z")
		service.playbackReportFinished(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE), now)
		service.lastMoviePlayback shouldBe now
		service.lastTvPlayback shouldBe null
		service.playbackUpdates.value shouldBe 1L
	}
})
