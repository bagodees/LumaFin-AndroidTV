package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.focusOutline
import org.jellyfin.androidtv.ui.base.rememberAccessibilityOptions
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.jellyfin.androidtv.ui.composable.rememberReducedMotion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpMethod
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber

/** A title from Seerr that may not be in the library yet (#100). [mediaId] is Seerr's (TMDB) id. */
data class SeerrItem(
	val name: String,
	val mediaType: String,
	val mediaId: Int,
	/** Landscape artwork; replaced with the titled English backdrop when SeerrFin has one. */
	val imageUrl: String?,
	val posterUrl: String? = null,
	val year: Int?,
	val rating: Float? = null,
	val tmdbId: Int? = null,
	/** My Requests: where the request stands ("Available", "Downloading · 45%"), shown instead of the year. */
	val statusLabel: String? = null,
	/** My Requests: the library item once the title has arrived, opened instead of the request dialog. */
	val jellyfinItemId: java.util.UUID? = null,
) {
	/** A stand-in item for the preview above the rows: title, year, rating and kind. */
	fun previewItem(overview: String? = null) = org.jellyfin.sdk.model.api.BaseItemDto(
		id = java.util.UUID.nameUUIDFromBytes("seerr|$mediaType|$mediaId".toByteArray()),
		name = name,
		type = if (mediaType == "tv") org.jellyfin.sdk.model.api.BaseItemKind.SERIES else org.jellyfin.sdk.model.api.BaseItemKind.MOVIE,
		productionYear = year,
		communityRating = rating,
		overview = overview,
	)
}

/**
 * Seerr through the SeerrFin plugin on the Jellyfin server (#100): discover rows and requests use the
 * signed-in Jellyfin session, so there's no Seerr address or login in LumaFin. Without the plugin
 * nothing shows.
 */
object SeerrFin {
	private val json = Json { ignoreUnknownKeys = true; isLenient = true }

	/** Whether SeerrFin answered on the last Home load, for showing its setting. */
	val available = MutableStateFlow(false)

	/** Discover rows LumaFin shows, in order: title and SeerrFin path. */
	val rows = listOf(
		R.string.seerr_trending_movies to "discover/movies/trending",
		R.string.seerr_trending_shows to "discover/tv/trending",
		R.string.seerr_upcoming_movies to "discover/movies/upcoming",
		R.string.seerr_upcoming_shows to "discover/tv/upcoming",
	)

	private suspend fun get(api: ApiClient, path: String, query: Map<String, Any?> = emptyMap()) = withContext(Dispatchers.IO) {
		api.request(HttpMethod.GET, "/SeerrFin/$path", emptyMap(), query, null)
	}

