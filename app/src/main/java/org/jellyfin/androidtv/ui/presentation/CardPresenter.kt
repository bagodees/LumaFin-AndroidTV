package org.jellyfin.androidtv.ui.presentation

import org.jellyfin.androidtv.preference.UserPreferences
import android.view.View
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.view.ViewGroup
import android.widget.ImageView
import org.jellyfin.androidtv.ui.composable.rememberReducedMotion
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.rememberAccessibilityOptions
import org.jellyfin.androidtv.ui.base.focusOutline
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemCardBaseItemOverlay
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.handleCardLongPress
import org.jellyfin.androidtv.ui.itemhandling.showCardMenuHintOnce
import org.koin.core.context.GlobalContext
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject

class CardPresenter @JvmOverloads constructor(
	val showInfo: Boolean,
	val imageType: ImageType,
	val staticHeight: Int,
	val uniformAspect: Boolean,
	val homeRow: Boolean = false,
	val compactList: Boolean = false,
	/** Height of Home cards in dp, before the card size setting. */
	val homeCardHeight: Int = 150,
	/** Portrait cover art instead of landscape artwork (card style setting, #123). */
	val portrait: Boolean = false,
) : Presenter() {
	private val preferences by lazy { GlobalContext.get().get<UserPreferences>() }
	constructor(showInfo: Boolean, imageType: ImageType, staticHeight: Int) : this(showInfo, imageType, staticHeight, false)
	constructor(showInfo: Boolean, staticHeight: Int) : this(showInfo, ImageType.POSTER, staticHeight)
	constructor(showInfo: Boolean) : this(showInfo, 150)
	constructor() : this(true)

	companion object {
		fun homeRows() = CardPresenter(true, ImageType.THUMB, 150, true, homeRow = true)
		/** Home cards at [height] dp, as portrait covers when [portrait] is set. */
		fun homeRows(height: Int, portrait: Boolean) = CardPresenter(true, ImageType.THUMB, 150, true,
			homeRow = true, homeCardHeight = height, portrait = portrait)
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(view: View) {
					val owner = view.parent as? View ?: return
					setParentCompositionContext(owner.findViewTreeCompositionContext())
					setViewTreeLifecycleOwner(owner.findViewTreeLifecycleOwner())
					setViewTreeSavedStateRegistryOwner(owner.findViewTreeSavedStateRegistryOwner())
				}
				override fun onViewDetachedFromWindow(view: View) = Unit
			})
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true

		}

		return CardViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		if (viewHolder !is CardViewHolder) return
		val rowItem = when {
			item is BaseRowItem -> item
			homeRow && item is GridButton -> GridButtonBaseRowItem(item)
			else -> return
		}
		viewHolder.bind(rowItem)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		if (viewHolder !is CardViewHolder) return

		viewHolder.unbind()
	}

	private inner class CardViewHolder(composeView: ComposeView) : ViewHolder(composeView) {
		private val _item = MutableStateFlow<BaseRowItem?>(null)
		private val _focused = MutableStateFlow(false)
		private var boundAccountRevision = -1

		init {
			composeView.setOnLongClickListener {
				val item = _item.value
				if (item == null || !view.isFocused || preferences.accountRevision != boundAccountRevision) true
				else handleCardLongPress(view, item, preferences[UserPreferences.cardLongPressAction])
			}
			composeView.setContent {
				val item by _item.collectAsState()
				val focused by _focused.collectAsState()

				if (compactList) {
					CompactLibraryRow(item, focused)
				} else CardViewHolderContent(
					item = item,
					focused = focused,
					showInfo = showInfo,
					imageType = imageType,
					staticHeight = staticHeight,
					uniformAspect = uniformAspect,
					homeRow = homeRow,
					homeCardHeight = homeCardHeight,
					portrait = portrait,
				)
			}

			_focused.value = view.isFocused
			composeView.onFocusChangeListener = { _, focused ->
				_focused.value = focused
				if (focused) _item.value?.let { showCardMenuHintOnce(view, it) }
			}
		}

		fun bind(item: BaseRowItem) {
			boundAccountRevision = preferences.accountRevision
			_item.value = item
			_focused.value = view.isFocused
		}

		fun unbind() {
			boundAccountRevision = -1
			_item.value = null
			_focused.value = false
		}
	}
}

