package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.itemhandling.setItemsStartLetter
import org.jellyfin.androidtv.ui.itemhandling.setArtistsStartLetter
import org.jellyfin.androidtv.ui.itemhandling.setAlbumArtistsStartLetter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetArtistsRequest
import org.jellyfin.sdk.model.api.request.GetAlbumArtistsRequest

class AlphabetInputTests : FunSpec({
	test("letter input supports non-Latin and combining characters") {
		AlphabetInput.normalize(" Ж ") shouldBe "Ж"
		AlphabetInput.normalize("界") shouldBe "界"
		AlphabetInput.normalize("e\u0301") shouldBe "é"
		AlphabetInput.normalize("5") shouldBe "5"
		AlphabetInput.normalize("") shouldBe null
		AlphabetInput.normalize("AB") shouldBe null
		AlphabetInput.normalize("!") shouldBe null
	}
	test("numeric range replaces prefix and All clears range without changing sorting or filters") {
		val request = GetItemsRequest(sortBy = setOf(ItemSortBy.SORT_NAME), sortOrder = setOf(SortOrder.DESCENDING), isFavorite = true)
		val numbers = setItemsStartLetter(request, "#")
		numbers.nameStartsWith shouldBe null
		numbers.nameStartsWithOrGreater shouldBe "0"
		numbers.nameLessThan shouldBe ":"
		numbers.sortBy shouldBe request.sortBy
		numbers.sortOrder shouldBe request.sortOrder
		numbers.isFavorite shouldBe true
		val letter = setItemsStartLetter(numbers, "Ж")
		letter.nameStartsWith shouldBe "Ж"
		letter.nameLessThan shouldBe null
		letter.nameStartsWithOrGreater shouldBe null
		setItemsStartLetter(letter, null).nameStartsWith shouldBe null
	}
	test("artist and album artist queries use the same numeric range and clear behavior") {
		val artists = setArtistsStartLetter(GetArtistsRequest(), "#")
		artists.nameLessThan shouldBe ":"
		setArtistsStartLetter(artists, null).nameStartsWithOrGreater shouldBe null
		val albumArtists = setAlbumArtistsStartLetter(GetAlbumArtistsRequest(), "#")
		albumArtists.nameLessThan shouldBe ":"
		setAlbumArtistsStartLetter(albumArtists, "A").nameStartsWith shouldBe "A"
	}
})
