package org.jellyfin.androidtv.ui.itemdetail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.TextView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.dp
import org.koin.core.context.GlobalContext

internal fun detailsControlBackground(context: Context): StateListDrawable {
	val strong = GlobalContext.get().get<UserPreferences>()[UserPreferences.strongFocus]
	fun color(id: Int) = ContextCompat.getColor(context, id)
	fun shape(fill: Int, border: Int? = null) = GradientDrawable().apply {
		setColor(fill); cornerRadius = 12.dp(context).toFloat()
		if (border != null) setStroke((if (strong) 3 else 2).dp(context), border)
	}
	return StateListDrawable().apply {
		addState(intArrayOf(android.R.attr.state_focused), shape(color(R.color.lumafin_focus_row), if (strong) Color.WHITE else color(R.color.lumafin_cyan)))
		addState(intArrayOf(android.R.attr.state_pressed), shape(color(R.color.lumafin_focus_row), color(R.color.lumafin_cyan)))
		addState(intArrayOf(android.R.attr.state_activated), shape(Color.rgb(48, 36, 72), color(R.color.lumafin_purple)))
		addState(intArrayOf(), shape(color(R.color.lumafin_raised)))
	}
}

internal fun detailsControlTextColors(context: Context) = ColorStateList(
	arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf(android.R.attr.state_activated), intArrayOf()),
	intArrayOf(ContextCompat.getColor(context, R.color.lumafin_text), ContextCompat.getColor(context, R.color.lumafin_purple),
		ContextCompat.getColor(context, R.color.lumafin_text)),
)

internal fun TextView.applyDetailsTrackStyle() {
	background = detailsControlBackground(context)
	setTextColor(detailsControlTextColors(context))
	typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
	minimumHeight = 44.dp(context)
	gravity = android.view.Gravity.CENTER_VERTICAL
	compoundDrawablePadding = 6.dp(context)
	val chevron = ContextCompat.getDrawable(context, R.drawable.ic_settings_chevron)?.mutate()?.apply {
		setBounds(0, 0, 16.dp(context), 16.dp(context))
		setTint(ContextCompat.getColor(context, R.color.lumafin_secondary_text))
	}
	setCompoundDrawablesRelative(null, null, chevron, null)
	if (android.os.Build.VERSION.SDK_INT >= 26) defaultFocusHighlightEnabled = false
}

internal fun TextView.setDetailsTrackValue(label: String, value: String) {
	text = SpannableStringBuilder().apply {
		append(label)
		setSpan(AbsoluteSizeSpan(11, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.lumafin_secondary_text)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		append('\n'); append(value)
	}
	contentDescription = "$label: $value"
}
