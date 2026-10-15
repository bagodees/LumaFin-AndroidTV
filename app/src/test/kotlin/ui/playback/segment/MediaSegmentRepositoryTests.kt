package org.jellyfin.androidtv.ui.playback.segment

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType
import java.util.UUID

class MediaSegmentRepositoryTests : FunSpec({
	fun repository(saved: String): MediaSegmentRepositoryImpl {
		val preferences = mockk<UserPreferences>()
		every { preferences[UserPreferences.mediaSegmentActions] } answers { saved }
		return MediaSegmentRepositoryImpl(preferences, mockk())
	}
	fun intro(seconds: Long) = MediaSegmentDto(
		id = UUID.randomUUID(), itemId = UUID.randomUUID(), type = MediaSegmentType.INTRO,
		startTicks = 0, endTicks = seconds * 10_000_000,
	)
	test("saved intro modes select the expected playback action") {
		MediaSegmentAction.entries.forEach { action ->
			repository("Intro=${action.name},Outro=ASK_TO_SKIP").getMediaSegmentAction(intro(30)) shouldBe action
		}
	}
	test("intro choice leaves outro behavior independent") {
		val repo = repository("Intro=SKIP,Outro=ASK_TO_SKIP")
		repo.getDefaultSegmentTypeAction(MediaSegmentType.OUTRO) shouldBe MediaSegmentAction.ASK_TO_SKIP
	}
	test("very short intros do not auto skip or flash the prompt") {
		repository("Intro=SKIP").getMediaSegmentAction(intro(0)) shouldBe MediaSegmentAction.NOTHING
		repository("Intro=ASK_TO_SKIP").getMediaSegmentAction(intro(2)) shouldBe MediaSegmentAction.NOTHING
		repository("Intro=ASK_TO_SKIP").getMediaSegmentAction(intro(3)) shouldBe MediaSegmentAction.ASK_TO_SKIP
	}
	test("missing intro preference safely plays normally") {
		repository("Outro=SKIP").getMediaSegmentAction(intro(30)) shouldBe MediaSegmentAction.NOTHING
	}
	test("a changed account preference is used instead of the previous cached segment action") {
		var saved = "Intro=SKIP"
		val preferences = mockk<UserPreferences>()
		every { preferences[UserPreferences.mediaSegmentActions] } answers { saved }
		val repo = MediaSegmentRepositoryImpl(preferences, mockk())
		repo.getDefaultSegmentTypeAction(MediaSegmentType.INTRO) shouldBe MediaSegmentAction.SKIP
		saved = "Intro=NOTHING"
		repo.getDefaultSegmentTypeAction(MediaSegmentType.INTRO) shouldBe MediaSegmentAction.NOTHING
	}
})
