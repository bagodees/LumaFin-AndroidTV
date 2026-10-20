package org.jellyfin.androidtv.ui.itemdetail

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.DetailRowView
import org.jellyfin.androidtv.util.dp

/** Portrait-led person header, independent of the media metadata/track columns. */
internal fun DetailRowView.showPersonOverviewLayout() {
	val b = binding
	b.root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
	b.detailsContent.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
	b.detailsContent.minimumHeight = 310.dp(context)
	listOf(b.infoTitle1, b.infoValue1, b.infoTitle2, b.infoValue2, b.infoTitle3, b.infoValue3,
		b.fdGenreRow, b.fdTrackColumn, b.fdSubtitle, b.mainImageContainer).forEach { it.visibility = View.GONE }
	b.fdThumb.visibility = View.VISIBLE
	b.fdThumb.scaleType = ImageView.ScaleType.CENTER_CROP
	b.fdThumb.maxHeight = Int.MAX_VALUE
	b.fdTitle.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
	b.fdTitle.setTextColor(context.getColor(R.color.lumafin_text))
	b.fdTitle.textSize = 28f
	b.fdSummaryText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
	b.fdSummaryText.background = detailsControlBackground(context, transparentAtRest = true)
	b.fdSummaryText.setPadding(8.dp(context), 6.dp(context), 8.dp(context), 6.dp(context))
	if (android.os.Build.VERSION.SDK_INT >= 26) b.fdSummaryText.defaultFocusHighlightEnabled = false
	b.fdSummaryText.maxLines = 4
	b.fdSummaryText.setLineSpacing(2.dp(context).toFloat(), 1f)

	ConstraintSet().apply {
		clone(b.detailsContent)
		for (id in listOf(b.fdTitle.id, b.fdMainInfoRow.id, b.fdSummaryText.id, b.fdButtonRow.id, b.fdThumb.id)) clear(id)
		fun textColumn(id: Int, above: Int, gap: Int) {
			constrainWidth(id, 0)
			constrainHeight(id, ViewGroup.LayoutParams.WRAP_CONTENT)
			connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 220.dp(context))
			connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 52.dp(context))
			connect(id, ConstraintSet.TOP, above, if (above == ConstraintSet.PARENT_ID) ConstraintSet.TOP else ConstraintSet.BOTTOM, gap.dp(context))
		}
		textColumn(b.fdTitle.id, ConstraintSet.PARENT_ID, 64)
		textColumn(b.fdMainInfoRow.id, b.fdTitle.id, 8)
		// The embedded Compose info row needs a bounded height inside a wrapping header.
		constrainHeight(b.fdMainInfoRow.id, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, resources.displayMetrics).toInt())
		textColumn(b.fdSummaryText.id, b.fdMainInfoRow.id, 10)
		textColumn(b.fdButtonRow.id, b.fdSummaryText.id, 16)
		connect(b.fdButtonRow.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 24.dp(context))
		setVerticalBias(b.fdButtonRow.id, 0f)
		constrainWidth(b.fdThumb.id, 140.dp(context))
		constrainHeight(b.fdThumb.id, 210.dp(context))
		connect(b.fdThumb.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 52.dp(context))
		connect(b.fdThumb.id, ConstraintSet.TOP, b.fdTitle.id, ConstraintSet.TOP)
		applyTo(b.detailsContent)
	}
}
