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
    return lifecycleScope.launch {
        val available = try {
            org.jellyfin.androidtv.util.availableAlphabet(candidates) { letter ->
                when {
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
) {
    if (!isAdded) return
    val options = adapter.filters
    val any = getString(org.jellyfin.androidtv.R.string.library_filter_any)
    val labels = mutableListOf(
        getString(org.jellyfin.androidtv.R.string.library_filter_genre, options.genre ?: any),
        getString(org.jellyfin.androidtv.R.string.library_filter_year, options.year?.toString() ?: any),
        getString(org.jellyfin.androidtv.R.string.library_filter_rating, options.minimumRating?.let { getString(org.jellyfin.androidtv.R.string.library_rating_minimum, it) } ?: any),
        getString(org.jellyfin.androidtv.R.string.library_filter_watch, getString(if (options.isUnwatchedOnly) org.jellyfin.androidtv.R.string.lbl_unwatched else org.jellyfin.androidtv.R.string.library_filter_all)),
        getString(org.jellyfin.androidtv.R.string.library_filter_favorites, getString(if (options.isFavoriteOnly) org.jellyfin.androidtv.R.string.library_filter_on else org.jellyfin.androidtv.R.string.library_filter_off)),
        getString(org.jellyfin.androidtv.R.string.library_filter_reset),
    )
    val resolutions = org.jellyfin.androidtv.data.model.LibraryResolution.entries
    if (allowResolution) labels.add(5, getString(org.jellyfin.androidtv.R.string.library_filter_resolution, getString(options.resolution.labelRes)))
    fun apply() {
        adapter.setFilters(options)
        onChanged()
        showLibraryFilterMenu(adapter, genres, years, allowResolution, onChanged)
    }
    fun reset() {
        options.genre = null; options.year = null; options.minimumRating = null
        options.isFavoriteOnly = false; options.isUnwatchedOnly = false
        options.resolution = org.jellyfin.androidtv.data.model.LibraryResolution.ANY
        apply()
    }
    fun choose(title: Int, labels: List<String>, selected: Int, action: (Int) -> Unit) {
        var picked = false
        androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle(title)
            .setSingleChoiceItems(labels.toTypedArray(), selected) { dialog, index ->
                picked = true
                dialog.dismiss()
                action(index)
                apply()
            }.setNegativeButton(android.R.string.cancel, null)
            .create().apply {
                setOnDismissListener { if (!picked) showLibraryFilterMenu(adapter, genres, years, allowResolution, onChanged) }
                show()
            }
    }
    androidx.appcompat.app.AlertDialog.Builder(requireContext())
        .setTitle(org.jellyfin.androidtv.R.string.library_filters_title)
        .setItems(labels.toTypedArray()) { _, index ->
            when (index) {
                0 -> choose(org.jellyfin.androidtv.R.string.library_filter_genre_title, listOf(any) + genres,
                    genres.indexOf(options.genre) + 1) { options.genre = if (it == 0) null else genres[it - 1] }
                1 -> choose(org.jellyfin.androidtv.R.string.library_filter_year_title, listOf(any) + years.map(Int::toString),
                    years.indexOf(options.year) + 1) { options.year = if (it == 0) null else years[it - 1] }
                2 -> choose(org.jellyfin.androidtv.R.string.library_filter_rating_title,
                    listOf(any) + (1..10).map { getString(org.jellyfin.androidtv.R.string.library_rating_minimum, it) },
                    options.minimumRating ?: 0) { options.minimumRating = it.takeIf { it > 0 } }
                3 -> { options.isUnwatchedOnly = !options.isUnwatchedOnly; apply() }
                4 -> { options.isFavoriteOnly = !options.isFavoriteOnly; apply() }
                5 -> if (allowResolution) choose(org.jellyfin.androidtv.R.string.library_resolution_title,
                    resolutions.map { getString(it.labelRes) }, resolutions.indexOf(options.resolution)) {
                        options.resolution = resolutions[it]
                    } else reset()
                6 -> reset()
            }
        }.setNegativeButton(android.R.string.ok, null).show()
}
