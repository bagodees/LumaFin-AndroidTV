package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.util.KeyProcessor
import org.koin.android.ext.android.inject

enum class LibrariesLoadState { Loading, Ready, Empty, Error }

class LibrariesFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		var rows by remember { mutableStateOf<LibrariesRowsFragment?>(null) }
		val initialState = remember { MutableStateFlow(LibrariesLoadState.Loading) }
		val state by (rows?.loadState ?: initialState).collectAsState()
		val focusRequester = remember { FocusRequester() }
		LaunchedEffect(state) {
			if (state == LibrariesLoadState.Ready) focusRequester.requestFocus()
		}
		Column {
			MainToolbar(MainToolbarActiveButton.Libraries)
			Box(Modifier.fillMaxSize()) {
				AndroidFragment<LibrariesRowsFragment>(
					modifier = Modifier.fillMaxSize().focusGroup().focusRequester(focusRequester).focusProperties {
						onExit = {
							if (requestedFocusDirection != FocusDirection.Up) cancelFocusChange()
							else rows?.verticalGridView?.clearFocus()
						}
					},
					onUpdate = { rows = it },
				)
				if (state != LibrariesLoadState.Ready) {
					Column(
						modifier = Modifier.align(Alignment.Center).padding(32.dp),
						verticalArrangement = Arrangement.spacedBy(16.dp),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Text(stringResource(when (state) {
							LibrariesLoadState.Loading -> R.string.home_libraries_loading
							LibrariesLoadState.Empty -> R.string.home_libraries_empty
							else -> R.string.home_libraries_failed
						}))
						if (state != LibrariesLoadState.Loading) Button(onClick = { rows?.reload() }) {
							Text(stringResource(R.string.home_shortcut_retry))
						}
					}
				}
			}
		}
	}
}

class LibrariesRowsFragment : RowsSupportFragment(), View.OnKeyListener {
	private val views by inject<UserViewsRepository>()
	private val launcher by inject<ItemLauncher>()
	private val keyProcessor by inject<KeyProcessor>()
	private val backgroundService by inject<BackgroundService>()
	val loadState = MutableStateFlow(LibrariesLoadState.Loading)
	private var currentItem: BaseRowItem? = null
	private var loadJob: Job? = null
	private val cards = MutableObjectAdapter<Any>(CardPresenter(true, 126))

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		adapter = MutableObjectAdapter<Row>(PositionableListRowPresenter()).apply {
			add(ListRow(HeaderItem(getString(R.string.home_section_library)), cards))
		}
		setOnItemViewClickedListener { _, item, _, _ ->
			(item as? BaseRowItem)?.baseItem?.let(launcher::launchUserView)
		}
		setOnItemViewSelectedListener { _, item, _, _ -> currentItem = item as? BaseRowItem }
	}

	override fun onResume() {
		super.onResume()
		backgroundService.clearBackgrounds()
		reload()
	}

	fun reload() {
		loadJob?.cancel()
		loadJob = viewLifecycleOwner.lifecycleScope.launch {
			loadState.value = LibrariesLoadState.Loading
			try {
				val libraries = views.views.first()
				cards.replaceAll(libraries.map(::BaseItemDtoBaseRowItem))
				loadState.value = if (libraries.isEmpty()) LibrariesLoadState.Empty else LibrariesLoadState.Ready
			} catch (error: CancellationException) {
				throw error
			} catch (_: Exception) {
				cards.clear()
				currentItem = null
				loadState.value = LibrariesLoadState.Error
			}
		}
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		if (event?.action != KeyEvent.ACTION_UP || keyCode !in setOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_Y)) return false
		return keyProcessor.handleKey(keyCode, currentItem, activity)
	}
}
