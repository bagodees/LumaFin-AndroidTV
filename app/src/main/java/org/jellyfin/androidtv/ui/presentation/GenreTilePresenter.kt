package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.home.GenreTile
import org.jellyfin.design.Tokens

/**
 * Renders a [GenreTile] as a 2x2 collage of the genre's top items with a caption below,
 * matching the LumaFin-Tizen genre tile design.
 */
class GenreTilePresenter @JvmOverloads constructor(
	private val width: Int = 210,
) : Presenter() {
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
			composeView.onFocusChangeListener = { _, hasFocus -> focused.value = hasFocus }
		}

		fun bind(value: GenreTile) = composeView.setContent {
			val isFocused by focused.collectAsState()
			val borderModifier = if (isFocused) {
				Modifier.border(3.dp, Tokens.Color.colorRed500, RoundedCornerShape(8.dp))
			} else {
				Modifier
			}

			Column(modifier = Modifier.width(width.dp)) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.aspectRatio(1f)
						.clip(RoundedCornerShape(8.dp))
						.background(Tokens.Color.colorGrey800)
						.then(borderModifier)
				) {
					Column(modifier = Modifier.fillMaxSize()) {
						Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
							GenreTileCell(value.imageUrls.getOrNull(0), Modifier.weight(1f).fillMaxSize())
							GenreTileCell(value.imageUrls.getOrNull(1), Modifier.weight(1f).fillMaxSize())
						}
						Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
							GenreTileCell(value.imageUrls.getOrNull(2), Modifier.weight(1f).fillMaxSize())
							GenreTileCell(value.imageUrls.getOrNull(3), Modifier.weight(1f).fillMaxSize())
						}
					}
				}

				Text(
					text = value.name,
					modifier = Modifier.padding(top = 8.dp)
				)
			}
		}
	}

	@Composable
	private fun GenreTileCell(url: String?, modifier: Modifier) {
		if (url != null) {
			AsyncImage(
				modifier = modifier,
				url = url,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
		} else {
			Box(modifier = modifier.background(Tokens.Color.colorGrey700))
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
