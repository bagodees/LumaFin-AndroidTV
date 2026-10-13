package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class ContinueWatchingRemovalTests : FunSpec({
	test("confirmed removal clears only the selected item and refreshes Home after success") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)
		val mutations = mockk<ItemMutationRepository>()
		val messages = mockk<CustomMessageRepository>(relaxed = true)
		val refresh = DataRefreshService()
		coEvery { mutations.setPlayed(item.id, false) } returns mockk()
		removeContinueWatching(item, mutations, refresh, messages)
		coVerify(exactly = 1) { mutations.setPlayed(item.id, false) }
		refresh.playbackUpdates.value shouldBe 1L
		verify(exactly = 1) { messages.pushMessage(CustomMessage.RefreshCurrentItem) }
	}
	test("server failure leaves Home unchanged and does not report removal") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE)
		val mutations = mockk<ItemMutationRepository>()
		val messages = mockk<CustomMessageRepository>(relaxed = true)
		val refresh = DataRefreshService()
		coEvery { mutations.setPlayed(item.id, false) } throws IllegalStateException("server unavailable")
		shouldThrow<IllegalStateException> { removeContinueWatching(item, mutations, refresh, messages) }
		refresh.playbackUpdates.value shouldBe 0L
		verify(exactly = 0) { messages.pushMessage(any()) }
	}
})
