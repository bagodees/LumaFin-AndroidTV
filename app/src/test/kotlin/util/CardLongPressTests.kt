package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.CardLongPressAction
import org.jellyfin.androidtv.ui.home.HomeShortcutRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.cardLongPressAction
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class CardLongPressTests : FunSpec({
	test("each configured action applies to movies episodes and series") {
		listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES).forEach { type ->
			val card = BaseItemDtoBaseRowItem(BaseItemDto(id = UUID(0, 1), type = type))
			CardLongPressAction.entries.forEach { action -> cardLongPressAction(card, action) shouldBe action }
		}
	}
	test("library queue music and other card menus keep their behavior even when holds are disabled") {
		listOf(BaseItemKind.USER_VIEW, BaseItemKind.COLLECTION_FOLDER, BaseItemKind.MUSIC_ALBUM,
			BaseItemKind.AUDIO, BaseItemKind.TV_CHANNEL, BaseItemKind.SEASON, BaseItemKind.BOX_SET).forEach { type ->
			val card = BaseItemDtoBaseRowItem(BaseItemDto(id = UUID(0, 1), type = type))
			CardLongPressAction.entries.forEach { action -> cardLongPressAction(card, action) shouldBe CardLongPressAction.MENU }
		}
	}
	test("an unavailable pinned title always retains its recovery menu") {
		val item = BaseItemDto(id = UUID(0, 1), type = BaseItemKind.MOVIE)
		CardLongPressAction.entries.forEach { action ->
			cardLongPressAction(HomeShortcutRowItem(item, false), action) shouldBe CardLongPressAction.MENU
		}
		cardLongPressAction(HomeShortcutRowItem(item, true), CardLongPressAction.DETAILS) shouldBe CardLongPressAction.DETAILS
	}
})
