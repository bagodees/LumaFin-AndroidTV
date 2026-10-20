package org.jellyfin.androidtv.ui.base.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.dp
import org.koin.core.context.GlobalContext

/** Shared, remote-first menu panel. Rows show labels and values without native list-dialog chrome. */
internal class LumaFinMenuDialog(
	private val context: Context,
	/** Closes the panel when this owner stops; null for callers that dismiss it themselves. */
	private val owner: LifecycleOwner?,
	title: String,
	description: String? = null,
	private val fitContent: Boolean = false,
) : DefaultLifecycleObserver, DialogInterface {
	private val dialog = Dialog(context)
	private val root = LinearLayout(context).apply {
		orientation = LinearLayout.VERTICAL
		setPadding(20.dp(context), 20.dp(context), 20.dp(context), 16.dp(context))
		background = shape(color(R.color.lumafin_surface), 20)
	}
	private val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
	private val footer = LinearLayout(context).apply { gravity = Gravity.END; orientation = LinearLayout.HORIZONTAL }
	private val targets = mutableListOf<View>()
	private var dismissed: (() -> Unit)? = null
	private var message: TextView? = null
	private val heading: TextView
	private val rowTargets = mutableListOf<View>()
	private val strongFocus = runCatching { GlobalContext.get().get<UserPreferences>()[UserPreferences.strongFocus] }.getOrDefault(false)
	private val highContrast = runCatching { GlobalContext.get().get<UserPreferences>()[UserPreferences.highContrast] }.getOrDefault(false)

	init {
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
		heading = text(title, 24f, true).apply { setTextColor(color(R.color.lumafin_text)) }
		root.addView(heading)
		if (!description.isNullOrBlank()) root.addView(text(description, 13f).apply {
			message = this
			setPadding(0, 6.dp(context), 0, 12.dp(context))
			setTextColor(color(if (highContrast) R.color.lumafin_text else R.color.lumafin_secondary_text))
		})
		val scroll = ScrollView(context).apply {
			isFillViewport = false
			scrollBarStyle = View.SCROLLBARS_OUTSIDE_INSET
			clipToPadding = false
			setPadding(2.dp(context), 8.dp(context), 2.dp(context), 8.dp(context))
			addView(rows)
		}
		root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
		root.addView(footer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
		dialog.setContentView(root)
		dialog.setOnDismissListener { owner?.lifecycle?.removeObserver(this); dismissed?.invoke() }
		owner?.lifecycle?.addObserver(this)
	}

	fun row(label: String, value: String? = null, selected: Boolean? = null, navigates: Boolean = true, enabled: Boolean = true, action: () -> Unit): View {
		val row = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			minimumHeight = 56.dp(context)
			setPadding(14.dp(context), 9.dp(context), 14.dp(context), 9.dp(context))
			isFocusable = true
			isClickable = true
			isSelected = selected == true
			background = focusBackground()
			isEnabled = enabled
			alpha = if (enabled) 1f else 0.5f
			setOnClickListener { action() }
		}
		val labels = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
		labels.addView(text(label, 16f, true).apply { setTextColor(color(R.color.lumafin_text)) })
		if (value != null) labels.addView(text(value, 13f).apply {
			setTextColor(color(if (highContrast) R.color.lumafin_text else R.color.lumafin_secondary_text))
			setPadding(0, 3.dp(context), 0, 0)
		})
		row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		if (selected != null || navigates) {
			// Drawn icons, matching the weight of the rest of the app: check for the saved choice,
			// an empty ring for other options and a chevron where another menu opens.
			val indicator = ImageView(context).apply {
				when (selected) {
					true -> { setImageResource(R.drawable.ic_check); setColorFilter(color(R.color.lumafin_cyan)) }
					false -> setImageDrawable(GradientDrawable().apply {
						shape = GradientDrawable.OVAL
						setStroke(2.dp(context), color(R.color.lumafin_muted))
						setSize(18.dp(context), 18.dp(context))
					})
					null -> { setImageResource(R.drawable.ic_settings_chevron); setColorFilter(color(R.color.lumafin_muted)) }
				}
				scaleType = ImageView.ScaleType.CENTER_INSIDE
			}
			row.addView(indicator, LinearLayout.LayoutParams(24.dp(context), 24.dp(context)).apply { marginStart = 12.dp(context) })
		}
		rows.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
			bottomMargin = 6.dp(context)
		})
		targets += row
		rowTargets += row
		return row
	}

	/** Scrollable read-only text; it takes focus so the remote can scroll long content. */
	fun body(value: CharSequence): TextView = text("", 16f).apply {
		text = value
		setTextColor(color(R.color.lumafin_text))
		setLineSpacing(4.dp(context).toFloat(), 1f)
		isFocusable = true
		rows.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
		targets += this
	}

	fun title(value: String) { heading.text = value }

	/** Removes all rows (not footer buttons), e.g. before rebuilding a live list. */
	fun clearRows() {
		targets.removeAll(rowTargets.toSet()); rowTargets.clear()
		rows.removeAllViews()
	}

	/** Index of the focused row, or -1. */
	fun focusedRow() = rowTargets.indexOfFirst { it.hasFocus() }
	fun focusRow(index: Int) { rowTargets.getOrNull(index.coerceIn(0, (rowTargets.size - 1).coerceAtLeast(0)))?.requestFocus() }

	/** On/off row matching the settings switch. Flips in place; the switch position and color carry the state. */
	fun toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
		var state = checked
		val on = context.getString(R.string.library_filter_on)
		val off = context.getString(R.string.library_filter_off)
		val track = FrameLayout(context)
		val thumb = View(context)
		track.addView(thumb, FrameLayout.LayoutParams(16.dp(context), 16.dp(context)))
		val row = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			minimumHeight = 56.dp(context)
			setPadding(14.dp(context), 9.dp(context), 14.dp(context), 9.dp(context))
			isFocusable = true
			isClickable = true
			background = focusBackground()
		}
		fun render() {
			track.background = shape(color(if (state) R.color.lumafin_cyan else R.color.lumafin_muted).let { if (state) it else Color.argb(128, Color.red(it), Color.green(it), Color.blue(it)) }, 11)
			thumb.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color(if (state) R.color.lumafin_background else R.color.lumafin_text)) }
			(thumb.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL or (if (state) Gravity.END else Gravity.START)
			thumb.requestLayout()
			row.contentDescription = "$label, ${if (state) on else off}"
		}
		row.setOnClickListener { state = !state; render(); onChange(state) }
		row.addView(text(label, 16f, true).apply { setTextColor(color(R.color.lumafin_text)) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		track.setPadding(3.dp(context), 3.dp(context), 3.dp(context), 3.dp(context))
		row.addView(track, LinearLayout.LayoutParams(36.dp(context), 22.dp(context)).apply { marginStart = 12.dp(context) })
		render()
		rows.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
			bottomMargin = 6.dp(context)
		})
		targets += row
	}

	fun button(label: String, primary: Boolean = false, action: () -> Unit): TextView {
		val button = text(label, 15f, true).apply {
			gravity = Gravity.CENTER
			minimumHeight = 42.dp(context)
			setPadding(18.dp(context), 8.dp(context), 18.dp(context), 8.dp(context))
			setTextColor(color(if (primary) R.color.lumafin_background else R.color.lumafin_text))
			background = focusBackground(primary)
			isFocusable = true
			isClickable = true
			setOnClickListener { action() }
		}
		footer.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
			marginStart = 8.dp(context); topMargin = 8.dp(context)
		})
		if (targets.isEmpty()) targets += button
		return button
	}

	fun onDismiss(action: () -> Unit) { dismissed = action }
	val isShowing get() = dialog.isShowing
	fun description(value: String) { message?.text = value }
	override fun dismiss() = dialog.dismiss()
	override fun cancel() = dialog.cancel()
	/** [fullHeight] reserves the full panel height for content that is filled in after showing (e.g. live lists). */
	fun show(focus: Int = 0, fullHeight: Boolean = false) {
		val width = minOf(560.dp(context), context.resources.displayMetrics.widthPixels - 48.dp(context))
		var height = (if (rows.childCount == 0 && !fullHeight) 220 else 480).dp(context)
		if (fitContent) {
			val measureWidth = View.MeasureSpec.makeMeasureSpec(width - root.paddingLeft - root.paddingRight, View.MeasureSpec.EXACTLY)
			val unbounded = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
			height = root.paddingTop + root.paddingBottom
			for (index in 0 until root.childCount) {
				val child = root.getChildAt(index)
				if (child is ScrollView) {
					rows.measure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureWidth) - child.paddingLeft - child.paddingRight, View.MeasureSpec.EXACTLY), unbounded)
					height += rows.measuredHeight + child.paddingTop + child.paddingBottom
				} else {
					child.measure(measureWidth, unbounded)
					height += child.measuredHeight
				}
			}
			height = height.coerceAtMost(480.dp(context))
		}
		dialog.window?.apply {
			// These static menus do not need a GPU surface. Configure before showing:
			// resizing a newly shown window can stall the emulator EGL render thread.
			clearFlags(android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
			setBackgroundDrawableResource(android.R.color.transparent)
			addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			setDimAmount(0.65f)
			setWindowAnimations(0)
			setLayout(width, minOf(height, context.resources.displayMetrics.heightPixels - 48.dp(context)))
		}
		dialog.show()
		targets.getOrNull(focus.coerceIn(0, (targets.size - 1).coerceAtLeast(0)))?.requestFocus()
	}
	override fun onStop(owner: LifecycleOwner) { dismiss() }
	private fun color(id: Int) = ContextCompat.getColor(context, id)
	private fun text(value: String, size: Float, bold: Boolean = false) = TextView(context).apply {
		text = value; textSize = size
		typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
	}
	private fun shape(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
		setColor(fill); cornerRadius = radius.dp(context).toFloat()
		if (stroke != null) setStroke((if (strongFocus) 3 else 2).dp(context), stroke)
	}
	private fun focusBackground(primary: Boolean = false) = StateListDrawable().apply {
		addState(intArrayOf(android.R.attr.state_focused), shape(color(if (primary) R.color.lumafin_cyan else R.color.lumafin_focus_row), 12,
			if (strongFocus || primary) Color.WHITE else color(R.color.lumafin_cyan)))
		addState(intArrayOf(), shape(color(if (primary) R.color.lumafin_cyan else R.color.lumafin_raised), 12))
	}
}
