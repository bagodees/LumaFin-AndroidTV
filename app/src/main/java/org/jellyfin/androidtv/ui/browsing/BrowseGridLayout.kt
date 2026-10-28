package org.jellyfin.androidtv.ui.browsing

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.HorizontalGridBrowseBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.home.FocusedTitleInfo
import org.jellyfin.androidtv.ui.home.MainSideRail
import org.jellyfin.androidtv.ui.home.PreviewLogo
import org.jellyfin.androidtv.ui.home.SideMenuLibrary
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
		/** Room between the side menu and the first column, so the focused card's zoom isn't clipped. */
		const val GRID_START_MARGIN_DP = 20
		/** The letter strip and tool column on the right edge. */
		const val RIGHT_COLUMNS_WIDTH_DP = 96
		/** Grid inset that lines the first column up with Home's row titles. */
		const val GRID_CONTENT_INSET_DP = 36
		/** Gap between grid cards, the same as between cards in Home rows. */
		const val GRID_CARD_GAP_DP = 14
		private const val LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"
		/** How much taller the preview is than the classic title and info row. */
		const val EXTRA_PREVIEW_HEIGHT_DP = 32
		/** Height of the fade behind the status line at the bottom. */
		private const val FOOTER_FADE_DP = 64

		@JvmStatic
		fun isEnabled() = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.newHomeEnabled]
	}

	/** The library this grid belongs to, highlighted in the side menu. */
	var library by mutableStateOf<SideMenuLibrary?>(null)

	/** Inside a tabbed library page, which already has the side menu and the clock. */
	var embedded = false

	/** Returns the screen's root view, wrapped with the side menu when the LumaFin layout is on. */
	fun wrap(context: Context): View {
		val root = binding.root
		if (!enabled) return root
		installPreview(context)
		if (embedded) {
			binding.clock.visibility = View.GONE
			// The tabbed page draws one background behind the tabs and the grid
			root.background = null
			return root
		}

		val railView = ComposeView(context).apply {
			setContent {
				MainSideRail(MainToolbarActiveButton.Libraries, activeRequester, contentFocusRequester = null, onRight = { grid()?.requestFocus() }, onActiveScreen = false, library = library)
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
			// Lets the open menu dim the page beside it
			clipChildren = false
		}
	}

	/** Replaces the title and info row with the Home-style preview, keeping the clock and filter tools. */
	private fun installPreview(context: Context) {
		val preview = ComposeView(context).apply {
			id = View.generateViewId()
			setContent {
				val current by item.collectAsState()
				Box(modifier = Modifier.fillMaxWidth().height(128.dp).padding(start = 48.dp, top = 2.dp, end = 120.dp)) {
					current?.let {
						// Top-aligned so the title lines up with the clock; the logo fills the right third like on Home
						Row(verticalAlignment = Alignment.Top, modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
							Column(
								verticalArrangement = Arrangement.spacedBy(8.dp),
								modifier = Modifier.weight(2f),
							) { FocusedTitleInfo(it) }
							PreviewLogo(it, Modifier.weight(1f).height(104.dp).padding(start = 32.dp))
						}
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
		// The side menu already leads Home
		binding.clock.homeButton.visibility = View.GONE
		// The footer lined up with the hidden title; line it up with the grid instead
		(binding.libraryFooter.layoutParams as RelativeLayout.LayoutParams).marginStart =
			((GRID_START_MARGIN_DP + GRID_CONTENT_INSET_DP) * context.resources.displayMetrics.density).toInt()
		// Cards may draw past the grid so the focused one can zoom; fade them out under the footer text
		val background = context.getColor(R.color.lumafin_background)
		val footerFade = View(context).apply {
			this.background = android.graphics.drawable.GradientDrawable(
				android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
				intArrayOf(background, androidx.core.graphics.ColorUtils.setAlphaComponent(background, 0)),
			)
		}
		root.addView(footerFade, root.indexOfChild(binding.libraryFooter), RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT,
			(FOOTER_FADE_DP * context.resources.displayMetrics.density).toInt(),
		).apply { addRule(RelativeLayout.ALIGN_PARENT_BOTTOM) })
		(binding.rowsFragment.layoutParams as RelativeLayout.LayoutParams).apply {
			addRule(RelativeLayout.BELOW, preview.id)
			marginStart = (GRID_START_MARGIN_DP * context.resources.displayMetrics.density).toInt()
		}
		(binding.toolBar.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.BELOW, binding.clock.id)
	}

	private val lettersVisible = MutableStateFlow(false)
	private val jumpLetter = MutableStateFlow<String?>(null)

	/**
	 * Puts an A-Z strip and the sort and filter tools in columns on the right edge, so both stay one
	 * press away from anywhere in the grid. Picking a letter calls [onLetter] ("#" for the start).
	 */
	fun installSideColumns(context: Context, onLetter: (String) -> Unit) {
		if (!enabled) return
		val density = context.resources.displayMetrics.density
		fun dp(value: Int) = (value * density).toInt()
		val root = binding.root as RelativeLayout

		binding.toolBar.orientation = LinearLayout.VERTICAL
		binding.toolBar.gravity = Gravity.CENTER_HORIZONTAL
		binding.toolBar.layoutParams = RelativeLayout.LayoutParams(dp(48), RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
			addRule(RelativeLayout.ALIGN_PARENT_END)
			addRule(RelativeLayout.BELOW, binding.clock.id)
			topMargin = dp(12)
			marginEnd = dp(12)
		}

		val letters = ComposeView(context).apply {
			id = View.generateViewId()
			setContent {
				val visible by lettersVisible.collectAsState()
				val current by jumpLetter.collectAsState()
				// Left from the strip goes back into the grid, not past it to the side menu
				if (visible) Box(Modifier.onPreviewKeyEvent { event ->
					event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && grid()?.requestFocus() == true
				}) { LetterStrip(LETTERS, current ?: "#", onLetter) }
			}
		}
		root.addView(letters, RelativeLayout.LayoutParams(dp(32), RelativeLayout.LayoutParams.MATCH_PARENT).apply {
			addRule(RelativeLayout.START_OF, binding.toolBar.id)
			addRule(RelativeLayout.ALIGN_PARENT_TOP)
			addRule(RelativeLayout.ABOVE, binding.libraryFooter.id)
			topMargin = dp(60)
			marginEnd = dp(4)
		})
		(binding.rowsFragment.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.START_OF, letters.id)
	}

	/** The strip only works for name-sorted lists. */
	fun setLettersVisible(visible: Boolean) {
		lettersVisible.value = visible
	}

	fun setJumpLetter(letter: String?) {
		jumpLetter.value = letter
	}

	fun setItem(baseItem: BaseItemDto?) {
		item.value = baseItem
	}
}

@Composable
private fun LetterStrip(letters: String, selected: String, onLetter: (String) -> Unit) {
	Column(modifier = Modifier.fillMaxHeight()) {
		for (letter in letters.map(Char::toString)) {
			val isSelected = letter == selected
			ButtonBase(
				onClick = { onLetter(letter) },
				shape = RoundedCornerShape(6.dp),
				colors = ButtonDefaults.colors(
					containerColor = Color.Transparent,
					contentColor = if (isSelected) LumaFinPalette.Cyan else LumaFinPalette.SecondaryText,
				),
				modifier = Modifier.weight(1f).fillMaxWidth(),
			) {
				Text(
					letter,
					fontSize = 13.sp,
					fontWeight = if (isSelected) FontWeight.Bold else null,
					modifier = Modifier.align(Alignment.Center),
				)
			}
		}
	}
}
