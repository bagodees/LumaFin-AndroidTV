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
    allowMediaType: Boolean,
    onChanged: () -> Unit,
) = showModernLibraryFilters(adapter, api, allowResolution, onChanged, allowMediaType)

fun BrowseGridFragment.createRuntimeController(
    adapter: org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter,
    onRestored: () -> Unit,
): org.jellyfin.androidtv.ui.itemhandling.RuntimeFilterController {
    val api = org.koin.core.context.GlobalContext.get().get<org.jellyfin.sdk.api.client.ApiClient>()
    return org.jellyfin.androidtv.ui.itemhandling.RuntimeFilterController(requireContext(), viewLifecycleOwner, adapter, api,
        onRestored = onRestored,
        onOpenFilters = {
            if (isAdded && view != null) showCombinedFilters(adapter, api, true, false) {
                onRestored()
                adapter.Retrieve()
            }
        })
}