	suspend fun discover(api: ApiClient, path: String): List<SeerrItem>? = try {
		val response = get(api, path, mapOf("limit" to 20))
		if (response.status !in 200..299) null
		else json.decodeFromString(BaseItemDtoQueryResult.serializer(), response.body.decodeToString()).items.mapNotNull { item ->
			val ids = item.providerIds.orEmpty()
			val id = ids["Jellyseerr"]?.toIntOrNull() ?: return@mapNotNull null
			fun absolute(url: String?) = url?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("/")) api.baseUrl?.trimEnd('/') + it else it }
			val image = ids["JellyseerrBackdrop"]?.takeIf { it.isNotBlank() } ?: ids["JellyseerrPoster"]?.takeIf { it.isNotBlank() }
			SeerrItem(
				name = item.name.orEmpty(),
				mediaType = item.sourceType ?: if (path.contains("/tv/")) "tv" else "movie",
				mediaId = id,
				// Cached images are served by the Jellyfin server
				imageUrl = absolute(image),
				posterUrl = absolute(ids["JellyseerrPoster"]),
				year = item.premiereDate?.year?.takeIf { it > 1970 },
				rating = item.communityRating,
				tmdbId = ids["Tmdb"]?.toIntOrNull(),
			)
		// Seerr's pages can repeat a title
		}.distinctBy { it.mediaType to it.mediaId }
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.d(error, "SeerrFin not available")
		null
	}

	/** The English backdrop with the title on it, like the artwork on LumaFin's other cards; null without one. */
	suspend fun titledBackdrop(api: ApiClient, item: SeerrItem): String? = try {
		val tmdbId = item.tmdbId
		if (tmdbId == null) null else {
			val response = get(api, "backdrop/${item.mediaType}/$tmdbId")
			if (response.status !in 200..299) null
			else json.parseToJsonElement(response.body.decodeToString()).jsonObject.let { body ->
				if (body["hasEnglishBackdrop"]?.jsonPrimitive?.content != "true") null
				else body["backdropUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
					?.let { if (it.startsWith("/")) api.baseUrl?.trimEnd('/') + it else it }
			}
		}
	} catch (error: CancellationException) { throw error } catch (error: Exception) { null }

	/** Logos for previewed Seerr titles, by preview item id; read by the preview's logo. */
	val logos = MutableStateFlow<Map<java.util.UUID, String>>(emptyMap())
	private var tmdbKey: String? = null
	private val overviews = mutableMapOf<String, String>()

	/** The title's description from Seerr, and its logo from TMDB when SeerrFin has a TMDB key. Cached. */
	suspend fun enrich(api: ApiClient, item: SeerrItem): String? {
		val key = "${item.mediaType}/${item.mediaId}"
		overviews[key]?.let { return it }
		val overview = try {
			val response = get(api, "details/$key")
			if (response.status !in 200..299) null
			else json.parseToJsonElement(response.body.decodeToString()).jsonObject["overview"]?.jsonPrimitive?.content
		} catch (error: CancellationException) { throw error } catch (error: Exception) { null }
		overview?.let { overviews[key] = it }
		loadLogo(api, item)
		return overview
	}

	private suspend fun loadLogo(api: ApiClient, item: SeerrItem) {
		val id = item.previewItem().id
		if (id in logos.value) return
		try {
			val apiKey = tmdbKey ?: run {
				val response = get(api, "client-settings")
				if (response.status !in 200..299) return
				json.parseToJsonElement(response.body.decodeToString()).jsonObject["tmdbApiKey"]?.jsonPrimitive?.content.orEmpty()
					.also { tmdbKey = it }
			}
			if (apiKey.isBlank()) return
			val tmdbId = item.tmdbId ?: return
			val body = withContext(Dispatchers.IO) {
				java.net.URL("https://api.themoviedb.org/3/${if (item.mediaType == "tv") "tv" else "movie"}/$tmdbId/images?api_key=$apiKey&include_image_language=en,null")
					.openStream().use { it.readBytes().decodeToString() }
			}
			val path = json.parseToJsonElement(body).jsonObject["logos"]?.let { it as? kotlinx.serialization.json.JsonArray }
				?.firstOrNull()?.jsonObject?.get("file_path")?.jsonPrimitive?.content ?: return
			logos.value = logos.value + (id to "https://image.tmdb.org/t/p/w500$path")
		} catch (error: CancellationException) { throw error } catch (error: Exception) {
			Timber.d(error, "No logo for %s", item.name)
		}
	}

	/** Seerr's status for a title: 5 available, 4 partly available, 2 or 3 requested, otherwise not requested. */
	suspend fun status(api: ApiClient, item: SeerrItem): Int? = try {
		val response = get(api, "details/${item.mediaType}/${item.mediaId}")
		if (response.status !in 200..299) null
		else json.parseToJsonElement(response.body.decodeToString()).jsonObject["mediaInfo"]
			?.let { (it as? JsonObject)?.get("status")?.jsonPrimitive?.intOrNull }
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		null
	}

	/** The signed-in user's requests, newest first, with where each one stands. Null when SeerrFin can't answer. */
	suspend fun myRequests(context: Context, api: ApiClient): List<SeerrItem>? = try {
		val response = get(api, "requests", mapOf("take" to 20))
		if (response.status !in 200..299) null
		else json.parseToJsonElement(response.body.decodeToString()).jsonObject["requests"]
			?.let { it as? kotlinx.serialization.json.JsonArray }.orEmpty()
			.mapNotNull { element ->
				val request = element as? JsonObject ?: return@mapNotNull null
				fun text(key: String) = request[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
				val tmdbId = request["tmdbId"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
				val progress = request["servarrProgress"] as? JsonObject
				val percent = progress?.get("percent")?.jsonPrimitive?.intOrNull ?: 0
				val inLibrary = text("jellyfinItemId") != null
				// Plain words instead of Seerr's and the download apps' own labels
				val status = when (progress?.get("statusKey")?.jsonPrimitive?.contentOrNull) {
					"queued" -> if (percent in 1..99) context.getString(R.string.seerr_status_downloading, percent) else context.getString(R.string.seerr_status_waiting)
					"unreleased" -> context.getString(R.string.seerr_status_unreleased)
					"failed" -> context.getString(R.string.seerr_status_failed)
					"downloaded-monitored", "downloaded-unmonitored" ->
						context.getString(if (inLibrary) R.string.seerr_status_in_library else R.string.seerr_status_arriving)
					else -> when (text("mediaStatusLabel")) {
						"Available" -> context.getString(R.string.seerr_status_in_library)
						"Partially Available" -> context.getString(R.string.seerr_status_partly_in_library)
						"Pending Approval" -> context.getString(R.string.seerr_status_awaiting_approval)
						"Declined" -> context.getString(R.string.seerr_status_declined)
						"Failed" -> context.getString(R.string.seerr_status_failed)
						"Deleted", "Blocklisted" -> null
						else -> context.getString(R.string.seerr_status_requested_short)
					}
				}
				SeerrItem(
					name = text("title") ?: return@mapNotNull null,
					mediaType = text("type") ?: "movie",
					mediaId = tmdbId,
					imageUrl = text("backdropUrl") ?: text("posterUrl"),
					posterUrl = text("posterUrl"),
					year = request["year"]?.jsonPrimitive?.intOrNull,
					tmdbId = tmdbId,
					statusLabel = status,
					jellyfinItemId = text("jellyfinItemId")?.let { runCatching { it.toUUID() }.getOrNull() },
				)
			}.distinctBy { it.mediaType to it.mediaId }
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.d(error, "SeerrFin requests not available")
		null
	}

	/**
	 * Aired seasons of a show in the library that Seerr can still get: only when Seerr already knows the
	 * show as partly available, so a show Seerr hasn't matched never offers seasons that are already here.
	 */
	suspend fun missingSeasons(api: ApiClient, tmdbId: Int): List<Int>? = try {
		val response = get(api, "details/tv/$tmdbId")
		if (response.status !in 200..299) null
		else missingSeasons(json.parseToJsonElement(response.body.decodeToString()).jsonObject, java.time.LocalDate.now())
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.d(error, "No Seerr season details for %d", tmdbId)
		null
	}

	/** Requests a title; a show is requested with all seasons unless [seasons] names some. True when Seerr accepted it. */
	suspend fun request(api: ApiClient, item: SeerrItem, seasons: List<Int> = emptyList()): Boolean = try {
		val body = buildJsonObject {
			put("MediaType", item.mediaType); put("MediaId", item.mediaId)
			if (seasons.isNotEmpty()) put("Seasons", kotlinx.serialization.json.JsonArray(seasons.map { kotlinx.serialization.json.JsonPrimitive(it) }))
		}
		withContext(Dispatchers.IO) {
			api.request(HttpMethod.POST, "/SeerrFin/request", emptyMap(), emptyMap(), body)
		}.status in 200..299
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.w(error, "Seerr request failed")
		false
	}
}

/** The user's Seerr requests and where each stands (#100); arrived titles open in the library. */
class SeerrRequestsRow(private val api: ApiClient) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val requests = SeerrFin.myRequests(context, api).orEmpty()
		if (requests.isEmpty()) return
		val portrait = cardPresenter.portrait
		val items = if (portrait) requests else coroutineScope {
			requests.map { item -> async { SeerrFin.titledBackdrop(api, item)?.let { item.copy(imageUrl = it) } ?: item } }
				.map { it.await() }
		}
		val adapter = ArrayObjectAdapter(SeerrCardPresenter(portrait, cardPresenter.homeCardHeight)).apply { addAll(0, items) }
		rowsAdapter.add(ListRow(HeaderItem(context.getString(R.string.seerr_my_requests)), adapter))
	}
}

