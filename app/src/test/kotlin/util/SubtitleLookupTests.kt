package org.jellyfin.androidtv.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.data.repository.*
import org.jellyfin.sdk.model.api.*
import java.util.UUID

class SubtitleLookupTests : FunSpec({
	fun item(source: String = "version-a", index: Int? = null, language: String = "eng", external: Boolean = true) = BaseItemDto(
		id = UUID(0, 1), type = BaseItemKind.MOVIE,
		mediaSources = listOf(mockk<MediaSourceInfo> {
			every { id } returns source
			every { mediaStreams } returns index?.let { number -> listOf(mockk<MediaStream> {
				every { type } returns MediaStreamType.SUBTITLE
				every { isExternal } returns external
				every { this@mockk.language } returns language
				every { this@mockk.index } returns number
				every { codec } returns "srt"
				every { path } returns "not-for-display"
			}) }.orEmpty()
		}),
	)
	class Gateway(var snapshots: List<BaseItemDto>, var results: List<RemoteSubtitleInfo> = emptyList()) : SubtitleLookupGateway {
		var reads = 0
		var posts = 0
		var searches = 0
		override suspend fun item() = snapshots[minOf(reads++, snapshots.lastIndex)]
		override suspend fun search(language: String): List<RemoteSubtitleInfo> { searches++; return results }
		override suspend fun download(id: String) { posts++ }
	}
	test("lookup requires title type permission and server support") {
		for (type in BaseItemKind.entries) subtitleLookupAllowed(type, true, true) shouldBe (type == BaseItemKind.MOVIE || type == BaseItemKind.EPISODE)
		subtitleLookupAllowed(BaseItemKind.MOVIE, false, true) shouldBe false
		subtitleLookupAllowed(BaseItemKind.MOVIE, true, false) shouldBe false
		subtitleLookupAllowed(null, true, true) shouldBe false
	}
	test("the lookup target is the selected version UUID and unknown source IDs are rejected") {
		val selected = UUID(0, 7)
		subtitleLookupTarget(selected.toString()) shouldBe selected
		subtitleLookupTarget(selected.toString().replace("-", "")) shouldBe selected
		subtitleLookupTarget("remote-source") shouldBe null
		subtitleLookupTarget("") shouldBe null
	}
	test("a delayed new external track confirms a single download") {
		val original = item()
		val updated = item(index = 2)
		val gateway = Gateway(listOf(original, original, updated))
		SubtitleLookup(gateway, { true }, {}).download("provider-id", "version-a", "eng") shouldBe updated
		gateway.posts shouldBe 1
		gateway.reads shouldBe 3
	}
	test("204 without a new track stays unconfirmed and never repeats the POST") {
		val gateway = Gateway(listOf(item(index = 2)))
		SubtitleLookup(gateway, { true }, {}).download("provider-id", "version-a", "eng") shouldBe null
		gateway.posts shouldBe 1
		gateway.reads shouldBe 13
	}
	test("another version language or embedded track cannot verify this download") {
		for (updated in listOf(item("version-b", 2), item(index = 2, language = "fra"), item(index = 2, external = false))) {
			SubtitleLookup(Gateway(listOf(item(), updated)), { true }, {}).download("id", "version-a", "eng") shouldBe null
		}
	}
	test("account or title changes before lookup or download prohibit server work") {
		val gateway = Gateway(listOf(item()))
		val lookup = SubtitleLookup(gateway, { false }, {})
		shouldThrow<CancellationException> { lookup.search("eng") }
		shouldThrow<CancellationException> { lookup.download("id", "version-a", "eng") }
		gateway.searches shouldBe 0
		gateway.posts shouldBe 0
	}
	test("cancellation during refresh stops polling without retrying a submitted write") {
		val gateway = Gateway(listOf(item()))
		val lookup = SubtitleLookup(gateway, { true }, { throw CancellationException() })
		shouldThrow<CancellationException> { lookup.download("id", "version-a", "eng") }
		gateway.posts shouldBe 1
		gateway.reads shouldBe 1
	}
	test("missing selected version prevents download and invalid language prevents search") {
		val gateway = Gateway(listOf(item("other")))
		val lookup = SubtitleLookup(gateway, { true }, {})
		shouldThrow<IllegalStateException> { lookup.download("id", "version-a", "eng") }
		shouldThrow<IllegalArgumentException> { lookup.search("../eng") }
		gateway.posts shouldBe 0
		gateway.searches shouldBe 0
	}
	test("search retains only unique usable result IDs") {
		fun result(id: String?) = mockk<RemoteSubtitleInfo> { every { this@mockk.id } returns id }
		val first = result("one")
		val gateway = Gateway(listOf(item()), listOf(first, first, result(null), result(" ")))
		SubtitleLookup(gateway, { true }, {}).search("eng") shouldBe listOf(first)
	}
})
