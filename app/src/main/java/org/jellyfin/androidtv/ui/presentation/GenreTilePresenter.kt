package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.koin.compose.koinInject
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.home.GenreTile
import org.jellyfin.design.Tokens

/**
 * A landscape genre card with readable text over its randomly selected artwork.
 */
class GenreTilePresenter : Presenter() {
	private class ComposeViewWrapper(composeView: ComposeView) : FrameLayout(composeView.context) {
		init {
			isFocusable = true
			isFocusableInTouchMode = true
			descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
			addView(composeView)
		}

		// Hack to prevent Compose crash with leanback presenters
		override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
			if (isAttachedToWindow) super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			else setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
		}
	}

	inner class ViewHolder(
		private val composeView: ComposeView,
	) : Presenter.ViewHolder(ComposeViewWrapper(composeView)) {
		private val focused = MutableStateFlow(false)

		init {
			view.onFocusChangeListener = { _, hasFocus -> focused.value = hasFocus }
		}

		fun bind(value: GenreTile) = composeView.setContent {
			val preferences = koinInject<UserPreferences>()
			val cardSize by preferences.mediaCardSizeChanges.collectAsState(initial = preferences[UserPreferences.mediaCardSize])
			val cardWidth = 150.dp * (16f / 9f) * cardSize.scale
			val isFocused by focused.collectAsState()
			val captionHeight = with(LocalDensity.current) { 50.sp.toDp() } + 12.dp

			ItemPreview(card = {
				Box(
					modifier = Modifier
						.width(cardWidth)
						.aspectRatio(16f / 9f)
						.clip(RoundedCornerShape(8.dp))
						.background(Tokens.Color.colorGrey800)
						.graphicsLayer(alpha = if (isFocused) 1f else 0.7f)
				) {
					AsyncImage(url = value.imageUrl, scaleType = ImageView.ScaleType.CENTER_CROP,
						modifier = Modifier.fillMaxSize())
				}

			}, metadataMinHeight = captionHeight, title = {
				Text(value.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
					lineHeight = 18.sp, minLines = 1, maxLines = 2, overflow = TextOverflow.Ellipsis)
			}, subtitle = { Text(" ", fontSize = 11.sp, lineHeight = 14.sp) })
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			isFocusable = true
		}

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
		if (viewHolder !is ViewHolder) return
		if (item is GenreTile) viewHolder.bind(item)
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
	override fun onViewAttachedToWindow(viewHolder: Presenter.ViewHolder) = Unit
}
