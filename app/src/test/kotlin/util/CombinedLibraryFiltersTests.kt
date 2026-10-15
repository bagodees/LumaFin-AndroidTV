package org.jellyfin.androidtv.ui.itemhandling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.data.model.FilterOptions
import org.jellyfin.androidtv.data.model.LibraryResolution
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import java.util.UUID

class CombinedLibraryFiltersTests : FunSpec({
	test("combined filters preserve library, letter, sort and pagination") {
		val base = GetItemsRequest(parentId = UUID.randomUUID(), nameStartsWith = "S",
			sortBy = listOf(ItemSortBy.SORT_NAME), sortOrder = listOf(SortOrder.DESCENDING), startIndex = 40, limit = 40)
		val options = FilterOptions().apply {
			genre = "Comedy"; year = 2020; minimumRating = 7
			isFavoriteOnly = true; isUnwatchedOnly = true
		}
		val result = setCombinedItemsFilter(base, options)
		result.genres shouldBe listOf("Comedy")
		result.years shouldBe listOf(2020)
		result.minCommunityRating shouldBe 7.0
		result.filters shouldBe setOf(ItemFilter.IS_FAVORITE, ItemFilter.IS_UNPLAYED)
		result.parentId shouldBe base.parentId
		result.nameStartsWith shouldBe "S"
		result.sortOrder shouldBe base.sortOrder
		result.startIndex shouldBe 40
	}
	test("reset removes selected filters and restores baseline scope") {
		val base = GetItemsRequest(genres = listOf("Drama"), years = listOf(2000), minCommunityRating = 3.0)
		val changed = setCombinedItemsFilter(base, FilterOptions().apply { genre = "Comedy"; year = 2020; minimumRating = 8 })
		val reset = setCombinedItemsFilter(changed, FilterOptions(), base)
		reset.genres shouldBe base.genres
		reset.years shouldBe base.years
		reset.minCommunityRating shouldBe 3.0
		reset.filters shouldBe emptySet()
	}
	test("clearing one dimension retains the other selections") {
		val base = GetItemsRequest()
		val options = FilterOptions().apply { genre = "Drama"; year = 2020; minimumRating = 7 }
		val changed = setCombinedItemsFilter(base, options)
		options.genre = null
		val result = setCombinedItemsFilter(changed, options, base)
		result.genres shouldBe null
		result.years shouldBe listOf(2020)
		result.minCommunityRating shouldBe 7.0
	}
	test("resolution combines with metadata filters and respects an existing width scope") {
		val base = GetItemsRequest(minWidth = 2000, maxWidth = 3000, nameStartsWith = "S")
		val options = FilterOptions().apply {
			resolution = LibraryResolution.FULL_HD; genre = "Drama"; isUnwatchedOnly = true
		}
		val result = setCombinedItemsFilter(base, options)
		result.minWidth shouldBe 2000
		result.maxWidth shouldBe 3000
		result.genres shouldBe listOf("Drama")
		result.filters shouldBe setOf(ItemFilter.IS_UNPLAYED)
		result.nameStartsWith shouldBe "S"
	}
	test("Any resolution clears prior width bounds while keeping selected genre and rating") {
		val base = GetItemsRequest()
		val options = FilterOptions().apply { resolution = LibraryResolution.UHD; genre = "Drama"; minimumRating = 7 }
		val filtered = setCombinedItemsFilter(base, options)
		options.resolution = LibraryResolution.ANY
		val result = setCombinedItemsFilter(filtered, options, base)
		result.minWidth shouldBe null
		result.maxWidth shouldBe null
		result.genres shouldBe listOf("Drama")
		result.minCommunityRating shouldBe 7.0
	}
	test("SD excludes unknown video width and changing resolution removes the previous upper bound") {
		val base = GetItemsRequest()
		val options = FilterOptions().apply { resolution = LibraryResolution.SD }
		val sd = setCombinedItemsFilter(base, options)
		(sd.minWidth!! > 0) shouldBe true
		options.resolution = LibraryResolution.UHD
		val uhd = setCombinedItemsFilter(sd, options, base)
		uhd.maxWidth shouldBe null
		(uhd.minWidth!! > sd.maxWidth!!) shouldBe true
	}
})
