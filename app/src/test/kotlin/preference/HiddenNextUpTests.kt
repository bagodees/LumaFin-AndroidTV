package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class HiddenNextUpTests : FunSpec({
	test("hidden shows round trip and invalid storage is ignored") {
		val show = UUID.randomUUID().toString()
		val encoded = HiddenNextUp.encode(mapOf(show to HiddenNextUpShow("Pioneer One", 1000)))
		HiddenNextUp.decode(encoded) shouldBe mapOf(show to HiddenNextUpShow("Pioneer One", 1000))
		HiddenNextUp.decode("not JSON") shouldBe emptyMap()
		HiddenNextUp.decode("""{"not-a-uuid":{"name":"Gone","hiddenAt":1}}""") shouldBe emptyMap()
		HiddenNextUp.isValid(encoded) shouldBe true
		HiddenNextUp.isValid("[1,2]") shouldBe false
		HiddenNextUp.isValid(5) shouldBe false
	}
	test("settings backup carries the hidden list and rejects a malformed one") {
		val show = UUID.randomUUID().toString()
		val value = HiddenNextUp.encode(mapOf(show to HiddenNextUpShow("Show", 1)))
		SettingsBackup.snapshot { null }["hidden_next_up"] shouldBe "{}"
		val backup = SettingsBackup.encode(SettingsBackup.snapshot { if (it == "hidden_next_up") value else null })
		SettingsBackup.review(backup).values["hidden_next_up"] shouldBe value
		SettingsSyncModel.syncedKeys.contains("hidden_next_up") shouldBe true
	}
	test("episodes of hidden shows are filtered by series") {
		val hidden = UUID.randomUUID()
		val kept = UUID.randomUUID()
		val items = listOf(
			BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seriesId = hidden),
			BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seriesId = kept),
		)
		items.filterNot { it.seriesId?.toString() in setOf(hidden.toString()) }.map { it.seriesId } shouldBe listOf(kept)
	}
})
