package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage as CoilImage
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.getUrl
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Row
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.R
import android.widget.Toast
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.preference.HomeShortcutPreferences
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.koin.core.context.GlobalContext
import org.jellyfin.androidtv.constant.Extras
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.home.HomeFocusedPreview
import org.jellyfin.androidtv.ui.home.SideMenuFrame
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.studiosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/** Tabbed library pages for Movies and TV in the LumaFin layout (#92). */
object LibraryTabs {
	/** Height of the tab row, which the embedded grid gives up. */
	const val TAB_BAR_HEIGHT_DP = 64

	@JvmStatic
	fun supports(folder: BaseItemDto?) = SideMenuFrame.isEnabled() &&
		folder?.collectionType in setOf(CollectionType.MOVIES, CollectionType.TVSHOWS)

	/** Limits a library grid request to one genre (by name) or studio (by id). */
	@JvmStatic
	fun filtered(request: GetItemsRequest, genre: String?, studioId: String?): GetItemsRequest = request.copy(
		genres = genre?.let(::setOf) ?: request.genres,
		studioIds = studioId?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let(::setOf) ?: request.studioIds,
	)

	internal fun folderArgs(folder: BaseItemDto) = bundleOf(Extras.Folder to Json.encodeToString(BaseItemDto.serializer(), folder))
}

private enum class LibraryTab(val label: Int) {
	Recommended(R.string.library_tab_recommended),
	Library(R.string.library_tab_library),
	Collections(R.string.lbl_collections),
	Genres(R.string.lbl_genres),
	Studios(R.string.library_tab_studios),
}

private fun Fragment.folderArgument(): BaseItemDto =
	Json.decodeFromString(BaseItemDto.serializer(), requireNotNull(requireArguments().getString(Extras.Folder)))

/**
 * A Movies or TV library: the side menu, a tab row on the clock line, and the picked tab's content below.
 * Moving along the tabs switches after a short pause, so passing over one doesn't load it.
 */
class LibraryTabsFragment : Fragment() {
	private val folder by lazy { folderArgument() }
	private val tabs by lazy {
		if (folder.collectionType == CollectionType.TVSHOWS) listOf(LibraryTab.Recommended, LibraryTab.Library, LibraryTab.Genres, LibraryTab.Studios)
		else listOf(LibraryTab.Recommended, LibraryTab.Library, LibraryTab.Collections, LibraryTab.Genres)
	}
	private val selected = MutableStateFlow(LibraryTab.Recommended)
	private var content: FrameLayout? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		savedInstanceState?.getString(STATE_TAB)?.let { name -> LibraryTab.entries.firstOrNull { it.name == name } }?.let { selected.value = it }
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(STATE_TAB, selected.value.name)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val context = requireContext()
		val tabRow = ComposeView(context).apply {
			setContent {
				val current by selected.collectAsState()
				LibraryTabRow(tabs, current, onSelect = ::show,
					// While on the tabs, the content can't take focus (a grid grabs it when it loads); Down lets it
					onTabsFocused = { inTabs -> content?.descendantFocusability = if (inTabs) ViewGroup.FOCUS_BLOCK_DESCENDANTS else ViewGroup.FOCUS_AFTER_DESCENDANTS },
					onDown = {
						content?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
						// A grid's tools sit to the right, nearer the tabs; go to the grid itself
						(childFragmentManager.findFragmentById(R.id.library_tab_content) as? BrowseGridFragment)?.focusGrid() == true
					})
			}
		}
		val content = FrameLayout(context).apply { id = R.id.library_tab_content }
		this.content = content
		val root = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			// Same dimming over the backdrop as Home, behind the tabs and every tab's content
			setBackgroundResource(R.drawable.details_readability_gradient)
			addView(tabRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
			addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
		}
		return SideMenuFrame.wrap(context, root, MainToolbarActiveButton.Libraries, SideMenuFrame.libraryOf(folder))
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (childFragmentManager.findFragmentById(R.id.library_tab_content) == null) replaceContent(selected.value)
	}

	private fun show(tab: LibraryTab) {
		if (tab == selected.value) return
		selected.value = tab
		replaceContent(tab)
	}

	private fun replaceContent(tab: LibraryTab) {
		val fragment = when (tab) {
			LibraryTab.Recommended -> LibraryRecommendedFragment()
			LibraryTab.Library -> BrowseGridFragment()
			LibraryTab.Collections -> BrowseGridFragment()
			LibraryTab.Genres, LibraryTab.Studios -> LibraryNameTilesFragment()
		}
		fragment.arguments = LibraryTabs.folderArgs(folder).apply {
			putBoolean(Extras.Embedded, true)
			if (tab == LibraryTab.Collections) putString(Extras.IncludeType, "BoxSet")
			if (tab == LibraryTab.Studios) putBoolean(LibraryNameTilesFragment.ARG_STUDIOS, true)
		}
		childFragmentManager.beginTransaction().replace(R.id.library_tab_content, fragment).commit()
	}

	private companion object {
		const val STATE_TAB = "library_tab"
	}
}