private data class BaseRowItemDisplayConfig(
	val image: JellyfinImage?,
	val iconRes: Int,
	val aspectRatio: Float,
	val overrideShowInfo: Boolean? = null,
	val scaleType: ImageView.ScaleType? = null,
)

private fun BaseRowItem.getDisplayConfig(imageType: ImageType, uniformAspect: Boolean): BaseRowItemDisplayConfig = when (baseRowType) {
	BaseRowType.BaseItem -> {
		val preferSeriesPoster = this is BaseItemDtoBaseRowItem && preferSeriesPoster
		val primaryAspectRatio = baseItem?.primaryImageAspectRatio?.toFloat()

		// LumaFin: cards are uniform landscape tiles (Thumb -> Backdrop -> Primary, see
		// BaseItemDtoBaseRowItem.getImage) rather than per-type portrait posters.
		val base = BaseRowItemDisplayConfig(
			aspectRatio = when (imageType) {
				ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
				else -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			},
			image = getImage(imageType),
			iconRes = R.drawable.ic_clapperboard,
		)

		when (baseItem?.type) {
			BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM -> base.copy(
				iconRes = R.drawable.ic_music_album,
				aspectRatio = if (uniformAspect) 1f else primaryAspectRatio ?: 1f,
			)

			BaseItemKind.PERSON,
			BaseItemKind.MUSIC_ARTIST -> base.copy(
				iconRes = R.drawable.ic_user,
				aspectRatio = if (uniformAspect) 1f else primaryAspectRatio ?: 1f,
			)

			BaseItemKind.SEASON, BaseItemKind.SERIES -> base.copy(
				iconRes = R.drawable.ic_tv
			)

			BaseItemKind.EPISODE -> when (preferSeriesPoster) {
				true -> base.copy(
					aspectRatio = ImageHelper.ASPECT_RATIO_2_3.toFloat(),
					iconRes = R.drawable.ic_tv
				)

				false -> base.copy(
					iconRes = R.drawable.ic_tv,
					overrideShowInfo = true,
				)
			}

			BaseItemKind.COLLECTION_FOLDER, BaseItemKind.USER_VIEW -> base.copy(
				iconRes = R.drawable.ic_folder,
			)

			BaseItemKind.FOLDER, BaseItemKind.GENRE, BaseItemKind.MUSIC_GENRE -> base.copy(
				iconRes = R.drawable.ic_folder,
			)

			BaseItemKind.PHOTO -> base.copy(
				iconRes = R.drawable.ic_photo
			)

			BaseItemKind.PHOTO_ALBUM, BaseItemKind.PLAYLIST -> base.copy(
				iconRes = R.drawable.ic_folder
			)

			else -> base
		}
	}

	BaseRowType.LiveTvChannel -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: 1f
		},
		image = getImage(imageType),
		scaleType = ImageView.ScaleType.FIT_CENTER,
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.LiveTvProgram -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		},
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
		overrideShowInfo = true,
	)

	BaseRowType.LiveTvRecording -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		},
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.SeriesTimer -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		iconRes = R.drawable.ic_tv_timer,
		image = getImage(imageType),
		overrideShowInfo = true,
	)

	BaseRowType.Person -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_user,
	)

	BaseRowType.Chapter -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)

	BaseRowType.GridButton -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)
}

