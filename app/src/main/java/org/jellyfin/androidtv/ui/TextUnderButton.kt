package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import org.jellyfin.androidtv.databinding.TextUnderButtonBinding
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.ui.itemdetail.detailsControlBackground
import org.jellyfin.androidtv.ui.itemdetail.detailsControlTextColors

class TextUnderButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = TextUnderButtonBinding.inflate(LayoutInflater.from(context), this, true)

	init {
		isFocusable = true
		isFocusableInTouchMode = true
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		if (AndroidVersion.isAtLeastO) defaultFocusHighlightEnabled = false
	}

	private var rowStyle = false

	fun setLabel(text: String?) {
		binding.label.isVisible = text != null
		binding.label.text = if (rowStyle) text?.replace("\n", " · ") else text
		binding.imageButton.contentDescription = text
		contentDescription = text
	}

	/** Used by the shared details presenter, including person, series and season actions. */
	fun applyDetailsStyle() {
		binding.imageButton.background = detailsControlBackground(context)
		binding.imageButton.foreground = null
		binding.imageButton.imageTintList = detailsControlTextColors(context)
		binding.imageButton.setPadding(20.dp(context), 8.dp(context), 20.dp(context), 8.dp(context))
		binding.label.alpha = 1f
		binding.label.setTextColor(detailsControlTextColors(context))
		binding.label.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
		(binding.label.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.apply {
			topMargin = 5.dp(context)
			binding.label.layoutParams = this
		}
		val label = binding.label.text
		val separator = label.indexOf('\n')
		if (separator >= 0) binding.label.text = android.text.SpannableString(label).apply {
			setSpan(android.text.style.AbsoluteSizeSpan(11, true), separator + 1, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
			setSpan(android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(context, org.jellyfin.androidtv.R.color.lumafin_secondary_text)),
				separator + 1, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
		binding.label.isDuplicateParentStateEnabled = true
	}

	/** LumaFin details layout (#85): icon and label side by side on one pill. */
	fun applyDetailsRowStyle() {
		rowStyle = true
		background = detailsControlBackground(context)
		minimumHeight = 36.dp(context)
		setPadding(14.dp(context), 0, 16.dp(context), 0)
		binding.root.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL)
		binding.imageButton.apply {
			background = null
			foreground = null
			imageTintList = detailsControlTextColors(context)
			minimumWidth = 0
			minimumHeight = 0
			setPadding(0)
			layoutParams = RelativeLayout.LayoutParams(18.dp(context), 18.dp(context)).apply {
				addRule(RelativeLayout.ALIGN_PARENT_START)
				addRule(RelativeLayout.CENTER_VERTICAL)
			}
		}
		binding.label.apply {
			layoutParams = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
				addRule(RelativeLayout.END_OF, binding.imageButton.id)
				addRule(RelativeLayout.CENTER_VERTICAL)
				marginStart = 8.dp(context)
			}
			alpha = 1f
			maxLines = 1
			gravity = Gravity.CENTER_VERTICAL
			setPadding(0)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setTextColor(detailsControlTextColors(context))
			typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
			text = text?.toString()?.replace("\n", " · ")
			isDuplicateParentStateEnabled = true
		}
	}

	/**
	 * Row style only: an icon-only pill when the details button row is short on room. The label stays
	 * as the content description, so it's still read out.
	 */
	var compact = false
		set(value) {
			if (!rowStyle || field == value) return
			field = value
			binding.label.isVisible = !value && !binding.label.text.isNullOrEmpty()
			if (value) setPadding(14.dp(context), 0, 14.dp(context), 0) else setPadding(14.dp(context), 0, 16.dp(context), 0)
		}

	fun setIcon(@DrawableRes resource: Int, maxHeight: Int? = null) {
		binding.imageButton.setImageResource(resource)
		binding.imageButton.maxHeight = maxHeight ?: Int.MAX_VALUE
	}

	fun setPadding(padding: Int?) {
		binding.imageButton.setPadding(padding?.dp(context) ?: 0)
	}

	companion object {
		@JvmStatic
		@Suppress("LongParameterList")
		fun create(
			context: Context,
			@DrawableRes icon: Int,
			maxHeight: Int? = null,
			padding: Int? = null,
			label: String? = null,
			onClickListener: OnClickListener
		) = TextUnderButton(context).apply {
			setLabel(label)
			setIcon(icon, maxHeight)
			setPadding(padding)
			setOnClickListener(onClickListener)
		}
	}
}
