package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.preference.HiddenNextUp
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpMethod
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import timber.log.Timber
import java.util.UUID

/** One Home row defined by the server's Home Screen Sections plugin (#107). */
@Serializable
data class PluginSection(
	@SerialName("Section") val section: String,
	@SerialName("DisplayText") val displayText: String? = null,
	@SerialName("AdditionalData") val additionalData: String? = null,
)

@Serializable
private data class PluginSectionList(@SerialName("Items") val items: List<PluginSection> = emptyList())

/**
 * The server's Home Screen Sections plugin, when it's installed (#107). Missing or failing plugins
 * are treated as not installed, so Home quietly keeps LumaFin's own rows.
 */
object HomeScreenSectionsPlugin {
	private val json = Json { ignoreUnknownKeys = true; isLenient = true }

	/** Whether the plugin answered on the last Home load, for showing its setting. */
	val available = MutableStateFlow(false)

	/** The plugin's sections from the last Home load, for the Home editor. */
	val lastSections = MutableStateFlow<List<PluginSection>>(emptyList())

	/** Plugin sections LumaFin already has its own rows for; they aren't offered twice. */
	private val covered = setOf(
		"ContinueWatching", "NextUp", "ContinueWatchingNextUp", "MyMedia", "LiveTv", "BecauseYouWatched", "WatchAgain",
		"LatestMovies", "LatestShows", "LatestAlbums", "LatestBooks", "LatestAudioBooks", "LatestMusicVideos",
		"RecentlyAddedMovies", "RecentlyAddedShows", "RecentlyAddedAlbums", "RecentlyAddedArtists", "RecentlyAddedBooks",
		"RecentlyAddedAudioBooks", "RecentlyAddedMusicVideos", "RecentlyAddedInLibrary", "Collections", "Genre",
	)

	/** Rows only the plugin offers, which can be added to LumaFin's own Home (#107). */
	fun extraSections(sections: List<PluginSection>) = sections.filterNot { it.section in covered }

	/** Identifies a section, including the genre, person or studio it's about. */
	fun key(section: PluginSection) = section.section + "|" + section.additionalData.orEmpty()

	private val preferences get() = org.koin.core.context.GlobalContext.get().get<org.jellyfin.androidtv.preference.UserPreferences>()
	fun addedKeys(): Set<String> = preferences[org.jellyfin.androidtv.preference.UserPreferences.pluginHomeRows]
		.split(KEY_SEPARATOR).filter { it.isNotBlank() }.toSet()
	fun setAdded(keys: Set<String>) {
		preferences[org.jellyfin.androidtv.preference.UserPreferences.pluginHomeRows] = keys.joinToString(KEY_SEPARATOR)
	}
	private const val KEY_SEPARATOR = ";;"

	/** Rows that show titles not in the library; they need request cards first (#100). */
	private val unsupported = setOf("Discover", "DiscoverMovies", "DiscoverTV", "MyRequests")

	suspend fun sections(api: ApiClient, userId: UUID): List<PluginSection>? = try {
		val response = withContext(Dispatchers.IO) {
			api.request(HttpMethod.GET, "/HomeScreen/Sections", emptyMap(), mapOf("userId" to userId.toString()), null)
		}
		if (response.status !in 200..299) null
		else json.decodeFromString<PluginSectionList>(response.body.decodeToString()).items
			.filterNot { it.section in unsupported || it.section.startsWith("Upcoming") }
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.d(error, "Home Screen Sections plugin not available")
		null
	}.also { available.value = it != null; lastSections.value = it.orEmpty() }

	suspend fun items(api: ApiClient, userId: UUID, section: PluginSection): List<BaseItemDto> = try {
		val response = withContext(Dispatchers.IO) {
			api.request(HttpMethod.GET, "/HomeScreen/Section/{section}", mapOf("section" to section.section),
				buildMap {
					put("userId", userId.toString())
					section.additionalData?.let { put("additionalData", it) }
				}, null)
		}
		if (response.status !in 200..299) emptyList()
		else json.decodeFromString(BaseItemDtoQueryResult.serializer(), response.body.decodeToString()).items
	} catch (error: CancellationException) {
		throw error
	} catch (error: Exception) {
		Timber.w(error, "Failed to load plugin section %s", section.section)
		emptyList()
	}
}

/** A plugin section without a LumaFin equivalent, shown as a normal row of titles. */
class PluginSectionRow(private val api: ApiClient, private val userId: UUID, private val section: PluginSection) : HomeFragmentRow {
	override suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val items = HomeScreenSectionsPlugin.items(api, userId, section)
			.let { if (section.section == "NextUp") HiddenNextUp.filter(it) else it }
		if (items.isEmpty()) return
		val title = section.displayText?.takeIf { it.isNotBlank() } ?: section.section
		val rowAdapter = ItemRowAdapter(context, items, cardPresenter, rowsAdapter, true)
		val row = ListRow(HeaderItem(title), rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
