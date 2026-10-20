package org.jellyfin.androidtv.ui.itemdetail

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import org.jellyfin.androidtv.ui.DetailRowView
import org.jellyfin.androidtv.util.dp

/** Top-down media header: title position is independent of episode and metadata rows. */
internal fun DetailRowView.showMediaOverviewLayout(hasThumbnail: Boolean, imageIsLogo: Boolean) {
	val b = binding
	b.root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
	b.detailsContent.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
	b.detailsContent.minimumHeight = 440.dp(context)
	listOf(b.infoTitle1, b.infoValue1, b.infoTitle2, b.infoValue2, b.infoTitle3, b.infoValue3)
		.forEach { it.visibility = View.GONE }
	b.fdGenreRow.visibility = if (b.fdGenreRow.text.isNullOrBlank()) View.GONE else View.VISIBLE
	b.fdSummaryText.maxLines = 5
	b.fdThumb.scaleType = ImageView.ScaleType.CENTER_CROP
	b.mainImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
	b.mainImage.background = null
	b.mainImage.layoutParams = b.mainImage.layoutParams.apply {
		width = ViewGroup.LayoutParams.MATCH_PARENT
		height = ViewGroup.LayoutParams.MATCH_PARENT
	}
	val infoHeight = b.fdMainInfoRow.layoutParams.height
	val visibility = listOf(b.fdTitle, b.fdSubtitle, b.fdMainInfoRow, b.fdGenreRow, b.fdSummaryText,
		b.fdButtonRow, b.mainImageContainer, b.fdThumb, b.fdTrackColumn).associate { it.id to it.visibility }
	ConstraintSet().apply {
		clone(b.detailsContent)
		for (id in listOf(b.fdTitle.id, b.fdSubtitle.id, b.fdMainInfoRow.id, b.fdGenreRow.id,
			b.fdSummaryText.id, b.fdButtonRow.id, b.mainImageContainer.id, b.fdThumb.id, b.fdTrackColumn.id)) clear(id)
		fun textColumn(id: Int, above: Int, gap: Int) {
			constrainWidth(id, 0)
			constrainHeight(id, ViewGroup.LayoutParams.WRAP_CONTENT)
			connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 300.dp(context))
			connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 52.dp(context))
			connect(id, ConstraintSet.TOP, above, if (above == ConstraintSet.PARENT_ID) ConstraintSet.TOP else ConstraintSet.BOTTOM, gap.dp(context))
		}
		textColumn(b.fdTitle.id, ConstraintSet.PARENT_ID, 64)
		textColumn(b.fdSubtitle.id, b.fdTitle.id, 6)
		textColumn(b.fdMainInfoRow.id, b.fdSubtitle.id, 8)
		setGoneMargin(b.fdMainInfoRow.id, ConstraintSet.TOP, 0)
		constrainHeight(b.fdMainInfoRow.id, infoHeight)
		textColumn(b.fdGenreRow.id, b.fdMainInfoRow.id, 8)
		textColumn(b.fdSummaryText.id, b.fdGenreRow.id, 12)
		textColumn(b.fdButtonRow.id, b.fdSummaryText.id, 18)
		connect(b.fdButtonRow.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 24.dp(context))
		setVerticalBias(b.fdButtonRow.id, 0f)

		constrainWidth(b.mainImageContainer.id, 240.dp(context))
		constrainHeight(b.mainImageContainer.id, (if (hasThumbnail || imageIsLogo) 88 else 200).dp(context))
		connect(b.mainImageContainer.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 40.dp(context))
		connect(b.mainImageContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 64.dp(context))
		constrainWidth(b.fdThumb.id, 240.dp(context))
		constrainHeight(b.fdThumb.id, 135.dp(context))
		connect(b.fdThumb.id, ConstraintSet.START, b.mainImageContainer.id, ConstraintSet.START)
		connect(b.fdThumb.id, ConstraintSet.TOP, b.mainImageContainer.id, ConstraintSet.BOTTOM, 16.dp(context))
		constrainWidth(b.fdTrackColumn.id, 240.dp(context))
		constrainHeight(b.fdTrackColumn.id, ViewGroup.LayoutParams.WRAP_CONTENT)
		connect(b.fdTrackColumn.id, ConstraintSet.START, b.mainImageContainer.id, ConstraintSet.START)
		connect(b.fdTrackColumn.id, ConstraintSet.TOP, b.fdThumb.id, ConstraintSet.BOTTOM, 8.dp(context))
		connect(b.fdTrackColumn.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 24.dp(context))
		setVerticalBias(b.fdTrackColumn.id, 0f)
		visibility.forEach { (id, value) -> setVisibility(id, value) }
		applyTo(b.detailsContent)
	}
}
