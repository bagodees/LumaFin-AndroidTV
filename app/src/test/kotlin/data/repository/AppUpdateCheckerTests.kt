package org.jellyfin.androidtv.data.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AppUpdateCheckerTests : FunSpec({
	test("a higher patch, minor or major version is newer") {
		AppUpdateChecker.isNewer("0.1.9", "0.1.8") shouldBe true
		AppUpdateChecker.isNewer("0.2.0", "0.1.9") shouldBe true
		AppUpdateChecker.isNewer("1.0.0", "0.9.9") shouldBe true
	}

	test("the same or an older version is not newer") {
		AppUpdateChecker.isNewer("0.1.8", "0.1.8") shouldBe false
		AppUpdateChecker.isNewer("0.1.7", "0.1.8") shouldBe false
		AppUpdateChecker.isNewer("0.1.10", "0.2.0") shouldBe false
	}

	test("parts are compared as numbers, not text") {
		AppUpdateChecker.isNewer("0.1.10", "0.1.9") shouldBe true
	}

	test("a stable release is newer than a pre-release of the same version") {
		AppUpdateChecker.isNewer("0.2.0", "0.2.0-beta.1") shouldBe true
		AppUpdateChecker.isNewer("0.2.0-beta.1", "0.2.0") shouldBe false
	}

	test("a v prefix and missing parts are handled") {
		AppUpdateChecker.isNewer("v0.1.9", "0.1.8") shouldBe true
		AppUpdateChecker.isNewer("0.2", "0.1.9") shouldBe true
		AppUpdateChecker.isNewer("0.2", "0.2.0") shouldBe false
	}
})
