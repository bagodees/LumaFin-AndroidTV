package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.java.KoinJavaComponent

/**
 * Wraps a view-based screen with the side menu when the LumaFin layout is on. Left from the leftmost
 * focusable control in [content] enters the menu on [active]; Right returns to where focus was.
 */
object SideMenuFrame {
	@JvmStatic
	fun isEnabled() = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.newHomeEnabled]

	@JvmStatic
	@JvmOverloads
	fun wrap(context: Context, content: View, active: MainToolbarActiveButton, library: SideMenuLibrary? = null): View {
		if (!isEnabled()) return content
		val activeRequester = FocusRequester()
		val frame = Frame(context, content, activeRequester)
		frame.library = library
		val railView = ComposeView(context).apply {
			setContent {
				MainSideRail(active, activeRequester, contentFocusRequester = null, onActiveScreen = false, library = frame.library, onRight = {
					val target = frame.returnTo?.takeIf { it.isAttachedToWindow && it.isShown }
					if (target?.requestFocus() != true) content.requestFocus()
				})
			}
		}
		frame.rail = railView
		// Lets the open menu dim the page beside it
		frame.clipChildren = false
		val density = context.resources.displayMetrics.density
		frame.addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
			marginStart = (RailCollapsedWidth.value * density).toInt()
		})
		frame.addView(railView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT))
		return frame
	}

	/** Updates which library the menu highlights, for screens that only know it after loading (details pages). */
	@JvmStatic
	fun setLibrary(view: View?, item: BaseItemDto?) {
		(view as? Frame)?.library = libraryOf(item)
	}

	/** A library itself matches by id; anything else by the kind of library it lives in. */
	@JvmStatic
	fun libraryOf(item: BaseItemDto?): SideMenuLibrary? {
		if (item == null) return null
		if (item.type == BaseItemKind.USER_VIEW || item.type == BaseItemKind.COLLECTION_FOLDER) return SideMenuLibrary(item.id, item.collectionType)
		val type = when (item.type) {
			BaseItemKind.MOVIE -> CollectionType.MOVIES
			BaseItemKind.SERIES, BaseItemKind.SEASON, BaseItemKind.EPISODE -> CollectionType.TVSHOWS
			BaseItemKind.MUSIC_ALBUM, BaseItemKind.MUSIC_ARTIST, BaseItemKind.AUDIO -> CollectionType.MUSIC
			BaseItemKind.MUSIC_VIDEO -> CollectionType.MUSICVIDEOS
			BaseItemKind.BOX_SET -> CollectionType.BOXSETS
			BaseItemKind.PHOTO, BaseItemKind.PHOTO_ALBUM -> CollectionType.PHOTOS
			BaseItemKind.TV_CHANNEL, BaseItemKind.PROGRAM, BaseItemKind.RECORDING, BaseItemKind.LIVE_TV_CHANNEL, BaseItemKind.LIVE_TV_PROGRAM -> CollectionType.LIVETV
			else -> item.collectionType
		} ?: return null
		return SideMenuLibrary(null, type)
	}

	/** For the Live TV screens, which highlight the Live TV library. */
	@JvmStatic
	fun liveTv() = SideMenuLibrary(null, CollectionType.LIVETV)

	private class Frame(
		context: Context,
		private val content: View,
		private val activeRequester: FocusRequester,
	) : FrameLayout(context) {
		var rail: View? = null
		var returnTo: View? = null
		var library by mutableStateOf<SideMenuLibrary?>(null)

		// Compose content leaves for the menu on its own, so note where focus came from as it arrives
		private val focusWatcher = android.view.ViewTreeObserver.OnGlobalFocusChangeListener { old, new ->
			fun View?.inside(parent: View?) = this != null && parent != null && generateSequence(this) { it.parent as? View }.any { it === parent }
			if (new.inside(rail) && old.inside(content)) returnTo = old
		}

		override fun onAttachedToWindow() {
			super.onAttachedToWindow()
			viewTreeObserver.addOnGlobalFocusChangeListener(focusWatcher)
		}

		override fun onDetachedFromWindow() {
			viewTreeObserver.removeOnGlobalFocusChangeListener(focusWatcher)
			super.onDetachedFromWindow()
		}

		override fun dispatchKeyEvent(event: KeyEvent): Boolean {
			val focused = findFocus()
			val railView = rail
			// Compose content (like a library page's tabs) moves focus itself and leaves for the menu on its own
			val inCompose = focused != null && generateSequence(focused) { it.parent as? View }.any { it is ComposeView && it !== railView }
			// Only from the left side: tools on the right (a grid's A-Z strip and filters) move left into the content
			val onLeftSide = focused != null && android.graphics.Rect().also { rect ->
				focused.getDrawingRect(rect)
				offsetDescendantRectToMyCoords(focused, rect)
			}.left < width / 2
			if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && focused != null && railView != null && !inCompose && onLeftSide &&
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
	}
}
