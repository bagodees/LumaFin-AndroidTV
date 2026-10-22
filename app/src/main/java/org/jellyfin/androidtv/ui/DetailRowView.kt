package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.FrameLayout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding

class DetailRowView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewRowDetailsBinding.inflate(LayoutInflater.from(context), this, true)

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN &&
			(binding.fdButtonRow.hasFocus() || binding.fdAudioChip.hasFocus())) {
			// Leanback searches inside its row before honoring an external nextFocusUpId.
			// Handle this boundary before the grid can redirect Up into the track column.
			val home = rootView.findViewById<ClockUserView>(R.id.fdClock)?.homeButton
			if (home?.isShown == true && home.requestFocus()) return true
		}
		return super.dispatchKeyEvent(event)
	}

	/**
	 * Keeps track of the last selected button and reselect it when navigating back to the buttons row.
	 */
	private val buttonsHierarchyChangeListener = object : OnHierarchyChangeListener {

		private var lastFocusedButton: View? = null

		private val focusChangeListener = OnFocusChangeListener { view, hasFocus ->
			// Restore last focused button when navigating back to button row
			if (hasFocus && lastFocusedButton != null) {
				lastFocusedButton?.requestFocus()
				lastFocusedButton = null
			}
			view.post {
				// Store last focused button when navigating away from button row
				if (binding.fdButtonRow.focusedChild == null) {
					lastFocusedButton = view
				}
			}
		}

		override fun onChildViewAdded(parent: View?, child: View?) {
			// Track selectors sit beside the actions, so geometric Up search can
			// bounce between Audio and the remembered action instead of the header.
			child?.nextFocusUpId = R.id.home
			child?.onFocusChangeListener = focusChangeListener
		}

		override fun onChildViewRemoved(parent: View?, child: View?) {
			child?.onFocusChangeListener = null
		}
	}

	init {
		binding.fdButtonRow.setOnHierarchyChangeListener(buttonsHierarchyChangeListener)
		binding.fdAudioChip.nextFocusUpId = R.id.home
		binding.mainImage.clipToOutline = true
	}
}
