package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.ui.browsing.compactDurationMinutes
import org.jellyfin.androidtv.ui.browsing.compactProgress
import org.jellyfin.androidtv.ui.browsing.supportsCompactList
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class CompactLibraryTests : FunSpec({
	fun item(duration: Long? = null, percent: Double? = null, position: Long = 0, watched: Boolean = false) = BaseItemDto(
		id = UUID(0, 1), type = org.jellyfin.sdk.model.api.BaseItemKind.MOVIE, runTimeTicks = duration,
		userData = mockk<UserItemDataDto> {
			every { played } returns watched
			every { playedPercentage } returns percent
			every { playbackPositionTicks } returns position
		},
	)
	test("missing and invalid durations remain absent and short valid durations remain readable") {
		listOf(null, 0L, -1L).forEach { compactDurationMinutes(item(it)) shouldBe null }
		compactDurationMinutes(item(1)) shouldBe 1
		compactDurationMinutes(item(600_000_000L * 125)) shouldBe 125
	}
	test("progress uses watched state then finite server percentage then valid position") {
		compactProgress(item(percent = 25.0, watched = true)) shouldBe 1f
		compactProgress(item(percent = 50.0)) shouldBe .5f
		compactProgress(item(1000, position = 250)) shouldBe .25f
		compactProgress(item()) shouldBe null
		compactProgress(item(0, position = 250)) shouldBe null
	}
	test("corrupt progress cannot overflow a row or fabricate a watched flag") {
		compactProgress(item(percent = 150.0)) shouldBe 1f
		compactProgress(item(1000, percent = Double.NaN, position = 250)) shouldBe .25f
		compactProgress(item(percent = Double.POSITIVE_INFINITY)) shouldBe null
		compactProgress(item(1000, position = Long.MAX_VALUE)) shouldBe 1f
		compactProgress(item(percent = -1.0)) shouldBe null
	}
	test("list is offered only for movie and TV libraries") {
		supportsCompactList(CollectionType.MOVIES) shouldBe true
		supportsCompactList(CollectionType.TVSHOWS) shouldBe true
		supportsCompactList(CollectionType.MUSIC) shouldBe false
		supportsCompactList(null) shouldBe false
	}
})