/** A Home row of Seerr titles; selecting one offers to request it. */
class SeerrDiscoverRow(private val api: ApiClient, private val title: Int, private val path: String) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val discovered = SeerrFin.discover(api, path).orEmpty()
		if (discovered.isEmpty()) return
		val portrait = cardPresenter.portrait
		// Landscape cards use the titled backdrop like other cards; portrait cards use the poster
		val items = if (portrait) discovered else coroutineScope {
			discovered.map { item -> async { SeerrFin.titledBackdrop(api, item)?.let { item.copy(imageUrl = it) } ?: item } }
				.map { it.await() }
		}
		val adapter = ArrayObjectAdapter(SeerrCardPresenter(portrait, cardPresenter.homeCardHeight)).apply { addAll(0, items) }
		rowsAdapter.add(ListRow(HeaderItem(context.getString(title)), adapter))
	}
}

/** Cards for Seerr titles, drawn like Home's own cards: same size, focus, dimming and caption. */
class SeerrCardPresenter(private val portrait: Boolean = false, private val cardHeight: Int = 150) : Presenter() {
	private class SeerrViewHolder(view: ComposeView) : ViewHolder(view) {
		val item = MutableStateFlow<SeerrItem?>(null)
		val focused = MutableStateFlow(false)
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		// Rows measure cards before they're attached, so borrow the row's composition and lifecycle like CardPresenter
		val view = ComposeView(parent.context).apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(view: android.view.View) {
					val owner = view.parent as? android.view.View ?: return
					setParentCompositionContext(owner.findViewTreeCompositionContext())
					setViewTreeLifecycleOwner(owner.findViewTreeLifecycleOwner())
					setViewTreeSavedStateRegistryOwner(owner.findViewTreeSavedStateRegistryOwner())
				}
				override fun onViewDetachedFromWindow(view: android.view.View) = Unit
			})
			isFocusable = true
			isFocusableInTouchMode = true
		}
		val holder = SeerrViewHolder(view)
		// Set once here, before the row wraps it for the focus zoom; replacing it on bind turned the zoom off
		view.onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus -> holder.focused.value = hasFocus }
		val preferences = org.koin.core.context.GlobalContext.get().get<org.jellyfin.androidtv.preference.UserPreferences>()
		view.setContent {
			val seerr by holder.item.collectAsState()
			val isFocused by holder.focused.collectAsState()
			val item = seerr ?: return@setContent
			val titlesHidden by preferences.homeCardTitlesHiddenChanges.collectAsState(
				initial = preferences[org.jellyfin.androidtv.preference.UserPreferences.newHomeEnabled] && !preferences[org.jellyfin.androidtv.preference.UserPreferences.newHomeCardTitles]
			)
			val cardSize by preferences.mediaCardSizeChanges.collectAsState(initial = preferences[org.jellyfin.androidtv.preference.UserPreferences.mediaCardSize])
			val height = cardHeight.dp * cardSize.scale
			val aspect = if (portrait) 2f / 3f else 16f / 9f
			val accessibility = rememberAccessibilityOptions()
			val reducedMotion = rememberReducedMotion()
			val cardAlpha by animateFloatAsState(
				targetValue = if (isFocused) 1f else 0.7f,
				animationSpec = if (reducedMotion) snap() else spring(),
				label = "SeerrCardFocusAlpha",
			)
			val card = @Composable {
				ItemCard(
					image = {
						AsyncImage(
							url = if (portrait) item.posterUrl ?: item.imageUrl else item.imageUrl,
							aspectRatio = aspect,
							scaleType = android.widget.ImageView.ScaleType.CENTER_CROP,
							modifier = Modifier.fillMaxSize(),
						)
					},
					modifier = Modifier
						.size(DpSize(height * aspect, height))
						.then(if (isFocused && !accessibility.strongFocus) Modifier.border(2.dp, JellyfinTheme.colorScheme.inputFocused, JellyfinTheme.shapes.medium) else Modifier)
						.focusOutline(isFocused, JellyfinTheme.shapes.medium, accessibility.strongFocus)
						.graphicsLayer(alpha = cardAlpha),
				)
			}
			if (titlesHidden) card()
			else ItemPreview(
				card = card,
				metadataMinHeight = with(LocalDensity.current) { 50.sp.toDp() } + 2.dp,
				title = {
					Text(
						item.name,
						fontSize = 15.sp,
						lineHeight = 18.sp,
						fontWeight = FontWeight.SemiBold,
						color = Color.White,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth(),
					)
				},
				subtitle = {
					Text(
						item.statusLabel ?: item.year?.toString() ?: " ",
						fontSize = 11.sp,
						lineHeight = 14.sp,
						color = Color.White.copy(alpha = 0.85f),
						maxLines = 1,
					)
				},
			)
		}
		return holder
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as? SeerrViewHolder ?: return
		holder.item.value = item as? SeerrItem
		holder.focused.value = holder.view.isFocused
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as? SeerrViewHolder)?.item?.value = null
	}
}


