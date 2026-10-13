package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.view.children
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewButtonAlphaPickerBinding

class AlphaPickerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : HorizontalScrollView(context, attrs, defStyleAttr, defStyleRes) {
	var onAlphaSelected: (letter: Char) -> Unit = {}
	private val buttonRow = LinearLayout(context)

	init {
		isFocusable = false
		isFocusableInTouchMode = false
		isHorizontalScrollBarEnabled = false

        addView(buttonRow)
        setAvailableLetters("#${resources.getString(R.string.byletter_letters)}")
    }

    fun showLoading() {
        setAvailableLetters("")
        buttonRow.addView(android.widget.TextView(context).apply {
            text = context.getString(R.string.loading)
            setPadding(16, 0, 16, 0)
        })
    }

    fun setAvailableLetters(available: String) {
        buttonRow.removeAllViews()
        val edgePadding = (8 * resources.displayMetrics.density).toInt()
        buttonRow.setPadding(edgePadding, 0, edgePadding, 0)
        val letters = "*$available…"

		letters.forEach { letter ->
			val binding = ViewButtonAlphaPickerBinding.inflate(LayoutInflater.from(context), this, false)
			binding.button.apply {
				text = when (letter) {
					'*' -> context.getString(R.string.alpha_all)
					'#' -> "0–9"
					'…' -> context.getString(R.string.alpha_other)
					else -> letter.toString()
				}
				tag = letter
				isAllCaps = false
				setSingleLine(true)
				textSize = 14f
				minimumWidth = (26 * resources.displayMetrics.density).toInt()
				minWidth = minimumWidth
				val padding = ((if (letter == '*' || letter == '#' || letter == '…') 8 else 4) * resources.displayMetrics.density).toInt()
				setPadding(padding, paddingTop, padding, paddingBottom)
				setOnClickListener { _ ->
					onAlphaSelected(letter)
				}
			}

			buttonRow.addView(binding.root)
		}

	}

	fun contentWidth(maximum: Int): Int {
		buttonRow.measure(
			android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
			android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
		)
		return buttonRow.measuredWidth.coerceAtMost(maximum)
	}

	fun focus(letter: Char) {
		val buttons = (getChildAt(0) as LinearLayout).children.filterIsInstance<Button>().toList()
		(buttons.firstOrNull { it.tag == letter } ?: buttons.lastOrNull())?.requestFocus()
	}
}
