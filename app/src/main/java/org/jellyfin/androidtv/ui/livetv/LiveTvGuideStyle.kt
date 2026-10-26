package org.jellyfin.androidtv.ui.livetv

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.LiveTvGuideBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.home.SideMenuFrame
import org.jellyfin.androidtv.ui.itemdetail.detailsControlBackground
import org.jellyfin.androidtv.ui.itemdetail.detailsControlTextColors
import org.jellyfin.androidtv.util.dp
import org.koin.java.KoinJavaComponent

/** LumaFin look for the Live TV guide (#111): navy cells with the usual cyan focus outline. Classic keeps the old colors. */
object LiveTvGuideStyle {
	@JvmStatic
	fun isEnabled() = SideMenuFrame.isEnabled()

	/** Rounded guide cell; [tint] is the program-type color when "color code guide" is on, 0 otherwise. */
	@JvmStatic
	@JvmOverloads
	fun cellBackground(context: Context, tint: Int = 0): Drawable {
		val strong = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.strongFocus]
		fun color(id: Int) = ContextCompat.getColor(context, id)
		fun shape(fill: Int, border: Int? = null) = GradientDrawable().apply {
			setColor(fill)
			cornerRadius = 8.dp(context).toFloat()
			if (border != null) setStroke((if (strong) 3 else 2).dp(context), border)
		}
		val rest = if (tint != 0) ColorUtils.compositeColors(tint, color(R.color.lumafin_surface)) else color(R.color.lumafin_surface)
		val states = StateListDrawable().apply {
			addState(intArrayOf(android.R.attr.state_focused), shape(color(R.color.lumafin_focus_row), if (strong) Color.WHITE else color(R.color.lumafin_cyan)))
			addState(intArrayOf(), shape(rest))
		}
		// Leaves a thin gap between cells instead of the old border lines
		return InsetDrawable(states, 2.dp(context))
	}

	@JvmStatic
	fun styleProgramName(name: TextView) {
		name.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
		name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
		name.setTextColor(ContextCompat.getColor(name.context, R.color.lumafin_text))
	}

	@JvmStatic
	fun apply(binding: LiveTvGuideBinding) {
		val context = binding.root.context
		val background = ContextCompat.getColor(context, R.color.lumafin_background)
		listOf(binding.channelScroller, binding.programVScroller, binding.timelineHScroller, binding.displayDate)
			.forEach { it.setBackgroundColor(background) }
		binding.clock.homeButton.visibility = View.GONE
		binding.title.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
		binding.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
		binding.title.setTextColor(ContextCompat.getColor(context, R.color.lumafin_text))
		binding.summary.setTextColor(ContextCompat.getColor(context, R.color.lumafin_secondary_text))
		binding.programImage.clipToOutline = true
		binding.programImage.background = GradientDrawable().apply { cornerRadius = 12.dp(context).toFloat(); setColor(Color.TRANSPARENT) }
		// Same rounded buttons as the details pages
		listOf(binding.resetButton, binding.dateButton, binding.filterButton, binding.optionsButton).forEach { button: ImageButton ->
			button.background = detailsControlBackground(context)
			button.imageTintList = detailsControlTextColors(context)
			button.layoutParams = button.layoutParams.apply { width = 36.dp(context); height = 36.dp(context) }
			button.setPadding(8.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
		}
	}

	/** Timeline labels in the secondary text color. */
	@JvmStatic
	fun styleTimeLabel(label: TextView) {
		label.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(label.context, R.color.lumafin_secondary_text)))
		label.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
	}
}