@Composable
private fun LibraryTabRow(
	tabs: List<LibraryTab>,
	selected: LibraryTab,
	onSelect: (LibraryTab) -> Unit,
	onTabsFocused: (Boolean) -> Unit,
	// Returns whether it moved focus itself
	onDown: () -> Boolean,
) {
	var focused by remember { mutableStateOf<LibraryTab?>(null) }
	// Switch when focus rests on a tab, not while passing over it
	LaunchedEffect(focused) {
		val tab = focused ?: return@LaunchedEffect
		delay(350)
		onSelect(tab)
	}
	val selectedRequester = remember { FocusRequester() }
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.fillMaxWidth().height(LibraryTabs.TAB_BAR_HEIGHT_DP.dp).padding(start = 40.dp, end = 32.dp, top = 8.dp),
	) {
		// Coming back up from the content lands on the open tab
		Row(
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			modifier = Modifier
				.onFocusChanged { onTabsFocused(it.hasFocus) }
				.onPreviewKeyEvent { event ->
					event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onDown()
				}
				.focusRestorer(selectedRequester)
				.focusGroup(),
		) {
			for (tab in tabs) {
				LibraryTabButton(
					label = stringResource(tab.label),
					selected = tab == selected,
					modifier = if (tab == selected) Modifier.focusRequester(selectedRequester) else Modifier,
					onFocused = { focused = tab },
					onClick = { onSelect(tab) },
				)
			}
		}
		Spacer(Modifier.weight(1f))
		// Same spot as Home's clock: 20dp from the top, counting the row's own top padding
		ToolbarClock(modifier = Modifier.align(Alignment.Top).padding(top = 12.dp))
	}
}

@Composable
private fun LibraryTabButton(label: String, selected: Boolean, modifier: Modifier, onFocused: () -> Unit, onClick: () -> Unit) {
	val strongFocus = koinInject<UserPreferences>()[UserPreferences.strongFocus]
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val shape = RoundedCornerShape(20.dp)
	ButtonBase(
		onClick = onClick,
		shape = shape,
		interactionSource = interactionSource,
		colors = ButtonDefaults.colors(
			containerColor = Color.Transparent,
			contentColor = if (selected) LumaFinPalette.Text else LumaFinPalette.SecondaryText,
			focusedContainerColor = LumaFinPalette.FocusedRow,
			focusedContentColor = LumaFinPalette.Text,
		),
		modifier = modifier
			.height(40.dp)
			.onFocusChanged { if (it.isFocused) onFocused() }
			.then(if (isFocused) Modifier.border(if (strongFocus) 3.dp else 2.dp, if (strongFocus) Color.White else LumaFinPalette.Cyan, shape) else Modifier),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
		) {
			Text(label, fontSize = 17.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
			// The open tab keeps an underline when focus is elsewhere
			Box(
				Modifier.padding(top = 3.dp).width(28.dp).height(2.dp)
					.background(if (selected && !isFocused) LumaFinPalette.Cyan else Color.Transparent, RoundedCornerShape(1.dp))
			)
		}
	}
}

/** The Recommended tab: the Home preview over rows for this library only. */
class LibraryRecommendedFragment : Fragment() {
	private val folder by lazy { folderArgument() }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val context = requireContext()
		// Filled in once the rows exist (onViewCreated), since it shows their focused card
		val preview = ComposeView(context)
		val rows = FrameLayout(context).apply { id = R.id.library_recommended_rows }
		return LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
			addView(rows, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (childFragmentManager.findFragmentById(R.id.library_recommended_rows) == null) {
			childFragmentManager.beginTransaction()
				.replace(R.id.library_recommended_rows, LibraryRowsFragment().apply { arguments = LibraryTabs.folderArgs(folder) })
				.commitNow()
		}
		val rows = childFragmentManager.findFragmentById(R.id.library_recommended_rows) as LibraryRowsFragment
		((view as ViewGroup).getChildAt(0) as ComposeView).let { preview ->
			preview.setContent {
				val item by rows.focusedItem.collectAsState()
				HomeFocusedPreview(item, underTabs = true)
			}
		}
	}
}

