package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.sdk.api.client.ApiClient

class CompactListPreferenceTests : FunSpec({
	test("compact list defaults off and does not overwrite independent grid choices or another library") {
		val api = mockk<ApiClient>()
		val movies = LibraryPreferences("movies", api)
		val shows = LibraryPreferences("shows", api)
		movies[LibraryPreferences.compactList] shouldBe false
		movies[LibraryPreferences.gridDirection] = GridDirection.VERTICAL
		movies[LibraryPreferences.posterSize] = PosterSize.SMALL
		movies[LibraryPreferences.compactList] = true
		shows[LibraryPreferences.compactList] shouldBe false
		movies[LibraryPreferences.compactList] = false
		movies[LibraryPreferences.gridDirection] shouldBe GridDirection.VERTICAL
		movies[LibraryPreferences.posterSize] shouldBe PosterSize.SMALL
	}
	test("the existing account-switch cache clear removes the previous user's list choice") {
		val library = LibraryPreferences("same-library", mockk())
		library[LibraryPreferences.compactList] = true
		library.clearCache()
		library[LibraryPreferences.compactList] shouldBe false
	}
})
