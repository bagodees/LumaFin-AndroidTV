package org.jellyfin.androidtv.preference

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.util.UUID

class SettingsBackupTests : FunSpec({
	fun backup(settings: String, version: Int = 1) = """{"format":"lumafin-settings","version":$version,"settings":{$settings}}"""
	fun defaults() = SettingsBackup.snapshot { null }

	test("all supported types round trip and export excludes credentials identifiers and device settings") {
		val private = mapOf<String, Any>("access_token" to "secret-token", "server_address" to "private-address",
			"external_player_component" to "private-component", "hdr10_override" to "DISABLE", "user_id" to "private-user",
			"subtitle_brightness" to 80, "subtitles_text_size" to 30f, "homeSectionOrder" to "resume,nextup")
		val snapshot = SettingsBackup.snapshot { private[it] }
		val encoded = SettingsBackup.encode(snapshot)
		SettingsBackup.review(encoded).values shouldBe snapshot
		listOf("secret-token", "private-address", "private-component", "private-user", "hdr10_override").forEach { encoded shouldNotContain it }
	}
	test("home style defaults to the side menu, round trips and rejects unknown styles") {
		defaults()["home_style"] shouldBe "SIDE_MENU"
		SettingsBackup.review(backup(""""home_style":"TOP_BAR"""")).values["home_style"] shouldBe "TOP_BAR"
		shouldThrowAny { SettingsBackup.review(backup(""""home_style":"GRID"""")) }
	}
	test("partial restore retains omissions and reports unsupported fields without applying them") {
		val review = SettingsBackup.review(backup(""""subtitle_brightness":60,"access_token":"ignore","new_future_setting":true"""))
		review.values shouldBe mapOf("subtitle_brightness" to 60)
		review.skipped shouldBe 2
	}
	test("invalid known values types home rows and versions reject the complete backup") {
		listOf(
			""""subtitle_brightness":0""", """"subtitle_brightness":"80""", """"subtitle_brightness":null""",
			""""subtitle_brightness":80,"subtitles_text_size":999""", """"subtitles_text_size":"NaN""",
			""""homeSectionOrder":"resume,resume""", """"homeSectionOrder":"unknown""", """"homeSectionOrder":"""""",
			""""app_theme":"UNKNOWN""", """"subtitles_text_color":4294967296""", """"skipBackLength":-1""",
		).forEach { shouldThrowAny { SettingsBackup.review(backup(it)) } }
		shouldThrowAny { SettingsBackup.review(backup(""""subtitle_brightness":80""", 2)) }
		shouldThrowAny { SettingsBackup.review("not-json") }
		shouldThrowAny { SettingsBackup.review(backup(""""only_unknown":true""")) }
	}
	test("oversized and deeply nested files fail before parsing") {
		shouldThrowAny { SettingsBackup.review(" ".repeat(SettingsBackup.MAX_BYTES + 1)) }
		shouldThrowAny { SettingsBackup.review("[".repeat(10000) + "]".repeat(10000)) }
	}
	test("blank legacy order captures the effective default layout") {
		SettingsBackup.snapshot { if (it == "homeSectionOrder") "" else null } shouldBe defaults()
	}
	test("restore merges only supported values into the selected account and survives recreation") {
		val legacy = MemoryPreferences()
		val files = mutableMapOf<String, MemoryPreferences>()
		fun create() = AccountPreferences(legacy, { files.getOrPut(it) { MemoryPreferences() } })
		val server = UUID(0, 1); val user = UUID(0, 2); val other = UUID(0, 3)
		val accounts = create()
		accounts.select(server, user)
		accounts.preferences.edit().putString("hdr10_override", "DISABLE").putString("unrelated", "retain").apply()
		val target = accounts.backupTarget()
		accounts.restoreBackup(target, SettingsBackup.review(backup(""""subtitle_brightness":60,"homeSectionOrder":"resume,nextup","unknown":0""")))
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 60
		accounts.preferences.getString("unrelated", null) shouldBe "retain"
		accounts.preferences.getString("hdr10_override", null) shouldBe "DISABLE"
		accounts.preferences.contains("unknown") shouldBe false
		accounts.select(server, other)
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 100
		create().also { it.select(server, user) }.preferences.getInt("subtitle_brightness", 100) shouldBe 60
	}
	test("switching away and back invalidates a pending review and guest cannot restore") {
		val files = mutableMapOf<String, MemoryPreferences>()
		val accounts = AccountPreferences(MemoryPreferences(), { files.getOrPut(it) { MemoryPreferences() } })
		val server = UUID(0, 1); val user = UUID(0, 2)
		shouldThrowAny { accounts.backupTarget() }
		accounts.select(server, user)
		val target = accounts.backupTarget()
		accounts.select(server, UUID(0, 3))
		val restore = SettingsBackup.review(backup(""""subtitle_brightness":60"""))
		shouldThrowAny { accounts.restoreBackup(target, restore) }
		accounts.select(server, user)
		shouldThrowAny { accounts.restoreBackup(target, restore) }
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 100
	}
	test("changed preferences invalidate review and arbitrary keys cannot cross write boundary") {
		val accounts = AccountPreferences(MemoryPreferences(), { MemoryPreferences() })
		accounts.select(UUID(0, 1), UUID(0, 2))
		val target = accounts.backupTarget()
		accounts.preferences.edit().putInt("subtitle_brightness", 80).apply()
		shouldThrowAny { accounts.restoreBackup(target, SettingsBackup.review(backup(""""subtitle_brightness":60"""))) }
		shouldThrowAny { accounts.restoreBackup(accounts.backupTarget(), SettingsBackup.Review(mapOf("subtitle_brightness" to 60, "access_token" to "bad"), 0)) }
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 80
		accounts.preferences.contains("access_token") shouldBe false
	}
	test("failed disk commit rolls memory back and does not leave a half restore") {
		val files = mutableMapOf<String, MemoryPreferences>()
		val accounts = AccountPreferences(MemoryPreferences(), { files.getOrPut(it) { MemoryPreferences() } })
		accounts.select(UUID(0, 1), UUID(0, 2))
		accounts.preferences.edit().putInt("subtitle_brightness", 80).apply()
		val target = accounts.backupTarget()
		files.values.forEach { it.failNextCommit = true }
		shouldThrowAny { accounts.restoreBackup(target, SettingsBackup.review(backup(""""subtitle_brightness":60,"subtitles_text_size":30"""))) }
		accounts.backupTarget().values shouldBe target.values
		accounts.preferences.contains("subtitles_text_size") shouldBe false
	}
})