/** Rows for one library, in the order agreed for #92. Empty rows remove themselves. */
class LibraryRowsFragment : RowsSupportFragment() {
	private val folder by lazy { folderArgument() }
	private val itemLauncher by inject<ItemLauncher>()
	private val backgroundService by inject<BackgroundService>()
	private val userPreferences by inject<UserPreferences>()
	private val api by inject<ApiClient>()
	val focusedItem = MutableStateFlow<BaseItemDto?>(null)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val rows = MutableObjectAdapter<Row>(PositionableListRowPresenter())
		adapter = rows
		val context = requireContext()
		val presenter = CardPresenter.homeRows()
		val id = folder.id
		val now = LocalDateTime.now()
		fun add(title: Int, adapter: ItemRowAdapter) {
			val row = ListRow(HeaderItem(getString(title)), adapter)
			adapter.setRow(row)
			rows.add(row)
			adapter.Retrieve()
		}
		fun newest(type: BaseItemKind) = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = id,
			includeItemTypes = setOf(type),
			recursive = true,
			sortBy = setOf(ItemSortBy.PREMIERE_DATE),
			sortOrder = setOf(SortOrder.DESCENDING),
			maxPremiereDate = now,
			imageTypeLimit = 1,
			limit = 30,
		)
		// Ranked in the app (see TopRated.kt), then placed before Favorites, or last if Favorites is empty
		// The rows live as long as the fragment, so this loads once, not each time the view is rebuilt
		fun topRated(type: BaseItemKind) = lifecycleScope.launch {
			val items = runCatching { loadTopRated(api, id, type, 30) }
				.onFailure { if (it is CancellationException) throw it; Timber.w(it, "Top Rated didn't load") }
				.getOrDefault(emptyList())
			if (items.isEmpty() || !isAdded) return@launch
			val adapter = ItemRowAdapter(context, items, presenter, rows, true)
			val row = ListRow(HeaderItem(getString(R.string.library_row_top_rated)), adapter)
			adapter.setRow(row)
			val favorites = (0 until rows.size()).firstOrNull { (rows.get(it) as? ListRow)?.headerItem?.name == getString(R.string.lbl_favorites) }
			if (favorites != null) rows.add(favorites, row) else rows.add(row)
			adapter.Retrieve()
		}
		if (folder.collectionType == CollectionType.TVSHOWS) {
			// Episodes show the show's artwork, like the same rows on Home ("Use series thumbnails")
			val seriesArt = userPreferences[UserPreferences.seriesThumbnailsEnabled]
			add(R.string.home_section_resume, ItemRowAdapter(context, BrowsingUtils.createResumeItemsRequest(id, BaseItemKind.EPISODE), 50, seriesArt, presenter, rows))
			add(R.string.home_section_next_up, ItemRowAdapter(context, BrowsingUtils.createGetNextUpRequest(id), seriesArt, presenter, rows))
			add(R.string.library_row_recently_aired, ItemRowAdapter(context, newest(BaseItemKind.EPISODE), 50, seriesArt, presenter, rows))
			add(R.string.library_row_recently_added, ItemRowAdapter(context, BrowsingUtils.createLatestMediaRequest(id, BaseItemKind.EPISODE, true), seriesArt, presenter, rows))
			topRated(BaseItemKind.SERIES)
			add(R.string.lbl_favorites, ItemRowAdapter(context, BrowsingUtils.createFavoriteItemsRequest(id, BaseItemKind.SERIES), 60, false, presenter, rows))
		} else {
			add(R.string.home_section_resume, ItemRowAdapter(context, BrowsingUtils.createResumeItemsRequest(id, BaseItemKind.MOVIE), 50, false, presenter, rows))
			add(R.string.library_row_recently_released, ItemRowAdapter(context, newest(BaseItemKind.MOVIE), 50, false, presenter, rows))
			add(R.string.library_row_recently_added, ItemRowAdapter(context, BrowsingUtils.createLatestMediaRequest(id), false, presenter, rows))
			topRated(BaseItemKind.MOVIE)
			add(R.string.lbl_favorites, ItemRowAdapter(context, BrowsingUtils.createFavoriteItemsRequest(id, BaseItemKind.MOVIE), 60, false, presenter, rows))
		}

		onItemViewClickedListener = OnItemViewClickedListener { _, item, _, row ->
			if (item !is BaseRowItem || row !is ListRow) return@OnItemViewClickedListener
			@Suppress("UNCHECKED_CAST")
			itemLauncher.launch(item, row.adapter as MutableObjectAdapter<Any>, requireContext())
		}
		onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
			val baseItem = (item as? BaseRowItem)?.baseItem ?: return@OnItemViewSelectedListener
			focusedItem.value = baseItem
			backgroundService.setBackground(baseItem)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		// Same placement and spacing as the Home rows
		setAlignment((40 * resources.displayMetrics.density).toInt())
		verticalGridView?.verticalSpacing = (12 * resources.displayMetrics.density).toInt()
	}
}

