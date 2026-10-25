package org.jellyfin.androidtv.ui.browsing

import android.content.Context
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.databinding.HorizontalGridBrowseBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.home.FocusedTitleInfo
import org.jellyfin.androidtv.ui.home.MainSideRail
import org.jellyfin.androidtv.ui.home.RailCollapsedWidth
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent

/**
 * The LumaFin layout for library grids: the side menu on the left and a Home-style preview of the
 * selected item above the grid. With the classic Jellyfin style the screen is left unchanged.
 */
class BrowseGridLayout(private val binding: HorizontalGridBrowseBinding, private val grid: () -> View?) {
	val enabled = isEnabled()
	private val item = MutableStateFlow<BaseItemDto?>(null)
	private var rail: View? = null
	private val activeRequester = FocusRequester()

	companion object {
		const val SIDE_MENU_WIDTH_DP = 64
		/** How much taller the preview is than the classic title and info row. */
		const val EXTRA_PREVIEW_HEIGHT_DP = 100

		@JvmStatic
		fun isEnabled() = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.newHomeEnabled]
	}

	/** Returns the screen's root view, wrapped with the side menu when the LumaFin layout is on. */
	fun wrap(context: Context): View {
		val root = binding.root
		if (!enabled) return root
		installPreview(context)

		val railView = ComposeView(context).apply {
			setContent {
				MainSideRail(MainToolbarActiveButton.Libraries, activeRequester, contentFocusRequester = null, onRight = { grid()?.requestFocus() })
			}
		}
		rail = railView
		val density = context.resources.displayMetrics.density
		return object : FrameLayout(context) {
			// Left from the grid's first column enters the side menu
			override fun dispatchKeyEvent(event: KeyEvent): Boolean {
				val gridView = grid() as? ViewGroup
				val focused = findFocus()
				if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && gridView != null && focused != null &&
					generateSequence(focused) { it.parent as? View }.any { it === gridView } &&
					FocusFinder.getInstance().findNextFocus(gridView, focused, View.FOCUS_LEFT) == null
				) {
					// Entering through the view system lands on the nearest item; move to Libraries
					if (railView.requestFocus()) {
						post { runCatching { activeRequester.requestFocus() } }
						return true
					}
				}
				return super.dispatchKeyEvent(event)
			}
		}.apply {
			addView(root, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
				marginStart = (RailCollapsedWidth.value * density).toInt()
			})
			addView(railView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT))
		}
	}

	/** Replaces the title and info row with the Home-style preview, keeping the clock and filter tools. */
	private fun installPreview(context: Context) {
		val preview = ComposeView(context).apply {
			id = View.generateViewId()
			setContent {
				val current by item.collectAsState()
				Box(modifier = Modifier.fillMaxWidth().height(196.dp).padding(start = 48.dp, top = 20.dp, end = 300.dp)) {
					current?.let {
						Column(
							verticalArrangement = Arrangement.spacedBy(8.dp),
							modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 640.dp),
						) { FocusedTitleInfo(it) }
					}
				}
			}
		}
		val root = binding.root as RelativeLayout
		root.addView(preview, 0, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
			addRule(RelativeLayout.ALIGN_PARENT_TOP)
		})
		binding.title.visibility = View.GONE
		binding.infoRow.visibility = View.GONE
		(binding.rowsFragment.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.BELOW, preview.id)
		(binding.toolBar.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.BELOW, binding.clock.id)
	}

	fun setItem(baseItem: BaseItemDto?) {
		item.value = baseItem
	}
}
