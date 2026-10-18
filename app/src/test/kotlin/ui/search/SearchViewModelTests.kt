package org.jellyfin.androidtv.ui.search

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

private class SearchRepositoryFake(
	val handler: suspend (String) -> Result<List<BaseItemDto>>,
) : SearchRepository {
	override suspend fun search(searchTerm: String, itemTypes: Collection<BaseItemKind>) = handler(searchTerm)
}

class SearchViewModelTests : FunSpec({
	test("retry reruns an unchanged failed query and clears the failure state") {
		var failing = true
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, name = "Example")
		val model = SearchViewModel(SearchRepositoryFake {
			if (failing) Result.failure(IllegalStateException()) else Result.success(listOf(item))
		}, Dispatchers.Unconfined)
		model.searchImmediately("Example")
		model.state.value.failedGroups.isNotEmpty() shouldBe true
		failing = false
		model.retry()
		model.state.value.hasResults shouldBe true
		model.state.value.failedGroups shouldBe emptyList()
	}
	test("submitting an unchanged debounced query runs it immediately") {
		var calls = 0
		val model = SearchViewModel(SearchRepositoryFake { calls++; Result.success(emptyList()) }, Dispatchers.Unconfined)
		model.searchDebounced("Example")
		calls shouldBe 0
		model.searchImmediately("Example")
		(calls > 0) shouldBe true
		model.state.value.completed shouldBe true
	}
	test("a newer query cancels old work and preserves only its own results") {
		var cancelled = false
		val model = SearchViewModel(SearchRepositoryFake { query ->
			if (query == "old") {
				try { awaitCancellation() } finally { cancelled = true }
			} else Result.success(emptyList())
		}, Dispatchers.Unconfined)
		model.searchImmediately("old")
		model.searchImmediately("new")
		cancelled shouldBe true
		model.state.value.query shouldBe "new"
		model.state.value.completed shouldBe true
	}
	test("blanking the query cancels pending work and clears selection and results") {
		var cancelled = false
		val model = SearchViewModel(SearchRepositoryFake {
			try { awaitCancellation() } finally { cancelled = true }
		}, Dispatchers.Unconfined)
		model.searchImmediately("old")
		model.selection = 2 to 5
		model.searchImmediately("   ")
		cancelled shouldBe true
		model.state.value shouldBe SearchState()
		model.selection shouldBe (0 to 0)
	}
})
