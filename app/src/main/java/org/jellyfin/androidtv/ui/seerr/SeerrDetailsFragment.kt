package org.jellyfin.androidtv.ui.seerr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.ui.base.rememberAccessibilityOptions
import org.jellyfin.androidtv.ui.browsing.composable.inforow.BaseItemInfoRow
import org.jellyfin.androidtv.ui.home.PreviewLogo
import org.jellyfin.androidtv.ui.home.SeerrCardPresenter
import org.jellyfin.androidtv.ui.home.SeerrFin
import org.jellyfin.androidtv.ui.home.SeerrItem
import org.jellyfin.androidtv.ui.home.SideMenuFrame
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import org.jellyfin.androidtv.util.sdk.TrailerUtils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.personsApi
import org.jellyfin.sdk.model.api.request.GetPersonsRequest
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * A title from Seerr that may not be in the library yet (#100), laid out like the movie and show
 * details pages: backdrop, logo, info row, description and pill buttons, then Cast and More Like This.
 * Requesting happens here; it always asks first, opening on Cancel.
 */
class SeerrDetailsFragment : Fragment() {
	companion object {
		const val EXTRA_MEDIA_TYPE = "MediaType"
		const val EXTRA_MEDIA_ID = "MediaId"
	}

	private val api by inject<ApiClient>()
	private val backgroundService by inject<BackgroundService>()
	private val navigationRepository by inject<NavigationRepository>()
	private val details = MutableStateFlow<SeerrDetails?>(null)
	private val rows = ArrayObjectAdapter(ClassPresenterSelector().apply {
		addClassPresenter(HeaderRow::class.java, HeaderPresenter())
		addClassPresenter(ListRow::class.java, CustomListRowPresenter(null, true))
	})
	private var rowsLoaded = false

	private val mediaType get() = requireArguments().getString(EXTRA_MEDIA_TYPE) ?: "movie"
	private val mediaId get() = requireArguments().getInt(EXTRA_MEDIA_ID)

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val context = requireContext()
		val root = FrameLayout(context).apply { setBackgroundResource(R.drawable.details_readability_gradient) }
		// A fixed id, so the rows fragment finds its container again when coming back to this page
		val rowsContainer = FragmentContainerView(context).apply { id = R.id.rowsFragment }
		root.addView(rowsContainer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
		// Same spot as every other screen's clock
		root.addView(ComposeView(context).apply { setContent { ToolbarClock() } }, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.END or android.view.Gravity.TOP,
		).apply {
			val density = resources.displayMetrics.density
			topMargin = (20 * density).toInt(); marginEnd = (32 * density).toInt()
		})

		val rowsFragment = childFragmentManager.findFragmentById(rowsContainer.id) as? RowsSupportFragment ?: RowsSupportFragment().also {
			childFragmentManager.beginTransaction().replace(rowsContainer.id, it).commitNow()
		}
		rowsFragment.adapter = rows
		rowsFragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
			when (item) {
				is SeerrItem -> navigationRepository.navigate(Destinations.seerrDetails(item.mediaType, item.mediaId))
				is SeerrPerson -> openPerson(item)
			}
		}
		return SideMenuFrame.wrap(context, root, MainToolbarActiveButton.Home)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (rows.size() == 0) rows.add(HeaderRow)
		reload()
	}

	override fun onResume() {
		super.onResume()
		details.value?.backdropUrl?.let(backgroundService::setBackgroundUrl)
	}

	private fun reload() = viewLifecycleOwner.lifecycleScope.launch {
		val loaded = loadSeerrDetails(api, mediaType, mediaId)
		if (loaded == null) {
			if (details.value == null) {
				Toast.makeText(requireContext(), R.string.seerr_details_failed, Toast.LENGTH_LONG).show()
				navigationRepository.goBack()
			}
			return@launch
		}
		details.value = loaded
		backgroundService.setBackgroundUrl(loaded.backdropUrl)
		// The description and logo the Home preview uses
		SeerrFin.enrich(api, loaded.item)
		if (!rowsLoaded) {
			rowsLoaded = true
			addRows(loaded)
		}
	}

	private suspend fun addRows(loaded: SeerrDetails) {
		val context = requireContext()
		if (loaded.cast.isNotEmpty()) {
			val adapter = ArrayObjectAdapter(SeerrPersonCardPresenter()).apply { addAll(0, loaded.cast) }
			rows.add(ListRow(HeaderItem(getString(R.string.lbl_cast)), adapter))
		}
		val similar = loadSeerrRecommendations(api, mediaType, mediaId)
		if (similar.isEmpty() || !isAdded) return
		// The titled backdrop, like the cards on Home
		val cards = coroutineScope {
			similar.map { item -> async { SeerrFin.titledBackdrop(api, item)?.let { item.copy(imageUrl = it) } ?: item } }.map { it.await() }
		}
		val adapter = ArrayObjectAdapter(SeerrCardPresenter()).apply { addAll(0, cards) }
		rows.add(ListRow(HeaderItem(context.getString(R.string.lbl_more_like_this)), adapter))
	}

	/** Opens the person's page when they're in the library; Seerr's cast list isn't tied to it. */
	private fun openPerson(person: SeerrPerson) = viewLifecycleOwner.lifecycleScope.launch {
		val match = try {
			withContext(Dispatchers.IO) {
				api.personsApi.getPersons(GetPersonsRequest(searchTerm = person.name, limit = 5)).content.items
			}.firstOrNull { it.name.equals(person.name, ignoreCase = true) }
		} catch (error: CancellationException) {
			throw error
		} catch (error: Exception) {
			Timber.w(error, "Person lookup failed")
			null
		}
		if (match != null) navigationRepository.navigate(Destinations.itemDetails(match.id))
		else Toast.makeText(requireContext(), getString(R.string.seerr_person_not_in_library, person.name), Toast.LENGTH_SHORT).show()
	}

	private fun request(current: SeerrDetails, seasonsOnly: Boolean) {
		val context = requireContext()
		val item = current.item
		val tv = item.mediaType == "tv"
		val dialog = LumaFinMenuDialog(context, viewLifecycleOwner, item.name,
			listOfNotNull(item.year?.toString(), getString(if (tv) R.string.seerr_kind_show else R.string.seerr_kind_movie)).joinToString(" · "),
			fitContent = true)
		val chosen = current.requestableSeasons.map { it.number }.toMutableSet()
		if (tv) {
			dialog.body(getString(if (seasonsOnly) R.string.seerr_choose_missing_seasons else R.string.seerr_choose_seasons))
			for (season in current.requestableSeasons) {
				val label = listOfNotNull(
					getString(R.string.seerr_season, season.number),
					season.episodes?.let { resources.getQuantityString(R.plurals.episodes, it, it.toString()) },
				).joinToString(" · ")
				dialog.toggle(label, true) { on -> if (on) chosen += season.number else chosen -= season.number }
			}
		} else {
			dialog.body(getString(R.string.seerr_request_movie_detail))
		}
		dialog.button(getString(R.string.seerr_request), primary = true) {
			if (tv && chosen.isEmpty()) return@button
			dialog.dismiss()
			viewLifecycleOwner.lifecycleScope.launch {
				val ok = SeerrFin.request(api, item, if (tv) chosen.sorted() else emptyList())
				Toast.makeText(context, getString(if (ok) R.string.seerr_requested else R.string.seerr_request_failed, item.name), Toast.LENGTH_LONG).show()
				if (ok) reload()
			}
		}
		val cancel = dialog.button(getString(android.R.string.cancel)) { dialog.dismiss() }
		dialog.show()
		// Opens on Cancel, so a stray OK never sends a request
		cancel.requestFocus()
	}

	private object HeaderRow : Row()

	/** The details header as a row, so the cast and title rows below scroll and take focus like the other details pages. */
	private inner class HeaderPresenter : RowPresenter() {
		init {
			headerPresenter = null
			selectEffectEnabled = false
		}

		override fun isUsingDefaultSelectEffect() = false

		override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
			val view = ComposeView(parent.context).apply {
				setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
				setParentCompositionContext(parent.findViewTreeCompositionContext())
				setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
				setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
				layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
				setContent {
					val current by details.collectAsState()
					current?.let { SeerrHeader(it) }
				}
			}
			return ViewHolder(view)
		}
	}

	@Composable
	private fun SeerrHeader(current: SeerrDetails) {
		val preview = remember(current) { current.previewItem() }
		// Beside a logo the text column stops short of it, so the two never overlap
		val logos by SeerrFin.logos.collectAsState()
		val textWidth = if (preview.id in logos) 520.dp else 580.dp
		val firstButton = remember { FocusRequester() }
		LaunchedEffect(current.status) { runCatching { firstButton.requestFocus() } }
		Box(Modifier.fillMaxWidth().padding(start = 48.dp, top = 14.dp, end = 40.dp, bottom = 16.dp)) {
			// The logo sits top-right under the clock, like the other details pages
			PreviewLogo(preview, Modifier.align(Alignment.TopEnd).padding(top = 50.dp).width(260.dp).height(104.dp))
			Column {
				Text(
					current.item.name,
					fontSize = 32.sp,
					fontWeight = FontWeight.Medium,
					color = LumaFinPalette.Text,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.widthIn(max = textWidth),
				)
				Spacer(Modifier.height(8.dp))
				Box(Modifier.height(IntrinsicSize.Min)) {
					BaseItemInfoRow(item = preview, mediaSource = null, includeRuntime = true)
				}
				val genreLine = listOfNotNull(
					current.genres.joinToString(" / ").ifEmpty { null },
					current.credits?.let { stringResource(if (current.item.mediaType == "tv") R.string.seerr_created_by else R.string.seerr_directed_by, it) },
				).joinToString("\n")
				if (genreLine.isNotEmpty()) Text(
					genreLine,
					fontSize = 13.sp,
					lineHeight = 18.sp,
					color = LumaFinPalette.SecondaryText,
					modifier = Modifier.padding(top = 6.dp).widthIn(max = 560.dp),
				)
				current.overview?.let {
					Text(
						it,
						fontSize = 15.sp,
						lineHeight = 21.sp,
						color = LumaFinPalette.Text,
						maxLines = 3,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(top = 10.dp).widthIn(max = textWidth),
					)
				}
				Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 18.dp)) {
					val buttons = mutableListOf<@Composable (Modifier) -> Unit>()
					current.jellyfinItemId?.let { id ->
						buttons += { m -> SeerrPill(R.drawable.ic_play, stringResource(R.string.seerr_open_in_library), m) { navigationRepository.navigate(Destinations.itemDetails(id)) } }
					}
					when {
						current.requested -> buttons += { m -> SeerrPill(R.drawable.ic_check, stringResource(R.string.seerr_status_requested_short), m) {
							Toast.makeText(requireContext(), R.string.seerr_status_requested, Toast.LENGTH_LONG).show()
						} }
						current.status == 4 && current.requestableSeasons.isNotEmpty() ->
							buttons += { m -> SeerrPill(R.drawable.ic_add, stringResource(R.string.seerr_request_missing_seasons), m) { request(current, seasonsOnly = true) } }
						!current.inLibrary && current.status != 4 && (current.item.mediaType != "tv" || current.requestableSeasons.isNotEmpty()) ->
							buttons += { m -> SeerrPill(R.drawable.ic_add, stringResource(R.string.seerr_request), m) { request(current, seasonsOnly = false) } }
					}
					current.trailerUrl?.let { url ->
						buttons += { m -> SeerrPill(R.drawable.ic_trailer, stringResource(R.string.seerr_trailer), m) {
							runCatching { startActivity(TrailerUtils.getExternalTrailerIntent(url)) }
								.onFailure { Toast.makeText(requireContext(), R.string.seerr_trailer_failed, Toast.LENGTH_LONG).show() }
						} }
					}
					buttons.forEachIndexed { index, button -> button(if (index == 0) Modifier.focusRequester(firstButton) else Modifier) }
				}
			}
		}
	}

	/** A pill button like the ones on the other details pages: icon and label, cyan outline when focused. */
	@Composable
	private fun SeerrPill(icon: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
		val interaction = remember { MutableInteractionSource() }
		val focused by interaction.collectIsFocusedAsState()
		val strongFocus = rememberAccessibilityOptions().strongFocus
		val shape = RoundedCornerShape(12.dp)
		ButtonBase(
			onClick = onClick,
			shape = shape,
			interactionSource = interaction,
			colors = ButtonDefaults.colors(
				containerColor = LumaFinPalette.Raised,
				contentColor = LumaFinPalette.Text,
				focusedContainerColor = LumaFinPalette.FocusedRow,
				focusedContentColor = LumaFinPalette.Text,
			),
			modifier = modifier
				.heightIn(min = 36.dp)
				.then(if (focused && !strongFocus) Modifier.border(2.dp, LumaFinPalette.Cyan, shape) else Modifier),
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
			) {
				Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
				Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
			}
		}
	}
}
