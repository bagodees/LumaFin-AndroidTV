package org.jellyfin.androidtv.ui.itemdetail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.browsing.BrowsingUtils
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.apiclient.EmptyResponse
import org.jellyfin.androidtv.util.dp
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.util.UUID

/** A season pill in the LumaFin series page's tab row (#85). */
class SeasonTab(val season: BaseItemDto)

/** Season pills; moving onto one picks it, so the episodes below follow focus like tabs. */
class SeasonTabPresenter : Presenter() {
	var onPicked: (BaseItemDto) -> Unit = {}
	private var selectedId: UUID? = null
	private val views = mutableSetOf<TabView>()

	fun select(id: UUID) {
		selectedId = id
		views.forEach { it.picked = it.season?.id == id }
	}

	override fun onCreateViewHolder(parent: ViewGroup) = ViewHolder(TabView(parent.context).also { views += it })

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val view = viewHolder.view as TabView
		val season = (item as? SeasonTab)?.season
		view.season = season
		view.text = season?.name
		view.picked = season != null && season.id == selectedId
		view.onFocused = { season?.let { if (it.id != selectedId) onPicked(it) } }
		// Entering the row from above should land on the picked season, not the first one
		if (view.picked) view.post {
			val grid = view.parent as? RecyclerView ?: return@post
			val position = grid.getChildAdapterPosition(view)
			if (!grid.hasFocus() && position >= 0 && grid is BaseGridView) grid.selectedPosition = position
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		val view = viewHolder.view as TabView
		view.season = null
		view.onFocused = null
	}
}

private class TabView(context: Context) : AppCompatTextView(context) {
	var season: BaseItemDto? = null
	var onFocused: (() -> Unit)? = null
	// Its own "checked" state: leanback pushes the row's activated state down to every child view
	var picked = false
		set(value) {
			if (field == value) return
			field = value
			refreshDrawableState()
		}

	init {
		isFocusable = true
		isFocusableInTouchMode = true
		if (android.os.Build.VERSION.SDK_INT >= 26) defaultFocusHighlightEnabled = false
		val strong = GlobalContext.get().get<UserPreferences>()[UserPreferences.strongFocus]
		fun color(id: Int) = ContextCompat.getColor(context, id)
		fun pill(fill: Int, border: Int? = null) = GradientDrawable().apply {
			setColor(fill)
			cornerRadius = 18.dp(context).toFloat()
			if (border != null) setStroke((if (strong) 3 else 2).dp(context), border)
		}
		val focusBorder = if (strong) Color.WHITE else color(R.color.lumafin_cyan)
		// The picked season is a light pill; focus adds the usual outline
		background = StateListDrawable().apply {
			addState(intArrayOf(android.R.attr.state_focused, android.R.attr.state_checked), pill(color(R.color.lumafin_text), focusBorder))
			addState(intArrayOf(android.R.attr.state_focused), pill(color(R.color.lumafin_focus_row), focusBorder))
			addState(intArrayOf(android.R.attr.state_checked), pill(color(R.color.lumafin_text)))
			addState(intArrayOf(), pill(color(R.color.lumafin_raised)))
		}
		setTextColor(ColorStateList(
			arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(android.R.attr.state_focused), intArrayOf()),
			intArrayOf(color(R.color.lumafin_background), color(R.color.lumafin_text), color(R.color.lumafin_secondary_text)),
		))
		typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
		textSize = 14f
		gravity = Gravity.CENTER
		maxLines = 1
		setPadding(16.dp(context), 0, 16.dp(context), 0)
		layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 36.dp(context))
	}

	override fun onCreateDrawableState(extraSpace: Int): IntArray {
		val state = super.onCreateDrawableState(extraSpace + 1)
		return if (picked) mergeDrawableStates(state, intArrayOf(android.R.attr.state_checked)) else state
	}

	override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
		if (gainFocus) onFocused?.invoke()
	}
}

/**
 * LumaFin series page (#85): season tabs right under the header with the picked season's episodes
 * as wide cards below, in place of the Next up and Seasons rows. Starts on the next-up episode's season.
 */
fun FullDetailsFragment.addSeasonEpisodeRows(rows: MutableObjectAdapter<Row>) {
	val context = requireContext()
	val seriesId = mBaseItem.id
	lifecycleScope.launch {
		val api by inject<ApiClient>()
		val seasons = try {
			withContext(Dispatchers.IO) {
				api.tvShowsApi.getSeasons(seriesId = seriesId, fields = ItemRepository.itemFields).content.items
			}
		} catch (err: ApiClientException) {
			Timber.w(err, "Failed to get seasons")
			return@launch
		}
		if (seasons.isEmpty() || !isAdded || mBaseItem.id != seriesId) return@launch
		val nextUp = getNextUpEpisode()
		if (!isAdded || mBaseItem.id != seriesId) return@launch
		val start = seasons.firstOrNull { it.id == nextUp?.seasonId }
			?: seasons.firstOrNull { it.userData?.played == false }
			?: seasons.first()

		val tabs = SeasonTabPresenter()
		val tabsRow = ListRow(null, ArrayObjectAdapter(tabs).apply { seasons.forEach { add(SeasonTab(it)) } })
		// Right after the details row; the other rows keep their order below
		rows.add(1.coerceAtMost(rows.size()), tabsRow)
		var episodesRow: ListRow? = null
		fun show(season: BaseItemDto) {
			tabs.select(season.id)
			val adapter = ItemRowAdapter(context, BrowsingUtils.createSeasonEpisodesRequest(season.id), 0, false, true, CardPresenter.homeRows(), rows)
			val row = ListRow(null, adapter)
			adapter.setRow(row)
			// An empty season removes its row, so fall back to inserting under the tabs
			val index = episodesRow?.let { rows.indexOf(it) }?.takeIf { it >= 0 }
			if (index != null) rows.set(index, row) else rows.add(rows.indexOf(tabsRow) + 1, row)
			episodesRow = row
			// Start the row on the next-up episode when it's in this season
			val target = nextUp?.takeIf { it.seasonId == season.id }?.id
			if (target != null) adapter.setRetrieveFinishedListener(object : EmptyResponse(lifecycle) {
				override fun onResponse() {
					val index = (0 until adapter.size()).firstOrNull { (adapter[it] as? BaseRowItem)?.baseItem?.id == target } ?: return
					view?.post {
						val rowIndex = rows.indexOf(row).takeIf { it >= 0 } ?: return@post
						val holder = mRowsFragment?.getRowViewHolder(rowIndex) as? ListRowPresenter.ViewHolder ?: return@post
						if (!holder.gridView.hasFocus()) holder.gridView.selectedPosition = index
					}
				}
			})
			adapter.Retrieve()
		}
		tabs.onPicked = ::show
		show(start)
	}
}
