package org.jellyfin.androidtv.browsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.itemhandling.setItems
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

internal class PagingAdapterFixture(total: Int) {
	val adapter = mockk<ItemRowAdapter>(relaxed = true)
	val rows = mutableListOf<Any>()
	var loaded = 0
	var prefixReads = 0
	var diffInputs = 0
	var appendedCards = 0
	val insertedRanges = mutableListOf<Pair<Int, Int>>()
	init {
		every { adapter.itemsLoaded } answers { loaded }
		every { adapter.itemsLoaded = any() } answers { loaded = firstArg() }
		every { adapter.totalItems } returns total
		every { adapter.size() } answers { rows.size }
		every { adapter.get(any()) } answers { prefixReads++; rows[firstArg()] }
		every { adapter.replaceAll(any(), any(), any()) } answers {
			val next = firstArg<List<Any>>(); diffInputs += next.size
			rows.clear(); rows.addAll(next)
		}
		every { adapter.appendAll(any()) } answers {
			val next = firstArg<Collection<Any>>()
			insertedRanges.add(rows.size to next.size)
			appendedCards += next.size
			rows.addAll(next)
		}
	}
}

class BrowsePagingProfileTests : FunSpec({
	test("profile real adapter page application across a thousand ordered items") {
		val fixture = PagingAdapterFixture(1000)
		val items = (0L until 1000).map { BaseItemDtoBaseRowItem(BaseItemDto(id = UUID(0, it), type = BaseItemKind.MOVIE)) }
		items.chunked(25).forEach { page -> fixture.adapter.setItems(page) { item, _ -> item } }
		fixture.rows shouldBe items
		fixture.loaded shouldBe 1000
		fixture.prefixReads shouldBe 0
		fixture.diffInputs shouldBe 25
		fixture.appendedCards shouldBe 975
		println("PAGING_PROFILE items=1000 page=25 priorCardReads=${fixture.prefixReads} diffInputCards=${fixture.diffInputs}")
	}
	test("ten thousand results append in order with stable previous objects and one range per page") {
		val fixture = PagingAdapterFixture(10000)
		val items = (0L until 10000).map { BaseItemDtoBaseRowItem(BaseItemDto(id = UUID(0, it), type = BaseItemKind.MOVIE)) }
		items.chunked(37).forEach { page -> fixture.adapter.setItems(page) { item, _ -> item } }
		fixture.rows shouldBe items
		fixture.loaded shouldBe 10000
		(fixture.rows[36] === items[36]) shouldBe true
		fixture.insertedRanges.first() shouldBe (37 to 37)
		fixture.insertedRanges.last() shouldBe (9990 to 10)
		fixture.prefixReads shouldBe 0
		fixture.diffInputs shouldBe 37
	}
	test("a refreshed first page replaces the old query tail and restarts the server cursor") {
		val fixture = PagingAdapterFixture(1000)
		val items = (0L until 100).map { BaseItemDtoBaseRowItem(BaseItemDto(id = UUID(0, it), type = BaseItemKind.MOVIE)) }
		items.chunked(25).forEach { page -> fixture.adapter.setItems(page) { item, _ -> item } }
		fixture.loaded = 0
		val newOrder = items.reversed().take(25)
		fixture.adapter.setItems(newOrder) { item, _ -> item }
		fixture.rows shouldBe newOrder
		fixture.loaded shouldBe 25
	}
	test("empty refreshed results remove all old cards") {
		val fixture = PagingAdapterFixture(0)
		fixture.rows.add(BaseItemDtoBaseRowItem(BaseItemDto(id = UUID(0, 1), type = BaseItemKind.MOVIE)))
		fixture.adapter.setItems(emptyList<Int>()) { _, _ -> error("No items") }
		fixture.rows shouldBe emptyList()
		fixture.loaded shouldBe 0
	}
})
