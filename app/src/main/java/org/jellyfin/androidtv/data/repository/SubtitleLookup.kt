package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.subtitleApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo
import java.util.UUID
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

internal fun subtitleLookupTarget(sourceId: String): UUID? = sourceId.toUUIDOrNull()

internal fun subtitleLookupAllowed(type: BaseItemKind?, permitted: Boolean, supported: Boolean) =
	permitted && supported && type in setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE)

internal fun externalSubtitleKeys(item: BaseItemDto, sourceId: String, language: String): Set<String> = item.mediaSources
	?.firstOrNull { it.id == sourceId }?.mediaStreams.orEmpty()
	.filter { it.type == MediaStreamType.SUBTITLE && it.isExternal && it.language.equals(language, ignoreCase = true) }
	.map { "${it.index}|${it.language}|${it.codec}|${it.path}" }.toSet()

internal interface SubtitleLookupGateway {
	suspend fun item(): BaseItemDto
	suspend fun search(language: String): List<RemoteSubtitleInfo>
	suspend fun download(id: String)
}

internal class SdkSubtitleLookupGateway(private val api: ApiClient, private val userId: UUID, private val itemId: UUID) : SubtitleLookupGateway {
	override suspend fun item() = api.userLibraryApi.getItem(itemId = itemId, userId = userId).content
	override suspend fun search(language: String) = api.subtitleApi.searchRemoteSubtitles(itemId, language).content
	override suspend fun download(id: String) { api.subtitleApi.downloadRemoteSubtitles(itemId, id) }
}

/** A 204 is only acceptance: older servers swallow provider errors and queue metadata refresh. */
internal class SubtitleLookup(
	private val gateway: SubtitleLookupGateway,
	private val isCurrent: () -> Boolean,
	private val waitForRefresh: suspend () -> Unit = { delay(1000) },
) {
	private fun checkCurrent() { if (!isCurrent()) throw CancellationException("Subtitle context changed") }
	suspend fun search(language: String): List<RemoteSubtitleInfo> = withTimeout(30_000) {
		require(language.matches(Regex("[a-z]{3}")))
		checkCurrent()
		gateway.search(language).also { checkCurrent() }.filter { !it.id.isNullOrBlank() }.distinctBy { it.id }
	}
	suspend fun download(id: String, sourceId: String, language: String): BaseItemDto? = withTimeout(45_000) {
		require(id.isNotBlank() && sourceId.isNotBlank())
		checkCurrent()
		val before = gateway.item()
		checkCurrent()
		check(before.mediaSources.orEmpty().any { it.id == sourceId })
		val keys = externalSubtitleKeys(before, sourceId, language)
		gateway.download(id) // Exactly one POST; no automatic retry of a possibly completed server write.
		checkCurrent()
		repeat(12) {
			waitForRefresh()
			checkCurrent()
			val refreshed = gateway.item()
			checkCurrent()
			if ((externalSubtitleKeys(refreshed, sourceId, language) - keys).isNotEmpty()) return@withTimeout refreshed
		}
		null
	}
}
