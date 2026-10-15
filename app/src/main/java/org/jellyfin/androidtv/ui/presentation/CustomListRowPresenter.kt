package org.jellyfin.androidtv.ui.presentation

import android.view.View
import androidx.core.view.isVisible
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter
import kotlin.math.roundToInt

open class CustomListRowPresenter @JvmOverloads constructor(
	private val topPadding: Int? = null
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
		}

		val view = holder.view?.parent as? View ?: return
		if (topPadding != null) view.setPadding(view.paddingLeft, topPadding, view.paddingRight, view.paddingBottom)

		// Hide header view when the item doesn't have one
		holder.headerViewHolder.view.isVisible = !(item is ListRow && item.headerItem == null)
	}
}
