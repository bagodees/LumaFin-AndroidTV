package io.github.bagodees.lumafin.core.seerr

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SeerrRequestsTests : FunSpec({
	fun json(text: String) = Json.parseToJsonElement(text).jsonObject

	test("a request without its title or artwork asks for Seerr's details") {
		val requests = parseRequests(json("""
			{"requests":[
				{"tmdbId":1,"type":"movie","title":"Unknown"},
				{"tmdbId":2,"type":"tv"},
				{"tmdbId":3,"type":"movie","title":"Known","posterUrl":"/poster.jpg"}
			]}
		"""))
		requests.map { it.needsDetails } shouldBe listOf(true, true, false)
	}

	test("an unknown request takes the title, year and artwork from Seerr's details") {
		val item = parseRequests(json("""{"requests":[{"tmdbId":7,"type":"tv","title":"Unknown"}]}""")).single()
		val filled = item.withDetails(json("""{"name":"The Show","firstAirDate":"2021-04-02","posterPath":"/p.jpg","backdropPath":"/b.jpg"}"""))
		filled.name shouldBe "The Show"
		filled.year shouldBe 2021
		filled.posterUrl shouldBe "https://image.tmdb.org/t/p/w342/p.jpg"
		filled.imageUrl shouldBe "https://image.tmdb.org/t/p/w780/b.jpg"
		filled.needsDetails shouldBe false
	}

	test("what SeerrFin did send is kept") {
		val item = parseRequests(json("""{"requests":[{"tmdbId":7,"type":"movie","title":"Mine","year":1999}]}""")).single()
		val filled = item.withDetails(json("""{"title":"Theirs","releaseDate":"2001-01-01","posterPath":"/p.jpg"}"""))
		filled.name shouldBe "Mine"
		filled.year shouldBe 1999
		filled.posterUrl shouldBe "https://image.tmdb.org/t/p/w342/p.jpg"
		filled.imageUrl shouldBe "https://image.tmdb.org/t/p/w342/p.jpg"
	}
})
