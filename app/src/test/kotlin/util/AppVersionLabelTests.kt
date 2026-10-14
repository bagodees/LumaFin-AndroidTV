package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AppVersionLabelTests : FunSpec({
	test("builds between releases show the upcoming version with dev and debug markers") {
		AppVersionLabel.format("0.0.0-dev.1", "debug", "0.2.0") shouldBe "LumaFin 0.2.0 dev debug"
	}
	test("a released version shows its own version without dev or debug markers") {
		AppVersionLabel.format("0.2.0", "release", "0.3.0") shouldBe "LumaFin 0.2.0 release"
	}
})
