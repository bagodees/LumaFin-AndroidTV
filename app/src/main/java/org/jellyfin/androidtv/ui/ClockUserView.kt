package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import org.jellyfin.androidtv.databinding.ClockUserBugBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClockUserView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	private val binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), this, true)
	private val userPreferences by inject<UserPreferences>()
	private val navigationRepository by inject<NavigationRepository>()

	var isVideoPlayer = false
		set(value) {
			field = value
			updateClockVisibility()
		}

	val homeButton get() = binding.home

	/**
	 * LumaFin screens with the side menu: no Home button, and the clock in the same spot and font as
	 * Home's (20dp from the top, 32dp from the right edge).
	 */
	fun matchHomeClock() {
		binding.home.visibility = GONE
		binding.clock.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
		binding.clock.setPaddingRelative(0, 0, 0, 0)
		binding.clock.includeFontPadding = false
		(binding.root.layoutParams as? MarginLayoutParams)?.let { it.topMargin = 0; it.marginEnd = 0; binding.root.layoutParams = it }
		val density = resources.displayMetrics.density
		(layoutParams as? MarginLayoutParams)?.let {
			it.topMargin = (20 * density).toInt()
			it.marginEnd = (32 * density).toInt()
			layoutParams = it
		}
	}

	init {
		updateClockVisibility()

		binding.home.setOnClickListener {
			navigationRepository.reset(Destinations.home, clearHistory = true)
		}
	}

	private fun updateClockVisibility() {
		val showClock = userPreferences[UserPreferences.clockBehavior]

		binding.clock.isVisible = when (showClock) {
			ClockBehavior.ALWAYS -> true
			ClockBehavior.NEVER -> false
			ClockBehavior.IN_VIDEO -> isVideoPlayer
			ClockBehavior.IN_MENUS -> !isVideoPlayer
		}

		binding.home.isVisible = !isVideoPlayer
	}
}
