package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Draws a small tick on top of the seek bar at each chapter start. */
class ChapterTicksView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : View(context, attrs) {
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() }
	private val tickWidth = resources.displayMetrics.density * 2f
	private var fractions = FloatArray(0)

	init {
		isFocusable = false
		importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
	}

	fun setChapters(positionsMs: LongArray, durationMs: Long) {
		fractions = if (durationMs <= 0L) FloatArray(0)
		else positionsMs.filter { it > 0L && it < durationMs }.map { it.toFloat() / durationMs }.toFloatArray()
		invalidate()
	}

	override fun onDraw(canvas: Canvas) {
		val top = height * 0.2f
		val bottom = height * 0.8f
		for (fraction in fractions) {
			val x = fraction * width
			canvas.drawRect(x - tickWidth / 2, top, x + tickWidth / 2, bottom, paint)
		}
	}
}
