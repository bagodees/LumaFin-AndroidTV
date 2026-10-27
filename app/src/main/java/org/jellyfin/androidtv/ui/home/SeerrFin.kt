package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.border
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
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpMethod
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import timber.log.Timber

/** A title from Seerr that may not be in the library yet (#100). [mediaId] is Seerr's (TMDB) id. */
data class SeerrItem(
	val name: String,
	val mediaType: String,
	val mediaId: Int,
	val imageUrl: String?,
	val year: Int?,
	val rating: Float? = null,
	val tmdbId: Int? = null,
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
			val image = ids["JellyseerrBackdrop"]?.takeIf { it.isNotBlank() } ?: ids["JellyseerrPoster"]?.takeIf { it.isNotBlank() }
			SeerrItem(
				name = item.name.orEmpty(),
				mediaType = item.sourceType ?: if (path.contains("/tv/")) "tv" else "movie",
				mediaId = id,
				// Cached images are served by the Jellyfin server
				imageUrl = image?.let { if (it.startsWith("/")) api.baseUrl?.trimEnd('/') + it else it },
				year = item.premiereDate?.year?.takeIf { it > 1970 },
				rating = item.communityRating,
				tmdbId = ids["Tmdb"]?.toIntOrNull(),
			)
		}
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.d(error, "SeerrFin not available")
		null
	}

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

	/** Requests a title; a show is requested with all seasons. True when Seerr accepted it. */
	suspend fun request(api: ApiClient, item: SeerrItem): Boolean = try {
		val body = buildJsonObject { put("MediaType", item.mediaType); put("MediaId", item.mediaId) }
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

/** A Home row of Seerr titles; selecting one offers to request it. */
class SeerrDiscoverRow(private val api: ApiClient, private val title: Int, private val path: String) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val items = SeerrFin.discover(api, path).orEmpty()
		if (items.isEmpty()) return
		val adapter = ArrayObjectAdapter(SeerrCardPresenter()).apply { addAll(0, items) }
		rowsAdapter.add(ListRow(HeaderItem(context.getString(title)), adapter))
	}
}

/** Landscape cards for Seerr titles, sized like Home's own cards. */
class SeerrCardPresenter : Presenter() {
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
		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val seerr = item as? SeerrItem ?: return
		val view = viewHolder.view as ComposeView
		val focused = MutableStateFlow(view.isFocused)
		view.onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus -> focused.value = hasFocus }
		view.setContent {
			val isFocused by focused.collectAsState()
			val shape = RoundedCornerShape(12.dp)
			Column(modifier = Modifier.width(267.dp)) {
				Box(
					modifier = Modifier
						.width(267.dp)
						.height(150.dp)
						.clip(shape)
						.then(if (isFocused) Modifier.border(3.dp, LumaFinPalette.Cyan, shape) else Modifier),
				) {
					AsyncImage(url = seerr.imageUrl, aspectRatio = 16f / 9f, modifier = Modifier.fillMaxSize())
				}
				Text(
					seerr.name,
					fontSize = 14.sp,
					color = if (isFocused) Color.White else LumaFinPalette.SecondaryText,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.padding(top = 6.dp, start = 4.dp),
				)
			}
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}

/** Shows a Seerr title's status and offers Request (#100). Nothing is requested without choosing Request. */
fun showSeerrRequest(context: Context, owner: LifecycleOwner, api: ApiClient, item: SeerrItem, scope: kotlinx.coroutines.CoroutineScope) {
	scope.launch {
		val status = SeerrFin.status(api, item)
		val kind = context.getString(if (item.mediaType == "tv") R.string.seerr_kind_show else R.string.seerr_kind_movie)
		val detail = listOfNotNull(item.year?.toString(), kind).joinToString(" · ")
		LumaFinMenuDialog(context, owner, item.name, detail, fitContent = true).apply {
			when (status) {
				5 -> body(context.getString(R.string.seerr_status_available))
				4 -> body(context.getString(R.string.seerr_status_partly_available))
				2, 3 -> body(context.getString(R.string.seerr_status_requested))
				else -> {
					body(context.getString(if (item.mediaType == "tv") R.string.seerr_request_show_detail else R.string.seerr_request_movie_detail))
					button(context.getString(R.string.seerr_request), primary = true) {
						dismiss()
						scope.launch {
							val ok = SeerrFin.request(api, item)
							android.widget.Toast.makeText(context,
								context.getString(if (ok) R.string.seerr_requested else R.string.seerr_request_failed, item.name),
								android.widget.Toast.LENGTH_LONG).show()
						}
					}
				}
			}
			button(context.getString(android.R.string.cancel)) { dismiss() }
			show()
		}
	}
}
