package org.jellyfin.androidtv.ui.settings

import org.jellyfin.androidtv.ui.settings.search.SettingsSearchEntry
import org.jellyfin.androidtv.ui.settings.search.searchSettings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SettingsSearchTests : FunSpec({
	val entries = listOf(
		SettingsSearchEntry("cache", "/maintenance", "Maintenance", "Settings", "Clear downloaded artwork cache"),
		SettingsSearchEntry("subtitle", "/subtitles", "Subtitles", "Playback", "Customize captions, brightness and text size", "100%"),
		SettingsSearchEntry("size", "/subtitles", "Subtitle text size", "Subtitles", "Change caption size", "24 sp"),
		SettingsSearchEntry("theme", "/theme", "Thème", "Appearance", "Change colors"),
	)
	test("matches synonyms and multiple words in any order") {
		searchSettings(entries, "artwork CACHE").map { it.id } shouldBe listOf("cache")
		searchSettings(entries, "brightness captions").map { it.id } shouldBe listOf("subtitle")
		searchSettings(entries, "captions unknown").isEmpty() shouldBe true
	}
	test("ranks titles before description matches without collapsing shared destinations") {
		searchSettings(entries, "size").map { it.id } shouldBe listOf("size", "subtitle")
	}
	test("normalizes accents whitespace and case") {
		searchSettings(entries, "  THEME  ").single().id shouldBe "theme"
		searchSettings(entries, " captions\n  size ") shouldBe searchSettings(entries, "captions size")
	}
	test("empty query is deterministic and values are not search keywords") {
		searchSettings(entries, " ").size shouldBe entries.size
		searchSettings(entries.reversed(), "") shouldBe searchSettings(entries, "")
		searchSettings(entries, "100%").isEmpty() shouldBe true
	}
	test("format-specific results retain distinct navigation parameters and current values") {
		val formats = listOf(
			SettingsSearchEntry("hdr10", "/hdr/{format}", "HDR10", "HDR", "Device support", "Auto", mapOf("format" to "HDR10")),
			SettingsSearchEntry("hdr10plus", "/hdr/{format}", "HDR10+", "HDR", "Device support", "Disable", mapOf("format" to "HDR10PLUS")),
		)
		val results = searchSettings(formats, "HDR10")
		results.map { it.parameters["format"] } shouldBe listOf("HDR10", "HDR10PLUS")
		results.map { it.currentValue } shouldBe listOf("Auto", "Disable")
	}
})
