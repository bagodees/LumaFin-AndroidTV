package org.jellyfin.androidtv.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.legacyVideoQueueEditor
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class VideoQueueEditorTests : FunSpec({
	test("legacy view distinguishes current from upcoming and edits the reviewed snapshot only") {
		val items = List(4) { BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, name = "Video $it") }
		val controller = mockk<PlaybackController>()
		every { controller.videoQueueSnapshot } returns items
		every { controller.currentQueueIndex } returns 1
		every { controller.editUpcomingQueue(items, 1, 2, null) } returns true
		val view = legacyVideoQueueEditor(controller).snapshot(1)
		view.currentTitle shouldBe "Video 1"
		view.upcoming.map { it.title } shouldBe listOf("Video 2")
		view.hasMore shouldBe true
		view.edit(2, null) shouldBe true
		verify(exactly = 1) { controller.editUpcomingQueue(items, 1, 2, null) }
	}
	test("last item has no upcoming videos") {
		val controller = mockk<PlaybackController>()
		every { controller.videoQueueSnapshot } returns listOf(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, name = "Only video"))
		every { controller.currentQueueIndex } returns 0
		val view = legacyVideoQueueEditor(controller).snapshot(50)
		view.upcoming.isEmpty() shouldBe true
		view.hasMore shouldBe false
	}
	test("session queue edits retain remembered track languages and queue position") {
		val manager = VideoQueueManager()
		val items = List(3) { BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE) }
		manager.setCurrentVideoQueue(items)
		manager.setLastPlayedAudioLanguageIsoCode("eng")
		manager.setLastPlayedSubtitleLanguageIsoCode("spa")
		manager.updateCurrentVideoQueue(listOf(items[0], items[2]), 0)
		manager.getCurrentVideoQueue() shouldBe listOf(items[0], items[2])
		manager.getCurrentMediaPosition() shouldBe 0
		manager.getLastPlayedAudioLanguageIsoCode() shouldBe "eng"
		manager.getLastPlayedSubtitleLanguageIsoCode() shouldBe "spa"
	}
})
