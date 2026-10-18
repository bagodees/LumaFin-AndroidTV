package org.jellyfin.androidtv.ui.search

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

private class FakeSearchRepository(
	val handler: suspend (String, Collection<BaseItemKind>) -> Result<List<BaseItemDto>>,
) : SearchRepository {
	override suspend fun search(searchTerm: String, itemTypes: Collection<BaseItemKind>) = handler(searchTerm, itemTypes)
}

class SearchStateTests : FunSpec({
	val categories = linkedMapOf(1 to setOf(BaseItemKind.MOVIE), 2 to setOf(BaseItemKind.PERSON), 3 to setOf(BaseItemKind.BOX_SET))
	test("successful categories retain order while failed categories remain distinguishable") {
		runBlocking {
			val item = BaseItemDto(id = UUID.randomUUID(), name = "Example", type = BaseItemKind.MOVIE)
			val result = searchGroups("Example", categories, FakeSearchRepository { _, kinds ->
				if (BaseItemKind.PERSON in kinds) Result.failure(IllegalStateException("Unavailable")) else Result.success(listOf(item))
			})
			result.groups.map { it.labelRes } shouldBe listOf(1, 3)
			result.failedGroups shouldBe listOf(2)
			result.hasResults shouldBe true
			result.loading shouldBe false
			result.completed shouldBe true
		}
	}
	test("an empty successful search is different from a failed search") {
		runBlocking {
			val empty = searchGroups("missing", categories, FakeSearchRepository { _, _ -> Result.success(emptyList()) })
			empty.groups shouldBe emptyList()
			empty.failedGroups shouldBe emptyList()
			empty.completed shouldBe true
			val failed = searchGroups("missing", categories, FakeSearchRepository { _, _ -> Result.failure(IllegalStateException()) })
			failed.hasResults shouldBe false
			failed.failedGroups shouldBe listOf(1, 2, 3)
		}
	}
	test("people and collections receive the same query with separate category requests") {
		runBlocking {
			val calls = mutableListOf<Pair<String, Collection<BaseItemKind>>>()
			searchGroups("Jane Smith", categories, FakeSearchRepository { query, kinds ->
				calls.add(query to kinds)
				Result.success(emptyList())
			})
			calls shouldBe categories.values.map { "Jane Smith" to it }
		}
	}
	test("cancelling an obsolete query cancels its outstanding category requests without publishing results") {
		runBlocking {
			val started = CompletableDeferred<Unit>()
			var published = false
			var cancelled = false
			val job = launch {
				searchGroups("old", mapOf(1 to setOf(BaseItemKind.MOVIE)), FakeSearchRepository { _, _ ->
					started.complete(Unit)
					try { awaitCancellation() } finally { cancelled = true }
				})
				published = true
			}
			started.await()
			job.cancel()
			job.join()
			cancelled shouldBe true
			published shouldBe false
		}
	}
})
