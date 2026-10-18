package org.jellyfin.androidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemKind

class SearchViewModel(
	private val searchRepository: SearchRepository,
	private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
	companion object {
		private val debounceDuration = 600.milliseconds

		private val groups = mapOf(
			R.string.lbl_movies to setOf(BaseItemKind.MOVIE),
			R.string.lbl_series to setOf(BaseItemKind.SERIES),
			R.string.lbl_episodes to setOf(BaseItemKind.EPISODE),
			R.string.lbl_collections to setOf(BaseItemKind.BOX_SET),
			R.string.lbl_people to setOf(BaseItemKind.PERSON),
			R.string.lbl_videos to setOf(BaseItemKind.VIDEO),
			R.string.lbl_programs to setOf(BaseItemKind.LIVE_TV_PROGRAM),
			R.string.channels to setOf(BaseItemKind.LIVE_TV_CHANNEL),
			R.string.lbl_playlists to setOf(BaseItemKind.PLAYLIST),
			R.string.lbl_artists to setOf(BaseItemKind.MUSIC_ARTIST),
			R.string.lbl_albums to setOf(BaseItemKind.MUSIC_ALBUM),
			R.string.lbl_songs to setOf(BaseItemKind.AUDIO),
			R.string.photo_albums to setOf(BaseItemKind.PHOTO_ALBUM),
			R.string.photos to setOf(BaseItemKind.PHOTO),
		)
	}

	private var searchJob: Job? = null

	private var previousQuery: String? = null
	var selection: Pair<Int, Int> = 0 to 0

	private val _state = MutableStateFlow(SearchState())
	val state = _state.asStateFlow()

	fun searchImmediately(query: String) = searchDebounced(query, 0.milliseconds, force = true)
	fun retry() = searchImmediately(_state.value.query)

	fun searchDebounced(query: String, debounce: Duration = debounceDuration, force: Boolean = false): Boolean {
		val trimmed = query.trim()
		if (trimmed == previousQuery && !force) return false
		previousQuery = trimmed
		selection = 0 to 0

		searchJob?.cancel()

		if (trimmed.isBlank()) {
			_state.value = SearchState()
			return true
		}

		_state.value = SearchState(query = trimmed, loading = true)
		searchJob = viewModelScope.launch(dispatcher) {
			delay(debounce)
			_state.value = searchGroups(trimmed, groups, searchRepository)
		}

		return true
	}
}
