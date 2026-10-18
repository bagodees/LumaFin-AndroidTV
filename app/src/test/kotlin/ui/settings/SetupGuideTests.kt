package org.jellyfin.androidtv.ui.settings

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel

class SetupGuideTests : FunSpec({
	test("first-run presentation opens settings in guide mode and skipping hides it") {
		val model = SettingsViewModel()
		model.visible.value shouldBe false
		model.showSetupGuide()
		model.visible.value shouldBe true
		model.openSetupGuide shouldBe true
		model.hide()
		model.visible.value shouldBe false
	}
	test("normal settings opening after setup uses the normal entry point") {
		val model = SettingsViewModel()
		model.showSetupGuide()
		model.hide()
		model.show()
		model.visible.value shouldBe true
		model.openSetupGuide shouldBe false
	}
})
