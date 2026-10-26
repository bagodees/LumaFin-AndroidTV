package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.java.KoinJavaComponent

/**
 * Wraps a view-based screen with the side menu when the LumaFin layout is on. Left from the leftmost
 * focusable control in [content] enters the menu on [active]; Right returns to where focus was.
 */
object SideMenuFrame {
	@JvmStatic
	fun isEnabled() = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.newHomeEnabled]

	@JvmStatic
	fun wrap(context: Context, content: View, active: MainToolbarActiveButton): View {
		if (!isEnabled()) return content
		val activeRequester = FocusRequester()
		var returnTo: View? = null
		val railView = ComposeView(context).apply {
			setContent {
				MainSideRail(active, activeRequester, contentFocusRequester = null, onActiveScreen = false, onRight = {
					val target = returnTo?.takeIf { it.isAttachedToWindow && it.isShown }
					if (target?.requestFocus() != true) content.requestFocus()
				})
			}
		}
		val density = context.resources.displayMetrics.density
		return object : FrameLayout(context) {
			override fun dispatchKeyEvent(event: KeyEvent): Boolean {
				val focused = findFocus()
				if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && focused != null &&
					content is ViewGroup && generateSequence(focused) { it.parent as? View }.any { it === content } &&
					FocusFinder.getInstance().findNextFocus(content, focused, View.FOCUS_LEFT) == null
				) {
					returnTo = focused
					// Entering through the view system lands on the nearest item; move to the active one
					if (railView.requestFocus()) {
						post { runCatching { activeRequester.requestFocus() } }
						return true
					}
				}
				return super.dispatchKeyEvent(event)
			}
		}.apply {
			addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
				marginStart = (RailCollapsedWidth.value * density).toInt()
			})
			addView(railView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT))
		}
	}
}