/** The Genres and Studios tabs: one tile per name, opening the library grid limited to it. */
class LibraryNameTilesFragment : Fragment() {
	private val folder by lazy { folderArgument() }
	private val api by inject<ApiClient>()
	private val navigationRepository by inject<NavigationRepository>()
	// Backdrop URLs per genre or studio, best first, kept while the page is open
	private val backdrops = mutableMapOf<UUID, List<String>>()
	// Titles already pictured, so genres that share a top title (Action, Adventure...) get different art
	private val usedTitles = mutableSetOf<UUID>()

	/**
	 * Backdrops of the best-rated titles in this genre or studio, for its tile: one no other tile uses
	 * first, then the rest as fallbacks for an image that won't load.
	 */
	private suspend fun representativeBackdrops(entry: BaseItemDto, studios: Boolean): List<String> = try {
		val type = if (folder.collectionType == CollectionType.TVSHOWS) BaseItemKind.SERIES else BaseItemKind.MOVIE
		val candidates = withContext(Dispatchers.IO) {
			api.itemsApi.getItems(GetItemsRequest(
				parentId = folder.id,
				includeItemTypes = setOf(type),
				recursive = true,
				genres = if (studios) null else setOfNotNull(entry.name),
				studioIds = if (studios) setOf(entry.id) else null,
				imageTypes = setOf(ImageType.BACKDROP),
				sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
				sortOrder = setOf(SortOrder.DESCENDING),
				enableImageTypes = setOf(ImageType.BACKDROP),
				imageTypeLimit = 1,
				limit = 6,
			)).content.items
		}
		// Back on the main thread, so picking and marking a title can't race another tile
		val ordered = candidates.sortedBy { it.id in usedTitles }
		ordered.firstOrNull()?.let { usedTitles += it.id }
		ordered.mapNotNull { it.itemBackdropImages.firstOrNull()?.getUrl(api, maxWidth = 480) }
	} catch (err: CancellationException) {
		// The tile left the screen (or the grid rebuilt it) while loading; let it try again next time
		throw err
	} catch (err: Exception) {
		Timber.w(err, "Failed to load a backdrop for %s", entry.name)
		emptyList()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		val studios = requireArguments().getBoolean(ARG_STUDIOS)
		var names by remember { mutableStateOf<List<BaseItemDto>?>(null) }
		LaunchedEffect(Unit) {
			val type = if (folder.collectionType == CollectionType.TVSHOWS) BaseItemKind.SERIES else BaseItemKind.MOVIE
			names = runCatching {
				withContext(Dispatchers.IO) {
					if (studios) api.studiosApi.getStudios(parentId = folder.id, includeItemTypes = setOf(type)).content.items
					else api.genresApi.getGenres(parentId = folder.id, includeItemTypes = setOf(type), sortBy = setOf(ItemSortBy.SORT_NAME)).content.items
				}
			}.onFailure { Timber.w(it, "Failed to load library tiles") }.getOrDefault(emptyList())
		}
		val list = names ?: return@content
		if (list.isEmpty()) {
			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				Text(stringResource(R.string.library_tiles_empty), color = LumaFinPalette.SecondaryText, fontSize = 16.sp)
			}
			return@content
		}
		LazyVerticalGrid(
			columns = GridCells.Fixed(4),
			contentPadding = PaddingValues(start = 48.dp, end = 32.dp, top = 12.dp, bottom = 32.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
			// Coming back from the side menu or the tabs lands on the tile you left
			modifier = Modifier.fillMaxSize().focusRestorer(),
		) {
			items(list, key = { it.id }) { entry ->
				// A studio's own logo when the server has one; otherwise a top title's backdrop under the name
				// Studio thumbs are full cards (logo on its own background) that fill the tile; a bare logo is padded
				val thumb = if (studios) entry.itemImages[ImageType.THUMB] else null
				val logo = if (studios && thumb == null) entry.itemImages[ImageType.PRIMARY] else null
				var backdropOptions by remember(entry.id) { mutableStateOf(backdrops[entry.id].orEmpty()) }
				// Some titles' artwork won't load from the server; move on to the next title's
				var backdropIndex by remember(entry.id) { mutableStateOf(0) }
				if (thumb == null && logo == null) LaunchedEffect(entry.id) {
					// Cached only once a load finishes; a cancelled one throws and is retried
					if (entry.id !in backdrops) backdrops[entry.id] = representativeBackdrops(entry, studios)
					backdropOptions = backdrops[entry.id].orEmpty()
				}
				NameTile(
					entry.name.orEmpty(),
					logoUrl = logo?.getUrl(api, maxHeight = 160),
					cardUrl = thumb?.getUrl(api, maxWidth = 480),
					backdropUrl = backdropOptions.getOrNull(backdropIndex),
					onBackdropFailed = { backdropIndex++ },
					onMenu = { showTileMenu(entry) },
				) {
					navigationRepository.navigate(Destinations.libraryFiltered(folder, entry.name.orEmpty(),
						genre = if (studios) null else entry.name, studioId = if (studios) entry.id.toString() else null))
				}
			}
		}
	}

