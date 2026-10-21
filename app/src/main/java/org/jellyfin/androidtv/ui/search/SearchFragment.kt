package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ListRowPresenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.androidtv.ui.search.composable.SearchVoiceInput
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.util.speech.rememberSpeechRecognizerAvailability
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class SearchFragment : Fragment() {
	companion object {
		const val EXTRA_QUERY = "query"
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		// Same readability gradient as Home, libraries and details over the backdrop.
		view.setBackgroundResource(R.drawable.details_readability_gradient)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val viewModel = koinViewModel<SearchViewModel>()
		val state by viewModel.state.collectAsState()
		val keyboard = LocalSoftwareKeyboardController.current
		var submitted by remember { mutableStateOf(false) }
		var restoringResults by remember { mutableStateOf(state.hasResults) }
		val searchFragmentDelegate = koinInject<SearchFragmentDelegate> { parametersOf(requireContext()) }
		var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
		val textInputFocusRequester = remember { FocusRequester() }
		val resultFocusRequester = remember { FocusRequester() }
		val speechRecognizerAvailability = rememberSpeechRecognizerAvailability()

		LaunchedEffect(Unit) {
			val extraQuery = arguments?.getString(EXTRA_QUERY)
			if (query.text.isBlank() && !extraQuery.isNullOrBlank()) {
				query = query.copy(text = extraQuery)
				viewModel.searchImmediately(extraQuery)
				submitted = true
			} else if (state.query.isBlank() && query.text.isNotBlank()) {
				viewModel.searchImmediately(query.text)
				submitted = true
			} else if (!state.hasResults) {
				textInputFocusRequester.requestFocus()
			}
		}
		LaunchedEffect(state.groups) {
			searchFragmentDelegate.showResults(state.groups)
			if ((submitted || restoringResults) && state.hasResults) resultFocusRequester.requestFocus()
			restoringResults = false
		}

		Column(Modifier.fillMaxSize()) {
			MainToolbar(MainToolbarActiveButton.Search)

			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.focusRestorer()
					.focusGroup()
					.padding(horizontal = 48.dp)
			) {
				if (speechRecognizerAvailability) {
					SearchVoiceInput(
						onQueryChange = {
							submitted = false
							query = query.copy(text = it)
							viewModel.searchDebounced(it)
						},
						onQuerySubmit = {
							viewModel.searchImmediately(query.text)
							submitted = true
							keyboard?.hide()
							resultFocusRequester.requestFocus()
						}
					)
				}

				SearchTextInput(
					query = query.text,
					onQueryChange = {
						submitted = false
						query = query.copy(text = it)
						viewModel.searchDebounced(query.text)
					},
					onQuerySubmit = {
						viewModel.searchImmediately(query.text)
						submitted = true
						keyboard?.hide()
						// Note: We MUST change the focus to somewhere else when the keyboard is submitted because some vendors (like Amazon)
						// will otherwise just keep showing a (fullscreen) keyboard, soft-locking the app.
						resultFocusRequester.requestFocus()
					},
					modifier = Modifier
						.weight(1f)
						.focusRequester(textInputFocusRequester),
				)
			}

			// The leanback code has its own awful focus handling that doesn't work properly with Compose view inteop to workaround this
			// issue we add custom behavior that only allows focus exit when the current selected row is the first one. Additionally when
			// we do switch the focus, we reset the leanback state so it won't cause weird behavior when focus is regained
			var rowsSupportFragment by remember { mutableStateOf<RowsSupportFragment?>(null) }
			val statusText = when {
				state.loading -> stringResource(R.string.search_loading)
				state.failedGroups.isNotEmpty() && state.hasResults -> stringResource(
					R.string.search_partial_failure, state.failedGroups.joinToString { getString(it) }
				)
				state.failedGroups.isNotEmpty() -> stringResource(R.string.search_failed)
				state.completed && !state.hasResults -> stringResource(R.string.search_empty)
				!state.completed -> stringResource(R.string.search_guidance)
				else -> null
			}
			if (statusText != null) {
				Column(Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
					.background(JellyfinTheme.colorScheme.background.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
					.padding(12.dp).then(
					if (!state.hasResults) Modifier.focusRequester(resultFocusRequester).focusable() else Modifier
				)) {
					Text(statusText, color = JellyfinTheme.colorScheme.onBackground)
					if (!state.loading && state.failedGroups.isNotEmpty()) {
						Button(onClick = { viewModel.retry(); submitted = true }) {
							Text(stringResource(R.string.search_retry))
						}
					}
				}
			}

			if (state.hasResults) {
				AndroidFragment<RowsSupportFragment>(
					modifier = Modifier
						.focusGroup()
						.focusRequester(resultFocusRequester)
						.focusProperties {
							onExit = {
								val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
								if (requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected) {
									cancelFocusChange()
								} else {
									rowsSupportFragment?.selectedPosition = 0
									rowsSupportFragment?.verticalGridView?.clearFocus()
								}
							}
						}
						.padding(top = 5.dp)
						.fillMaxSize(),
					onUpdate = { fragment ->
						val selection = viewModel.selection
						rowsSupportFragment = fragment
						fragment.adapter = searchFragmentDelegate.rowsAdapter
						fragment.onItemViewClickedListener = searchFragmentDelegate.onItemViewClickedListener
						fragment.onItemViewSelectedListener = searchFragmentDelegate.onItemViewSelectedListener
						searchFragmentDelegate.onSelectionChanged = { row, column -> viewModel.selection = row to column }
						fragment.setSelectedPosition(selection.first, false, ListRowPresenter.SelectItemViewHolderTask(selection.second))
					}
				)
			}
		}
	}
}