@Composable
@Stable
private fun CardViewHolderContent(
	item: BaseRowItem?,
	focused: Boolean,
	showInfo: Boolean,
	imageType: ImageType,
	staticHeight: Int,
	uniformAspect: Boolean,
	homeRow: Boolean,
	homeCardHeight: Int = 150,
	portrait: Boolean = false,
) {
	val context = LocalContext.current
	val localDensity = LocalDensity.current

	val title = remember(item, context) { item?.getCardName(context) }
	val subtitle = remember(item, context) { item?.getSubText(context) }
	val preferences = koinInject<UserPreferences>()
	val hideArtwork by preferences.episodeArtworkChanges.collectAsState(initial = preferences[UserPreferences.hideUnwatchedEpisodeArtwork])
	val homeTitlesHidden by preferences.homeCardTitlesHiddenChanges.collectAsState(
		initial = preferences[UserPreferences.newHomeEnabled] && !preferences[UserPreferences.newHomeCardTitles]
	)
	// Portrait cards use cover art; landscape cards use wide artwork even where a library's image type is Poster
	val artType = when {
		portrait -> ImageType.POSTER
		imageType == ImageType.POSTER -> ImageType.THUMB
		else -> imageType
	}
	val displayConfig = remember(item, artType, uniformAspect, hideArtwork) { item?.getDisplayConfig(artType, uniformAspect) }
	if (item == null || displayConfig == null) return

	val image = displayConfig.image
	val aspectRatio = when {
		homeRow && portrait -> ImageHelper.ASPECT_RATIO_2_3.toFloat()
		homeRow -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
		// Square music covers and banners keep their own shape
		portrait && displayConfig.aspectRatio == ImageHelper.ASPECT_RATIO_16_9.toFloat() -> ImageHelper.ASPECT_RATIO_2_3.toFloat()
		else -> displayConfig.aspectRatio.takeIf { it >= 0.1f } ?: image?.aspectRatio?.takeIf { it >= 0.1f } ?: 1f
	}

	val cardSize by preferences.mediaCardSizeChanges.collectAsState(initial = preferences[UserPreferences.mediaCardSize])
	val mediaTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.SEASON, BaseItemKind.EPISODE,
		BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM, BaseItemKind.MUSIC_VIDEO, BaseItemKind.BOOK, BaseItemKind.AUDIO_BOOK,
		BaseItemKind.USER_VIEW, BaseItemKind.COLLECTION_FOLDER)
	val scale = if (homeRow || item.baseItem?.type in mediaTypes) cardSize.scale else 1f
	val baseSize = if (homeRow) DpSize(homeCardHeight.dp * aspectRatio, homeCardHeight.dp) else when (item.staticHeight) {
		true -> DpSize(staticHeight.dp * aspectRatio, staticHeight.dp)
		false if (aspectRatio > 1f) -> DpSize(130.dp * aspectRatio, 130.dp)
		else -> DpSize(150.dp * aspectRatio, 150.dp)
	}

	val size = DpSize(baseSize.width * scale, baseSize.height * scale)

	val usePreview = displayConfig.overrideShowInfo ?: showInfo
	val hideLibraryCaption = homeRow && item.baseItem?.type in setOf(BaseItemKind.USER_VIEW, BaseItemKind.COLLECTION_FOLDER)
	// The focused card is the visual anchor for a TV row. Gently dim the artwork on
	// its neighbours so focus remains clear without relying on an oversized zoom.
	val reducedMotion = rememberReducedMotion()
	val accessibility = rememberAccessibilityOptions()
	val cardAlpha by animateFloatAsState(
		targetValue = if (focused) 1f else 0.7f,
		animationSpec = if (reducedMotion) snap() else spring(),
		label = "CardFocusAlpha",
	)

	val card = @Composable {
		ItemCard(
			image = {
				// A wide card for a title that only has a tall poster: cropping it zooms into a face or cuts
				// the title art, so show the whole poster over a dimmed, blurred copy that fills the sides
				val posterInWideCard = image != null && aspectRatio > 1f &&
					image.type == org.jellyfin.sdk.model.api.ImageType.PRIMARY && (image.aspectRatio ?: 1f) < 1f
				if (image != null && posterInWideCard) {
					val api = koinInject<ApiClient>()
					val url = image.getUrl(
						api,
						maxHeight = with(localDensity) { size.height.roundToPx() },
					)
					AsyncImage(
						url = url,
						blurHash = image.blurHash,
						aspectRatio = aspectRatio,
						scaleType = ImageView.ScaleType.CENTER_CROP,
						modifier = Modifier
							.fillMaxSize()
							.blur(24.dp)
							.graphicsLayer(alpha = 0.45f)
					)
					AsyncImage(
						url = url,
						blurHash = image.blurHash,
						aspectRatio = image.aspectRatio ?: (2f / 3f),
						scaleType = ImageView.ScaleType.FIT_CENTER,
						modifier = Modifier
							.fillMaxSize()
					)
				} else if (image != null) {
					val api = koinInject<ApiClient>()
					AsyncImage(
						url = image.getUrl(
							api,
							maxWidth = with(localDensity) { size.width.roundToPx() },
							maxHeight = with(localDensity) { size.height.roundToPx() },
						),
						blurHash = image.blurHash,
						aspectRatio = aspectRatio,
						scaleType = displayConfig.scaleType ?: ImageView.ScaleType.CENTER_CROP,
						modifier = Modifier
							.fillMaxSize()
					)
				} else if (item is GridButtonBaseRowItem && item.gridButton.imageRes != null) {
					Image(
						painter = painterResource(item.gridButton.imageRes),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize()
					)
				} else {
					Image(
						painter = painterResource(displayConfig.iconRes),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize(0.4f)
							.align(Alignment.Center)
					)
				}
			},
			overlay = {
				val showInfo = !usePreview && item.showCardInfoOverlay
				item.baseItem?.let { baseItem ->
					ItemCardBaseItemOverlay(
						item = baseItem,
						subtleWatched = !homeRow,
						footer = {
							if (showInfo && title != null) {
								val focusModifier = if (focused) Modifier.basicMarquee(
									iterations = Int.MAX_VALUE,
									initialDelayMillis = 0,
								) else Modifier

								Box(
									modifier = Modifier
										.fillMaxWidth()
										.background(JellyfinTheme.colorScheme.surface.copy(alpha = if (accessibility.highContrast) 0.95f else 0.8f), JellyfinTheme.shapes.extraSmall),
								) {
									Text(
										text = title,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										textAlign = TextAlign.Center,
										color = Tokens.Color.colorWhite,
										modifier = Modifier
											.then(focusModifier)
											.padding(Tokens.Space.spaceXs),
									)
								}
							}
						}
					)
				}
			},
			modifier = Modifier
				.size(size)
				.then(if (focused && !accessibility.strongFocus) Modifier.border(2.dp, JellyfinTheme.colorScheme.inputFocused, JellyfinTheme.shapes.medium) else Modifier)
				.focusOutline(focused, JellyfinTheme.shapes.medium, accessibility.strongFocus)
				.then(if (hideLibraryCaption) Modifier.semantics { contentDescription = title.orEmpty() } else Modifier)
				.graphicsLayer(alpha = cardAlpha)
		)
	}

	if (hideLibraryCaption) {
		ItemPreview(
			card = { card() },
			metadataMinHeight = with(localDensity) { 50.sp.toDp() } + 12.dp,
		)
	} else if (usePreview && !(homeRow && homeTitlesHidden)) {
		val focusModifier = if (focused) Modifier.basicMarquee(
			iterations = Int.MAX_VALUE,
			initialDelayMillis = 0,
		) else Modifier

		ItemPreview(
			card = { card() },
			metadataMinHeight = if (homeRow) with(localDensity) { 50.sp.toDp() } + 2.dp else 0.dp,
			title = title?.let { text ->
				{
					Text(
						text = text,
						fontSize = if (homeRow) 15.sp else TextUnit.Unspecified,
						lineHeight = if (homeRow) 18.sp else TextUnit.Unspecified,
						fontWeight = if (homeRow) FontWeight.SemiBold else null,
						color = if (homeRow) Color.White else Color.Unspecified,
						minLines = 1,
						maxLines = if (homeRow) 2 else 1,
						overflow = TextOverflow.Ellipsis,
						textAlign = if (homeRow) TextAlign.Start else TextAlign.Center,
						modifier = if (homeRow) Modifier.fillMaxWidth() else Modifier.then(focusModifier),
					)
				}
			},
			subtitle = (subtitle ?: if (homeRow) " " else null)?.let { text ->
				{
					Text(
						text = text,
						fontSize = if (homeRow) 11.sp else TextUnit.Unspecified,
						lineHeight = if (homeRow) 14.sp else TextUnit.Unspecified,
						color = if (homeRow) Color.White.copy(alpha = 0.85f) else Color.Unspecified,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						textAlign = TextAlign.Center,
						modifier = Modifier.then(focusModifier),
					)
				}
			},
		)
	} else {
		card()
	}
}
