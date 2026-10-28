package org.jellyfin.androidtv.ui.home

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate

class SeerrMissingSeasonsTests : FunSpec({
	val today = LocalDate.of(2026, 10, 28)
	fun details(text: String) = Json.parseToJsonElement(text).jsonObject

	test("aired seasons Seerr doesn't have or know about are missing") {
		missingSeasons(details("""
			{"mediaInfo":{"status":4,"seasons":[{"seasonNumber":1,"status":5},{"seasonNumber":2,"status":5},{"seasonNumber":3,"status":2}]},
			 "seasons":[{"seasonNumber":0,"airDate":"2015-01-01"},{"seasonNumber":1,"airDate":"2015-09-01"},{"seasonNumber":2,"airDate":"2016-09-01"},
			            {"seasonNumber":3,"airDate":"2017-09-01"},{"seasonNumber":5,"airDate":"2019-09-01"},{"seasonNumber":4,"airDate":"2018-09-01"}]}
		"""), today) shouldBe listOf(4, 5)
	}
	test("specials and seasons that haven't aired aren't offered") {
		missingSeasons(details("""
			{"mediaInfo":{"status":4,"seasons":[{"seasonNumber":1,"status":5}]},
			 "seasons":[{"seasonNumber":0,"airDate":"2015-01-01"},{"seasonNumber":1,"airDate":"2025-01-01"},
			            {"seasonNumber":2,"airDate":"2027-01-01"},{"seasonNumber":3}]}
		"""), today) shouldBe emptyList()
	}
	test("only shows Seerr knows as partly available offer seasons") {
		val seasons = """"seasons":[{"seasonNumber":1,"airDate":"2015-09-01"},{"seasonNumber":2,"airDate":"2016-09-01"}]"""
		missingSeasons(details("""{$seasons}"""), today) shouldBe emptyList()
		missingSeasons(details("""{"mediaInfo":{"status":5,"seasons":[]},$seasons}"""), today) shouldBe emptyList()
		missingSeasons(details("""{"mediaInfo":{"status":4,"seasons":[{"seasonNumber":1,"status":5}]},$seasons}"""), today) shouldBe listOf(2)
	}
})