/**
 * Aired seasons in Seerr's show [details] that Seerr hasn't requested, isn't getting and doesn't have.
 * Empty unless Seerr knows the show as partly available (status 4), so a show Seerr hasn't matched never
 * offers seasons that are already in the library.
 */
internal fun missingSeasons(details: JsonObject, today: java.time.LocalDate): List<Int> {
	val mediaInfo = details["mediaInfo"] as? JsonObject
	if (mediaInfo?.get("status")?.jsonPrimitive?.intOrNull != 4) return emptyList()
	// Seasons Seerr has requested (2), is getting (3) or has in part or in full (4, 5)
	val known = (mediaInfo["seasons"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { season ->
		val obj = season as? JsonObject ?: return@mapNotNull null
		val status = obj["status"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
		obj["seasonNumber"]?.jsonPrimitive?.intOrNull?.takeIf { status in 2..5 }
	}.toSet()
	val day = today.toString()
	return (details["seasons"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { season ->
		val obj = season as? JsonObject ?: return@mapNotNull null
		val number = obj["seasonNumber"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
		val aired = obj["airDate"]?.jsonPrimitive?.contentOrNull?.takeIf { it.length >= 10 }?.let { it.take(10) <= day } == true
		number.takeIf { aired && it !in known }
	}.sorted()
}
