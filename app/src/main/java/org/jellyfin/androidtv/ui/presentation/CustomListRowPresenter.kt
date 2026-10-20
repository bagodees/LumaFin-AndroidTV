package org.jellyfin.androidtv.ui.presentation

import android.view.View
import android.view.KeyEvent
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import androidx.leanback.widget.ViewHolderTask as GridViewHolderTask
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter
import kotlin.math.roundToInt

open class CustomListRowPresenter @JvmOverloads constructor(
	private val topPadding: Int? = null,
	private val keepUpWithinRows: Boolean = false,
) : ListRowPresenter() {
	init {
		headerPresenter = CustomRowHeaderPresenter()
		enableChildRoundedCorners(false)
	}

	override fun isUsingDefaultShadow() = false
	override fun isUsingDefaultListSelectEffect() = false

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)
		if (holder is ListRowPresenter.ViewHolder) {
			// Share compact focus clearance across Home and Libraries. Compose
			// owns the artwork clipping and dimming, so no native wrapper is needed.
			holder.gridView.horizontalSpacing = (14 * holder.view.resources.displayMetrics.density).roundToInt()
			holder.gridView.setFocusDrawingOrderEnabled(true)
			if (keepUpWithinRows) holder.gridView.setOnKeyInterceptListener { event ->
				if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyInterceptListener false
				if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
					val count = holder.gridView.adapter?.itemCount ?: 0
					return@setOnKeyInterceptListener count > 0 && holder.gridView.selectedPosition == count - 1
				}
				if (event.keyCode != KeyEvent.KEYCODE_DPAD_UP) return@setOnKeyInterceptListener false
				val rows = generateSequence(holder.gridView.parent) { it.parent }.filterIsInstance<VerticalGridView>().firstOrNull()
					?: return@setOnKeyInterceptListener false
				val position = rows.findContainingViewHolder(holder.view)?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
				if (position <= 0) return@setOnKeyInterceptListener false
				val column = holder.gridView.selectedPosition.coerceAtLeast(0)
				// Geometric focus search can prefer the distant Home icon when the preceding
				// row is shorter. Select that row explicitly and clamp to its final card.
				rows.setSelectedPosition(position - 1, object : GridViewHolderTask {
					override fun run(viewHolder: RecyclerView.ViewHolder) {
						if (!rows.isAttachedToWindow || !rows.hasFocus()) return
						val target = (viewHolder as? ItemBridgeAdapter.ViewHolder)?.viewHolder ?: return
						if (target is ListRowPresenter.ViewHolder) {
							val count = target.gridView.adapter?.itemCount ?: 0
							if (count > 0) target.gridView.selectedPosition = column.coerceAtMost(count - 1)
							target.gridView.requestFocus()
						} else target.view.requestFocus(View.FOCUS_UP)
					}
				})
				true
			}
		}

		val view = holder.view?.parent as? View ?: return
		if (topPadding != null) view.setPadding(view.paddingLeft, topPadding, view.paddingRight, view.paddingBottom)

		// Hide header view when the item doesn't have one
		holder.headerViewHolder.view.isVisible = !(item is ListRow && item.headerItem == null)
	}

	override fun onUnbindRowViewHolder(holder: RowPresenter.ViewHolder) {
		if (keepUpWithinRows && holder is ListRowPresenter.ViewHolder) holder.gridView.setOnKeyInterceptListener(null)
		super.onUnbindRowViewHolder(holder)
	}
}
