package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.filterApi
import org.jellyfin.sdk.api.client.extensions.artistsApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import java.util.UUID

fun BrowseGridFragment.createSettingsVisibility() = MutableStateFlow(false)

fun BrowseGridFragment.addSettings(
	view: ComposeView,
	itemId: UUID,
	displayPreferencesId: String,
	visible: MutableStateFlow<Boolean>,
) {
	view.setContent {
		val isVisible by visible.collectAsState(false)

		ProvideRouter(
			routes,
			Routes.LIBRARIES_DISPLAY,
			mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
		) {
			SettingsDialog(
				visible = isVisible,
				onDismissRequest = {
					visible.value = false
					onResume()
				}
			) {
				SettingsRouterContent()
			}
		}
	}
}

/** Probe the server using the current library query, including favorite/unwatched filters. */
fun BrowseGridFragment.loadAvailableLetters(
    adapter: org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter,
    candidates: String,
    callback: (String?) -> Unit,
): kotlinx.coroutines.Job {
    val api = org.koin.core.context.GlobalContext.get().get<org.jellyfin.sdk.api.client.ApiClient>()
    val items = adapter.itemsRequest
    val artists = adapter.artistsRequest
    val albumArtists = adapter.albumArtistsRequest
    return viewLifecycleOwner.lifecycleScope.launch {
        val available = try {
            kotlinx.coroutines.withTimeoutOrNull(180_000L) {
            org.jellyfin.androidtv.util.availableAlphabet(candidates) { letter ->
                when {
                    items != null && adapter.runtimeController != null && adapter.filters.runtimeMinutes != null ->
                        adapter.runtimeController.hasRuntimeMatches(
                            org.jellyfin.androidtv.ui.itemhandling.setItemsStartLetter(items, letter.toString()))
                    artists != null -> api.artistsApi.getArtists(
                        org.jellyfin.androidtv.ui.itemhandling.setArtistsStartLetter(artists, letter.toString())
                            .copy(startIndex = 0, limit = 1, enableTotalRecordCount = false)
                    ).content.items.orEmpty().isNotEmpty()
                    albumArtists != null -> api.artistsApi.getAlbumArtists(
                        org.jellyfin.androidtv.ui.itemhandling.setAlbumArtistsStartLetter(albumArtists, letter.toString())
                            .copy(startIndex = 0, limit = 1, enableTotalRecordCount = false)
                    ).content.items.orEmpty().isNotEmpty()
                    items != null -> api.itemsApi.getItems(
                        org.jellyfin.androidtv.ui.itemhandling.setItemsStartLetter(items, letter.toString())
                            .copy(startIndex = 0, limit = 1, enableTotalRecordCount = false)
                    ).content.items.orEmpty().isNotEmpty()
                    else -> false
                }
            }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            timber.log.Timber.w(error, "Unable to load available library letters")
            null
        }
        callback(available)
    }
}

/** One menu combines filters without changing sorting or the current letter selection. */
fun BrowseGridFragment.showCombinedFilters(
    adapter: org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter,
    api: org.jellyfin.sdk.api.client.ApiClient,
    allowResolution: Boolean,
    onChanged: () -> Unit,
) {
    val context = requireContext()
    val request = adapter.itemsRequest ?: return
    var loadingFinished = false
    val loading = androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle(org.jellyfin.androidtv.R.string.library_filters_title)
        .setMessage(org.jellyfin.androidtv.R.string.library_filter_loading)
        .setNegativeButton(android.R.string.cancel, null).create()
    val job = lifecycleScope.launch {
        try {
            val available = api.filterApi.getQueryFiltersLegacy(
                userId = request.userId, parentId = request.parentId,
                includeItemTypes = request.includeItemTypes,
            ).content
            loadingFinished = true
            loading.dismiss()
            if (!isAdded || !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return@launch
            showLibraryFilterMenu(adapter, available.genres.orEmpty().distinct().sorted(), available.years.orEmpty().distinct().sortedDescending(), allowResolution, onChanged)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            loadingFinished = true
            loading.dismiss()
            timber.log.Timber.w(error, "Unable to load library filter options")
            if (isAdded) androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(org.jellyfin.androidtv.R.string.library_filters_title)
                .setMessage(org.jellyfin.androidtv.R.string.library_filter_load_failed)
                .setPositiveButton(android.R.string.ok, null).show()
        }
    }
    loading.setOnDismissListener { if (!loadingFinished) job.cancel() }
    loading.show()
}

private fun BrowseGridFragment.showLibraryFilterMenu(
    adapter: org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter,
    genres: List<String>, years: List<Int>, allowResolution: Boolean, onChanged: () -> Unit,
    draft: org.jellyfin.androidtv.data.model.FilterOptions = adapter.filters.copy(),
    focusItem: Int = 0,
) {
    if (!isAdded || view == null) return
    val any = getString(org.jellyfin.androidtv.R.string.library_filter_any)
    var selectedFocus = focusItem
    fun redisplay() = showLibraryFilterMenu(adapter, genres, years, allowResolution, onChanged, draft, selectedFocus)
    fun choose(title: Int, labels: List<String>, selected: Int, action: (Int) -> Unit) {
        var picked = false
        androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle(title)
            .setSingleChoiceItems(labels.toTypedArray(), selected) { dialog, index ->
                picked = true
                dialog.dismiss()
                action(index)
                redisplay()
            }.setNegativeButton(android.R.string.cancel, null)
            .create().apply {
                setOnDismissListener { if (!picked) redisplay() }
                show()
            }
    }
    val entries = mutableListOf<Pair<String, () -> Unit>>()
    entries += getString(org.jellyfin.androidtv.R.string.library_filter_genre, draft.genre ?: any) to {
        choose(org.jellyfin.androidtv.R.string.library_filter_genre_title, listOf(any) + genres, genres.indexOf(draft.genre) + 1) {
            draft.genre = if (it == 0) null else genres[it - 1]
        }
    }
    entries += getString(org.jellyfin.androidtv.R.string.library_filter_year, draft.year?.toString() ?: any) to {
        choose(org.jellyfin.androidtv.R.string.library_filter_year_title, listOf(any) + years.map(Int::toString), years.indexOf(draft.year) + 1) {
            draft.year = if (it == 0) null else years[it - 1]
        }
    }
    entries += getString(org.jellyfin.androidtv.R.string.library_filter_rating, draft.minimumRating?.let {
        getString(org.jellyfin.androidtv.R.string.library_rating_minimum, it)
    } ?: any) to {
        choose(org.jellyfin.androidtv.R.string.library_filter_rating_title,
            listOf(any) + (1..10).map { getString(org.jellyfin.androidtv.R.string.library_rating_minimum, it) }, draft.minimumRating ?: 0) {
            draft.minimumRating = it.takeIf { it > 0 }
        }
    }
    entries += getString(org.jellyfin.androidtv.R.string.library_filter_watch, getString(if (draft.isUnwatchedOnly)
        org.jellyfin.androidtv.R.string.lbl_unwatched else org.jellyfin.androidtv.R.string.library_filter_all)) to {
        draft.isUnwatchedOnly = !draft.isUnwatchedOnly; redisplay()
    }
    entries += getString(org.jellyfin.androidtv.R.string.library_filter_favorites, getString(if (draft.isFavoriteOnly)
        org.jellyfin.androidtv.R.string.library_filter_on else org.jellyfin.androidtv.R.string.library_filter_off)) to {
        draft.isFavoriteOnly = !draft.isFavoriteOnly; redisplay()
    }
    if (allowResolution) {
        val resolutions = org.jellyfin.androidtv.data.model.LibraryResolution.entries
        entries += getString(org.jellyfin.androidtv.R.string.library_filter_resolution, getString(draft.resolution.labelRes)) to {
            choose(org.jellyfin.androidtv.R.string.library_resolution_title, resolutions.map { getString(it.labelRes) }, resolutions.indexOf(draft.resolution)) {
                draft.resolution = resolutions[it]
            }
        }
        val durations = listOf(null, 30, 60, 90, 120)
        entries += getString(org.jellyfin.androidtv.R.string.runtime_filter_value, draft.runtimeMinutes?.let {
            getString(org.jellyfin.androidtv.R.string.runtime_check_under, it)
        } ?: any) to {
            choose(org.jellyfin.androidtv.R.string.runtime_filter_title, durations.map { value -> value?.let {
                getString(org.jellyfin.androidtv.R.string.runtime_check_under, it)
            } ?: any }, durations.indexOf(draft.runtimeMinutes)) { draft.runtimeMinutes = durations[it] }
        }
    }
    entries += getString(org.jellyfin.androidtv.R.string.library_filter_reset) to {
        draft.genre = null; draft.year = null; draft.minimumRating = null
        draft.isFavoriteOnly = false; draft.isUnwatchedOnly = false
        draft.resolution = org.jellyfin.androidtv.data.model.LibraryResolution.ANY
        draft.runtimeMinutes = null
        redisplay()
    }
    val menu = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        .setTitle(org.jellyfin.androidtv.R.string.library_filters_title)
        .setItems(entries.map { it.first }.toTypedArray()) { _, index -> selectedFocus = index; entries[index].second() }
        .setPositiveButton(org.jellyfin.androidtv.R.string.runtime_filter_apply) { _, _ ->
            val controller = adapter.runtimeController
            if (allowResolution && controller != null) controller.applyDraft(draft, onChanged, ::redisplay)
            else { adapter.setFilters(draft); onChanged() }
        }.setNegativeButton(android.R.string.cancel, null).create()
    menu.show()
    menu.listView.setSelection(selectedFocus)
}

fun BrowseGridFragment.createRuntimeController(
    adapter: org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter,
    onRestored: () -> Unit,
): org.jellyfin.androidtv.ui.itemhandling.RuntimeFilterController {
    val api = org.koin.core.context.GlobalContext.get().get<org.jellyfin.sdk.api.client.ApiClient>()
    return org.jellyfin.androidtv.ui.itemhandling.RuntimeFilterController(requireContext(), viewLifecycleOwner, adapter, api,
        onRestored = onRestored,
        onOpenFilters = {
            if (isAdded && view != null) showCombinedFilters(adapter, api, true) {
                onRestored()
                adapter.Retrieve()
            }
        })
}
