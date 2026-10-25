package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.LiveTvOption
import org.jellyfin.androidtv.data.querying.GetAdditionalPartsRequest
import org.jellyfin.androidtv.data.querying.GetSpecialsRequest
import org.jellyfin.androidtv.data.querying.GetTrailersRequest
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.browsing.BrowseGridFragment.SortOption
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.artistsApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.api.request.GetAlbumArtistsRequest
import org.jellyfin.sdk.model.api.request.GetArtistsRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import org.jellyfin.sdk.model.api.request.GetUpcomingEpisodesRequest
import timber.log.Timber
import kotlin.math.min

fun <T : Any> ItemRowAdapter.setItems(
	items: Collection<T>,
	transform: (T, Int) -> BaseRowItem?,
) {
	Timber.i("Creating items from $itemsLoaded existing and ${items.size} new, adapter size is ${size()}")
	val offset = itemsLoaded
	val mappedItems = items.mapIndexedNotNull { index, item -> transform(item, offset + index) }
	if (offset == 0) {
		// A new query/refresh must not retain an unrefreshed tail from the previous results.
		replaceAll(mappedItems)
		itemsLoaded = mappedItems.size
		return
	}
	if (offset == size() && offset + mappedItems.size <= totalItems) {
		appendAll(mappedItems)
		itemsLoaded = offset + mappedItems.size
		return
	}

	val allItems = buildList {
		// Add current items before loaded items
		repeat(itemsLoaded) {
			add(this@setItems.get(it))
		}

		// Add loaded items
		mappedItems.forEach { add(it) }

		// Add current items after loaded items
		repeat(min(totalItems, size()) - itemsLoaded - mappedItems.size) {
			add(this@setItems.get(it + itemsLoaded + mappedItems.size))
		}
	}

	replaceAll(allItems)
	itemsLoaded = allItems.size
}

