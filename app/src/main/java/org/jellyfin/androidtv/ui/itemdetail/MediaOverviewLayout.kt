package org.jellyfin.androidtv.ui.itemdetail

import android.view.View
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

/** LumaFin details header (#85): no poster column, the title lines up with the clock and the actions sit in one row. */
internal fun DetailRowView.showLumaFinOverviewLayout(hasLogo: Boolean) {
	val b = binding
	b.root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
	b.detailsContent.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
	b.detailsContent.minimumHeight = 0
	listOf(b.infoTitle1, b.infoValue1, b.infoTitle2, b.infoValue2, b.infoTitle3, b.infoValue3)
		.forEach { it.visibility = View.GONE }
	b.fdGenreRow.visibility = if (b.fdGenreRow.text.isNullOrBlank()) View.GONE else View.VISIBLE
	b.fdTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
	b.fdTitle.maxLines = 1
	b.fdSummaryText.maxLines = 3
	// Focusing the description (to read all of it) shows the usual outline instead of a dark box;
	// padded so the text stays put inside the outline
	b.fdSummaryText.background = descriptionFocusBackground(context)
	b.fdSummaryText.setPadding(10.dp(context), 6.dp(context), 10.dp(context), 6.dp(context))
	b.fdButtonRow.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
	val infoHeight = b.fdMainInfoRow.layoutParams.height
	val visibility = listOf(b.fdTitle, b.fdSubtitle, b.fdMainInfoRow, b.fdGenreRow, b.fdSummaryText, b.fdButtonRow)
		.associate { it.id to it.visibility }
	ConstraintSet().apply {
		clone(b.detailsContent)
		for (id in listOf(b.fdTitle.id, b.fdSubtitle.id, b.fdMainInfoRow.id, b.fdGenreRow.id,
			b.fdSummaryText.id, b.fdButtonRow.id, b.mainImageContainer.id, b.fdThumb.id, b.fdTrackColumn.id)) clear(id)
		fun textColumn(id: Int, above: Int, gap: Int, maxWidth: Int = 560) {
			constrainWidth(id, 0)
			constrainMaxWidth(id, maxWidth.dp(context))
			constrainHeight(id, ViewGroup.LayoutParams.WRAP_CONTENT)
			connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 48.dp(context))
			connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 40.dp(context))
			setHorizontalBias(id, 0f)
			connect(id, ConstraintSet.TOP, above, if (above == ConstraintSet.PARENT_ID) ConstraintSet.TOP else ConstraintSet.BOTTOM, gap.dp(context))
		}
		// Narrower beside a logo so the two never overlap
		textColumn(b.fdTitle.id, ConstraintSet.PARENT_ID, 14, maxWidth = if (hasLogo) 560 else 680)
		textColumn(b.fdSubtitle.id, b.fdTitle.id, 4)
		textColumn(b.fdMainInfoRow.id, b.fdSubtitle.id, 8)
		setGoneMargin(b.fdMainInfoRow.id, ConstraintSet.TOP, 8.dp(context))
		constrainHeight(b.fdMainInfoRow.id, infoHeight)
		textColumn(b.fdGenreRow.id, b.fdMainInfoRow.id, 6)
		textColumn(b.fdSummaryText.id, b.fdGenreRow.id, 4)
		// Pulled left by its padding so the text lines up with the title
		connect(b.fdSummaryText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 38.dp(context))
		constrainMaxWidth(b.fdSummaryText.id, 580.dp(context))
		textColumn(b.fdButtonRow.id, b.fdSummaryText.id, 18, maxWidth = 2000)
		connect(b.fdButtonRow.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 16.dp(context))
		setVerticalBias(b.fdButtonRow.id, 0f)
		visibility.forEach { (id, value) -> setVisibility(id, value) }
		// The title's logo sits top-right under the clock, like the Home preview's right third
		constrainWidth(b.mainImageContainer.id, 260.dp(context))
		constrainHeight(b.mainImageContainer.id, 104.dp(context))
		connect(b.mainImageContainer.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 40.dp(context))
		connect(b.mainImageContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 64.dp(context))
		setVisibility(b.mainImageContainer.id, if (hasLogo) View.VISIBLE else View.GONE)
		setVisibility(b.fdThumb.id, View.GONE)
		setVisibility(b.fdTrackColumn.id, View.GONE)
		applyTo(b.detailsContent)
	}
}
