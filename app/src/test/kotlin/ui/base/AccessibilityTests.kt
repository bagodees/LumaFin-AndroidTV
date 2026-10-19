package org.jellyfin.androidtv.ui.base

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jellyfin.androidtv.preference.constant.TextScale
import org.jellyfin.design.Tokens

class AccessibilityTests : FunSpec({
	test("text sizes are 100, 115 and 130 percent, in increasing order") {
		TextScale.entries.map { it.factor } shouldBe listOf(1f, 1.15f, 1.3f)
		TextScale.entries.first() shouldBe TextScale.DEFAULT
	}

	test("high contrast brightens secondary text and is stable when applied twice") {
		val normal = colorScheme()
		val contrast = normal.highContrast()
		contrast.listCaption shouldNotBe normal.listCaption
		contrast.listHeadline shouldBe Tokens.Color.colorWhite
		contrast.onBackground shouldBe Tokens.Color.colorWhite
		contrast.highContrast() shouldBe contrast
		// Unrelated brand colors are left alone.
		contrast.badge shouldBe normal.badge
		contrast.rangeControlFill shouldBe normal.rangeControlFill
	}

	test("focused buttons keep readable contrast in high contrast mode") {
		val contrast = colorScheme().highContrast()
		contrast.buttonFocused shouldBe Tokens.Color.colorWhite
		contrast.onButtonFocused shouldBe Tokens.Color.colorBlack
	}
})