	/** Pin to Home or Unpin for a genre or studio of this library (#96). */
	private fun showTileMenu(entry: BaseItemDto) {
		val shortcuts = GlobalContext.get().get<HomeShortcutPreferences>()
		val id = shortcuts.filterId(folder, entry)
		val pinned = shortcuts.isPinned(id)
		LumaFinMenuDialog(requireContext(), viewLifecycleOwner, entry.name.orEmpty(), fitContent = true).apply {
			row(getString(if (pinned) R.string.home_shortcut_unpin else R.string.home_shortcut_pin), navigates = false) {
				dismiss()
				if (pinned) shortcuts.unpin(id.toString()) else shortcuts.pinFilter(folder, entry)
				GlobalContext.get().get<CustomMessageRepository>().pushMessage(CustomMessage.HomeShortcutsChanged)
				Toast.makeText(requireContext(), if (pinned) R.string.home_shortcut_unpinned else R.string.home_shortcut_pinned, Toast.LENGTH_SHORT).show()
			}
			show()
		}
	}

	companion object {
		const val ARG_STUDIOS = "studios"
	}
}

@Composable
private fun NameTile(name: String, logoUrl: String?, cardUrl: String?, backdropUrl: String?, onBackdropFailed: () -> Unit, onMenu: () -> Unit, onClick: () -> Unit) {
	val strongFocus = koinInject<UserPreferences>()[UserPreferences.strongFocus]
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val shape = RoundedCornerShape(12.dp)
	ButtonBase(
		onClick = onClick,
		onLongClick = onMenu,
		shape = shape,
		interactionSource = interactionSource,
		colors = ButtonDefaults.colors(
			containerColor = LumaFinPalette.Raised,
			contentColor = LumaFinPalette.Text,
			focusedContainerColor = LumaFinPalette.FocusedRow,
			focusedContentColor = LumaFinPalette.Text,
		),
		modifier = Modifier
			.fillMaxWidth()
			.height(112.dp)
			// The remote's Menu key opens the same menu as holding OK
			.onPreviewKeyEvent { if (it.key == Key.Menu) { if (it.type == KeyEventType.KeyUp) onMenu(); true } else false }
			.then(if (focused) Modifier.border(if (strongFocus) 3.dp else 2.dp, if (strongFocus) Color.White else LumaFinPalette.Cyan, shape) else Modifier),
	) {
		when {
			// Compose's own image loading: the wrapped image view could start before a tile had a size and never draw
			cardUrl != null -> CoilImage(model = cardUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
			logoUrl != null -> CoilImage(
				model = logoUrl,
				contentDescription = null,
				contentScale = ContentScale.Fit,
				modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
			)
			backdropUrl != null -> {
				CoilImage(model = backdropUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), onError = { onBackdropFailed() })
				// Keeps the name readable over the artwork
				Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (focused) 0.35f else 0.55f)))
			}
		}
		if (logoUrl == null && cardUrl == null) Text(
			name,
			fontSize = 17.sp,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp),
		)
	}
}
