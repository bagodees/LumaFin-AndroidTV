package org.jellyfin.androidtv.ui.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.personsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetPersonsRequest
import timber.log.Timber

interface SearchRepository {
	suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>>
}

class SearchRepositoryImpl(
	private val apiClient: ApiClient,
	private val userRepository: UserRepository,
) : SearchRepository {
	companion object {
		private const val QUERY_LIMIT = 25
	}

	override suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>> {
		return try {
			val userId = userRepository.currentUser.value?.id
				?: return Result.failure(IllegalStateException("Search requires a signed-in user"))
			if (itemTypes == setOf(BaseItemKind.PERSON)) {
				return Result.success(withContext(Dispatchers.IO) {
					apiClient.personsApi.getPersons(GetPersonsRequest(
						searchTerm = searchTerm,
						userId = userId,
						limit = QUERY_LIMIT,
						imageTypeLimit = 1,
						fields = ItemRepository.browseFields,
					)).content.items
				})
			}
			var request = GetItemsRequest(
				userId = userId,
				searchTerm = searchTerm,
				limit = QUERY_LIMIT,
				imageTypeLimit = 1,
				includeItemTypes = itemTypes,
				fields = ItemRepository.browseFields,
				recursive = true,
				enableTotalRecordCount = false,
			)

			// Special case for video row
			if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.VIDEO) {
				request = request.copy(
					mediaTypes = setOf(MediaType.VIDEO),
					includeItemTypes = null,
					excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.TV_CHANNEL)
				)
			}

			val result = withContext(Dispatchers.IO) {
				apiClient.itemsApi.getItems(request).content
			}

			Result.success(result.items)
		} catch (e: ApiClientException) {
			Timber.e(e, "Failed to search for items")
			Result.failure(e)
		}
	}
}
