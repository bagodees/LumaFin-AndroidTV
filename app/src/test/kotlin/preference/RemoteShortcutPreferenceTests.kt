package org.jellyfin.androidtv.preference

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.preference.constant.CardLongPressAction
import java.util.UUID

class RemoteShortcutPreferenceTests : FunSpec({
	test("long press defaults to the existing menu and persists independently per server and user") {
		val files = mutableMapOf<String, MemoryPreferences>()
		val legacy = MemoryPreferences()
		fun accounts() = AccountPreferences(legacy, { files.getOrPut(it) { MemoryPreferences() } })
		val store = accounts()
		val preferences = UserPreferences(mockk<Context>(), store.preferences)
		val server = UUID(0, 1); val user = UUID(0, 2)
		store.select(server, user)
		preferences[UserPreferences.cardLongPressAction] shouldBe CardLongPressAction.MENU
		preferences[UserPreferences.cardLongPressAction] = CardLongPressAction.DISABLED
		store.select(server, UUID(0, 3))
		preferences[UserPreferences.cardLongPressAction] shouldBe CardLongPressAction.MENU
		preferences[UserPreferences.cardLongPressAction] = CardLongPressAction.DETAILS
		store.select(UUID(0, 4), user)
		preferences[UserPreferences.cardLongPressAction] shouldBe CardLongPressAction.MENU
		val recreated = accounts().also { it.select(server, user) }
		UserPreferences(mockk<Context>(), recreated.preferences)[UserPreferences.cardLongPressAction] shouldBe CardLongPressAction.DISABLED
	}
	test("backup round trips the selected action and older backups leave it untouched") {
		val values = SettingsBackup.snapshot { if (it == "card_long_press_action") "PLAY" else null }
		SettingsBackup.review(SettingsBackup.encode(values)).values["card_long_press_action"] shouldBe "PLAY"
		val old = SettingsBackup.review("""{"format":"lumafin-settings","version":1,"settings":{"subtitle_brightness":80}}""")
		old.values.containsKey("card_long_press_action") shouldBe false
	}
})
