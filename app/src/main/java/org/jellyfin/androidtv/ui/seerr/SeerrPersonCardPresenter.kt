package org.jellyfin.androidtv.ui.seerr

import io.github.bagodees.lumafin.core.seerr.SeerrPerson
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.focusOutline
import org.jellyfin.androidtv.ui.base.rememberAccessibilityOptions
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemPreview

/** Cast cards for the Seerr title page, sized and styled like the cast cards on the other details pages. */
class SeerrPersonCardPresenter : Presenter() {
	private class PersonViewHolder(view: ComposeView) : ViewHolder(view) {
		val person = MutableStateFlow<SeerrPerson?>(null)
		val focused = MutableStateFlow(false)
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true
		}
		val holder = PersonViewHolder(view)
		// Set once, before the row wraps it for the focus zoom
		view.onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus -> holder.focused.value = hasFocus }
		view.setContent {
			val person by holder.person.collectAsState()
			val isFocused by holder.focused.collectAsState()
			val current = person ?: return@setContent
			val accessibility = rememberAccessibilityOptions()
			val shape = JellyfinTheme.shapes.medium
			ItemPreview(
				card = {
					ItemCard(
						image = {
							if (current.imageUrl != null) AsyncImage(
								url = current.imageUrl,
								aspectRatio = 2f / 3f,
								scaleType = ImageView.ScaleType.CENTER_CROP,
								modifier = Modifier.fillMaxSize(),
							) else Image(
								painter = painterResource(R.drawable.ic_user),
								contentDescription = null,
								modifier = Modifier.size(40.dp).align(Alignment.Center),
							)
						},
						modifier = Modifier
							.size(DpSize(100.dp, 130.dp))
							.then(if (isFocused && !accessibility.strongFocus) Modifier.border(2.dp, JellyfinTheme.colorScheme.inputFocused, shape) else Modifier)
							.focusOutline(isFocused, shape, accessibility.strongFocus)
							.graphicsLayer(alpha = if (isFocused) 1f else 0.7f),
					)
				},
				title = { Text(current.name, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
				subtitle = current.role?.let { role -> { Text(role, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) } },
			)
		}
		return holder
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as? PersonViewHolder ?: return
		holder.person.value = item as? SeerrPerson
		holder.focused.value = holder.view.isFocused
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as? PersonViewHolder)?.person?.value = null
	}
}