fun ItemRowAdapter.retrieveResumeItems(api: ApiClient, query: GetResumeItemsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.itemsApi.getResumeItems(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						isStaticHeight
					)
				}
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveNextUpItems(api: ApiClient, query: GetNextUpRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.tvShowsApi.getNextUp(query).content
			}

			// Some special flavor for series, used in FullDetailsFragment
			val firstNextUp = response.items.firstOrNull()
			if (query.seriesId != null && response.items.size == 1 && firstNextUp?.seasonId != null && firstNextUp.indexNumber != null) {
				// If we have exactly 1 episode returned, the series is currently partially watched
				// we want to query the server for all episodes in the same season starting from
				// this one to create a list of all unwatched episodes
				val episodesResponse = withContext(Dispatchers.IO) {
					api.itemsApi.getItems(
						parentId = firstNextUp.seasonId,
						startIndex = firstNextUp.indexNumber,
					).content
				}

				// Combine the next up episode with the additionally retrieved episodes
				val items = buildList {
					add(firstNextUp)
					addAll(episodesResponse.items)
				}

				setItems(
					items = items,
					transform = { item, _ ->
						BaseItemDtoBaseRowItem(
							item,
							preferParentThumb,
							false
						)
					}
				)

				if (items.isEmpty()) removeRow()
			} else {
				setItems(
					items = response.items,
					transform = { item, _ ->
						BaseItemDtoBaseRowItem(
							item,
							preferParentThumb,
							isStaticHeight
						)
					}
				)

				if (response.items.isEmpty()) removeRow()
			}
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLatestMedia(api: ApiClient, query: GetLatestMediaRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.userLibraryApi.getLatestMedia(query).content
			}

			setItems(
				items = response,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						isStaticHeight,
					)
				}
			)

			if (response.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveSpecialFeatures(api: ApiClient, query: GetSpecialsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.userLibraryApi.getSpecialFeatures(query.itemId).content
			}

			setItems(
				items = response,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(item, preferParentThumb, false)
				}
			)

			if (response.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveAdditionalParts(api: ApiClient, query: GetAdditionalPartsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.videosApi.getAdditionalPart(query.itemId).content
			}

			setItems(
				items = response.items,
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveUserViews(api: ApiClient, userViewsRepository: UserViewsRepository) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.userViewsApi.getUserViews().content
			}

			val filteredItems = response.items
				.filter { userViewsRepository.isSupported(it.collectionType) }

			setItems(
				items = filteredItems,
				transform = { item, _ -> BaseItemDtoBaseRowItem(item, staticHeight = false) }
			)

			if (filteredItems.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveSeasons(api: ApiClient, query: GetSeasonsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.tvShowsApi.getSeasons(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveUpcomingEpisodes(api: ApiClient, query: GetUpcomingEpisodesRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.tvShowsApi.getUpcomingEpisodes(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveSimilarItems(api: ApiClient, query: GetSimilarItemsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.libraryApi.getSimilarItems(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveTrailers(api: ApiClient, query: GetTrailersRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.userLibraryApi.getLocalTrailers(itemId = query.itemId)
			}.content

			setItems(
				items = response,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						false,
						BaseRowItemSelectAction.Play,
						false
					)
				}
			)

			if (response.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLiveTvRecommendedPrograms(
	api: ApiClient,
	query: GetRecommendedProgramsRequest
) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.liveTvApi.getRecommendedPrograms(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						false,
						isStaticHeight,
					)
				}
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLiveTvRecordings(api: ApiClient, query: GetRecordingsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.liveTvApi.getRecordings(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						false,
						isStaticHeight,
					)
				}
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLiveTvSeriesTimers(
	api: ApiClient,
	context: Context,
	canManageRecordings: Boolean
) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.liveTvApi.getSeriesTimers().content
			}

			setItems(
				items = buildList {
					add(
						GridButton(
							LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID,
							context.getString(R.string.lbl_recorded_tv)
						)
					)

					if (canManageRecordings) {
						add(
							GridButton(
								LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID,
								context.getString(R.string.lbl_schedule)
							)
						)

						add(
							GridButton(
								LiveTvOption.LIVE_TV_SERIES_OPTION_ID,
								context.getString(R.string.lbl_series)
							)
						)
					}

					addAll(response.items)
				},
				transform = { item, _ ->
					when (item) {
						is GridButton -> GridButtonBaseRowItem(item)
						is SeriesTimerInfoDto -> SeriesTimerInfoDtoBaseRowItem(item)
						else -> error("Unknown type for item")
					}
				}
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

private fun ItemRowAdapter.loadPage(
	api: ApiClient,
	current: () -> Boolean = { true },
	fetch: suspend () -> BaseItemDtoQueryResult,
	apply: ItemRowAdapter.(BaseItemDtoQueryResult) -> Unit,
) {
	val identity = api.baseUrl to api.accessToken
	pageRequest.load(ProcessLifecycleOwner.get().lifecycleScope,
		fetch = { withContext(Dispatchers.IO) { fetch() } },
		isCurrent = { identity == (api.baseUrl to api.accessToken) && current() },
		apply = { apply(it) }, finished = { notifyRetrieveFinished(it) }, started = { notifyRetrieveStarted() })
}

fun ItemRowAdapter.retrieveLiveTvChannels(api: ApiClient, query: GetLiveTvChannelsRequest, startIndex: Int, batchSize: Int) {
	loadPage(api, fetch = { api.liveTvApi.getLiveTvChannels(query.copy(startIndex = startIndex, limit = batchSize)).content }) { response ->
		totalItems = response.totalRecordCount
		setItems(response.items) { item, _ -> BaseItemDtoBaseRowItem(item, false, isStaticHeight) }
		if (itemsLoaded == 0) removeRow()
	}
}

fun ItemRowAdapter.retrieveAlbumArtists(api: ApiClient, query: GetAlbumArtistsRequest, startIndex: Int, batchSize: Int) {
	loadPage(api, fetch = { api.artistsApi.getAlbumArtists(query.copy(startIndex = startIndex, limit = batchSize)).content }) { response ->
		totalItems = response.totalRecordCount
		setItems(response.items) { item, _ -> BaseItemDtoBaseRowItem(item, preferParentThumb, isStaticHeight) }
		if (itemsLoaded == 0) removeRow()
	}
}

fun ItemRowAdapter.retrieveArtists(api: ApiClient, query: GetArtistsRequest, startIndex: Int, batchSize: Int) {
	loadPage(api, fetch = { api.artistsApi.getArtists(query.copy(startIndex = startIndex, limit = batchSize)).content }) { response ->
		totalItems = response.totalRecordCount
		setItems(response.items) { item, _ -> BaseItemDtoBaseRowItem(item, preferParentThumb, isStaticHeight) }
		if (itemsLoaded == 0) removeRow()
	}
}

fun ItemRowAdapter.retrieveItems(api: ApiClient, query: GetItemsRequest, startIndex: Int, batchSize: Int) {
	if (runtimeController != null && filters?.runtimeMinutes != null) {
		pageRequest.cancel()
		runtimeController.retrieve(query, startIndex, batchSize)
		return
	}
	runtimeController?.clear()
	loadPage(api, current = { itemsRequest == query && filters?.runtimeMinutes == null },
		fetch = { api.itemsApi.getItems(query.copy(startIndex = startIndex, limit = batchSize)).content }) { response ->
		val items = if (query.enableTotalRecordCount == false && batchSize > 0) response.items.take(batchSize) else response.items
		totalItems = if (query.enableTotalRecordCount == false) items.size else response.totalRecordCount
		setItems(items) { item, _ -> BaseItemDtoBaseRowItem(item, preferParentThumb, isStaticHeight) }
		if (itemsLoaded == 0) removeRow()
	}
}
fun ItemRowAdapter.retrievePremieres(
	api: ApiClient,
	query: GetItemsRequest,
) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response = withContext(Dispatchers.IO) {
				api.itemsApi.getItems(query).content
			}

			setItems(
				items = response.items,
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						isStaticHeight,
					)
				}
			)

			if (response.items.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

// Request modifiers

fun setAlbumArtistsSorting(
	request: GetAlbumArtistsRequest,
	sortOption: SortOption,
) = request.copy(
	sortBy = setOf(sortOption.value, ItemSortBy.SORT_NAME),
	sortOrder = setOf(sortOption.order)
)

fun setArtistsSorting(
	request: GetArtistsRequest,
	sortOption: SortOption,
) = request.copy(
	sortBy = setOf(sortOption.value, ItemSortBy.SORT_NAME),
	sortOrder = setOf(sortOption.order)
)

fun setItemsSorting(
	request: GetItemsRequest,
	sortOption: SortOption,
) = request.copy(
	sortBy = setOf(sortOption.value, ItemSortBy.SORT_NAME),
	sortOrder = setOf(sortOption.order)
)

fun setAlbumArtistsFilter(
	request: GetAlbumArtistsRequest,
	filters: Collection<ItemFilter>?,
) = request.copy(
	filters = filters,
)

fun setArtistsFilter(
	request: GetArtistsRequest,
	filters: Collection<ItemFilter>?,
) = request.copy(
	filters = filters,
)

fun setItemsFilter(
	request: GetItemsRequest,
	filters: Collection<ItemFilter>?,
) = request.copy(
	filters = filters,
)

fun setAlbumArtistsStartLetter(
	request: GetAlbumArtistsRequest,
	startLetter: String?,
) = request.copy(
	nameStartsWith = startLetter?.takeUnless { it == "#" },
	nameStartsWithOrGreater = if (startLetter == "#") "0" else null,
	nameLessThan = if (startLetter == "#") ":" else null,
)

fun setArtistsStartLetter(
	request: GetArtistsRequest,
	startLetter: String?,
) = request.copy(
	nameStartsWith = startLetter?.takeUnless { it == "#" },
	nameStartsWithOrGreater = if (startLetter == "#") "0" else null,
	nameLessThan = if (startLetter == "#") ":" else null,
)

fun setItemsStartLetter(
	request: GetItemsRequest,
	startLetter: String?,
) = request.copy(
	nameStartsWith = startLetter?.takeUnless { it == "#" },
	nameStartsWithOrGreater = if (startLetter == "#") "0" else null,
	nameLessThan = if (startLetter == "#") ":" else null,
)

/**
 * Shows the items from [letter] onward (to the end of the alphabet) instead of only that letter, so a
 * name-sorted list can be skipped through. "#" or null shows everything, since numbers sort first.
 */
fun setItemsJumpLetter(
	request: GetItemsRequest,
	letter: String?,
) = request.copy(
	nameStartsWith = null,
	nameLessThan = null,
	nameStartsWithOrGreater = letter?.takeUnless { it == "#" },
)

fun setArtistsJumpLetter(
	request: GetArtistsRequest,
	letter: String?,
) = request.copy(
	nameStartsWith = null,
	nameLessThan = null,
	nameStartsWithOrGreater = letter?.takeUnless { it == "#" },
)

fun setAlbumArtistsJumpLetter(
	request: GetAlbumArtistsRequest,
	letter: String?,
) = request.copy(
	nameStartsWith = null,
	nameLessThan = null,
	nameStartsWithOrGreater = letter?.takeUnless { it == "#" },
)

@JvmOverloads
fun ItemRowAdapter.refreshItem(
	api: ApiClient,
	lifecycleOwner: LifecycleOwner,
	currentBaseRowItem: BaseRowItem,
	callback: () -> Unit = {}
) {
	if (currentBaseRowItem !is BaseItemDtoBaseRowItem || currentBaseRowItem is AudioQueueBaseRowItem) return
	val currentBaseItem = currentBaseRowItem.baseItem ?: return

	lifecycleOwner.lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(itemId = currentBaseItem.id).content
			}
		}.fold(
			onSuccess = { refreshedBaseItem ->
				val index = indexOf(currentBaseRowItem)
				// Item could be removed while API was loading, check if the index is valid first
				if (index == -1) return@fold

				set(
					index = index,
					element = BaseItemDtoBaseRowItem(
						item = refreshedBaseItem,
						preferParentThumb = currentBaseRowItem.preferParentThumb,
						staticHeight = currentBaseRowItem.staticHeight,
						selectAction = currentBaseRowItem.selectAction,
						preferSeriesPoster = currentBaseRowItem.preferSeriesPoster
					)
				)
			},
			onFailure = { err ->
				if (err is InvalidStatusException && err.status == 404) remove(currentBaseRowItem)
				else Timber.e(err, "Failed to refresh item")
			}
		)

		callback()
	}
}

fun setCombinedItemsFilter(request: GetItemsRequest, options: org.jellyfin.androidtv.data.model.FilterOptions, base: GetItemsRequest = request) = request.copy(
    filters = options.filters,
    genres = options.genre?.let { listOf(it) } ?: base.genres,
    years = options.year?.let { listOf(it) } ?: base.years,
    minCommunityRating = options.minimumRating?.toDouble() ?: base.minCommunityRating,
    minWidth = listOfNotNull(base.minWidth, options.resolution.minWidth).maxOrNull(),
    maxWidth = listOfNotNull(base.maxWidth, options.resolution.maxWidth).minOrNull(),
)
