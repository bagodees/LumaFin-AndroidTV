package org.jellyfin.androidtv.ui.browsing

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.model.FilterOptions
import org.jellyfin.androidtv.data.model.LibraryResolution
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.util.dp
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.filterApi
import org.koin.core.context.GlobalContext
import timber.log.Timber

fun createHideWatchedButton(context: Context) = TextView(context).apply {
	gravity = Gravity.CENTER
	textSize = 12f
	typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
	isFocusable = true
	isClickable = true
	minimumWidth = 26.dp(context)
	minimumHeight = 26.dp(context)
	setPadding(4.dp(context), 4.dp(context), 4.dp(context), 4.dp(context))
	compoundDrawablePadding = 0
	val icon = ContextCompat.getDrawable(context, R.drawable.ic_unwatch)?.mutate()?.apply { setBounds(0, 0, 18.dp(context), 18.dp(context)) }
	setCompoundDrawablesRelative(icon, null, null, null)
	val tint = ContextCompat.getColorStateList(context, R.drawable.button_icon_tint)
	setTextColor(tint)
	TextViewCompat.setCompoundDrawableTintList(this, tint)
	fun circle(fill: Int, stroke: Int? = null) = GradientDrawable().apply {
		setColor(fill); shape = GradientDrawable.OVAL
		if (stroke != null) setStroke(2.dp(context), stroke)
	}
	background = StateListDrawable().apply {
		addState(intArrayOf(android.R.attr.state_focused, android.R.attr.state_activated),
			circle(ContextCompat.getColor(context, R.color.lumafin_cyan), ContextCompat.getColor(context, R.color.lumafin_purple)))
		addState(intArrayOf(android.R.attr.state_focused), circle(ContextCompat.getColor(context, R.color.lumafin_cyan)))
		addState(intArrayOf(android.R.attr.state_activated), circle(ContextCompat.getColor(context, R.color.lumafin_raised), ContextCompat.getColor(context, R.color.lumafin_purple)))
		addState(intArrayOf(), circle(Color.TRANSPARENT))
	}
}

fun updateHideWatchedButton(button: TextView, active: Boolean) {
	button.isActivated = active
	val state = button.context.getString(if (active) R.string.library_filter_on else R.string.library_filter_off)
	button.text = null
	button.contentDescription = button.context.getString(R.string.library_hide_watched_state, state)
}

internal fun BrowseGridFragment.showModernLibraryFilters(adapter: ItemRowAdapter, api: ApiClient, allowResolution: Boolean, onChanged: () -> Unit) {
	val request = adapter.itemsRequest ?: return
	val root = view ?: return
	val sessions = GlobalContext.get().get<SessionRepository>()
	val session = sessions.currentSession.value
	fun current() = isAdded && view === root && session == sessions.currentSession.value &&
		viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
	var finished = false
	val loading = LumaFinMenuDialog(requireContext(), viewLifecycleOwner, getString(R.string.library_filters_title), getString(R.string.library_filter_loading))
	loading.button(getString(android.R.string.cancel)) { loading.dismiss() }
	val job = viewLifecycleOwner.lifecycleScope.launch(start = CoroutineStart.LAZY) {
		try {
			val available = api.filterApi.getQueryFiltersLegacy(userId = request.userId, parentId = request.parentId, includeItemTypes = request.includeItemTypes).content
			finished = true
			loading.dismiss()
			if (current()) modernFilterMenu(adapter, available.genres.orEmpty().distinct().sorted(), available.years.orEmpty().distinct().sortedDescending(),
				allowResolution, { if (current()) onChanged() }, ::current)
		} catch (cancelled: CancellationException) {
			throw cancelled
		} catch (failure: Exception) {
			finished = true
			loading.dismiss()
			Timber.w(failure, "Unable to load library filter options")
			if (current()) {
				val error = LumaFinMenuDialog(requireContext(), viewLifecycleOwner, getString(R.string.library_filters_title), getString(R.string.library_filter_load_failed))
				error.button(getString(android.R.string.cancel)) { error.dismiss() }
				error.button(getString(R.string.home_shortcut_retry), true) { error.dismiss(); if (current()) showModernLibraryFilters(adapter, api, allowResolution, onChanged) }
				error.show()
			}
		}
	}
	loading.onDismiss { if (!finished) job.cancel() }
	loading.show()
	job.start()
}

private fun BrowseGridFragment.modernFilterMenu(
	adapter: ItemRowAdapter, genres: List<String>, years: List<Int>, allowResolution: Boolean,
	onChanged: () -> Unit, current: () -> Boolean, draft: FilterOptions = adapter.filters.copy(), focus: Int = 0,
) {
	if (!current()) return
	val any = getString(R.string.library_filter_any)
	val menu = LumaFinMenuDialog(requireContext(), viewLifecycleOwner, getString(R.string.library_filters_title), getString(R.string.library_filter_menu_description))
	var selectedFocus = focus
	fun redisplay() = modernFilterMenu(adapter, genres, years, allowResolution, onChanged, current, draft, selectedFocus)
	fun choose(title: Int, labels: List<String>, selected: Int, action: (Int) -> Unit) {
		if (!current()) return
		var picked = false
		val choices = LumaFinMenuDialog(requireContext(), viewLifecycleOwner, getString(title), getString(R.string.library_filter_choice_description))
		labels.forEachIndexed { index, label -> choices.row(label, selected = index == selected) {
			if (current()) { picked = true; action(index) }
			choices.dismiss()
			redisplay()
		} }
		choices.button(getString(android.R.string.cancel)) { choices.dismiss() }
		choices.onDismiss { if (!picked) redisplay() }
		choices.show(selected.coerceAtLeast(0))
	}
	var index = 0
	fun row(title: Int, value: String? = null, checked: Boolean? = null, action: () -> Unit) {
		val rowIndex = index++
		menu.row(getString(title), value, checked) {
			selectedFocus = rowIndex
			menu.dismiss()
			if (current()) action()
		}
	}
	row(R.string.library_filter_genre_title, draft.genre ?: any) {
		choose(R.string.library_filter_genre_title, listOf(any) + genres, genres.indexOf(draft.genre) + 1) { draft.genre = if (it == 0) null else genres[it - 1] }
	}
	row(R.string.library_filter_year_title, draft.year?.toString() ?: any) {
		choose(R.string.library_filter_year_title, listOf(any) + years.map(Int::toString), years.indexOf(draft.year) + 1) { draft.year = if (it == 0) null else years[it - 1] }
	}
	row(R.string.library_filter_rating_title, draft.minimumRating?.let { getString(R.string.library_rating_minimum, it) } ?: any) {
		choose(R.string.library_filter_rating_title, listOf(any) + (1..10).map { getString(R.string.library_rating_minimum, it) }, draft.minimumRating ?: 0) { draft.minimumRating = it.takeIf { it > 0 } }
	}
	// Switches flip in place; like every other choice they stay a draft until Apply.
	index += 2
	menu.toggle(getString(R.string.library_hide_watched), draft.isUnwatchedOnly) { draft.isUnwatchedOnly = it }
	menu.toggle(getString(R.string.library_filter_favorites_title), draft.isFavoriteOnly) { draft.isFavoriteOnly = it }
	if (allowResolution) {
		val resolutions = LibraryResolution.entries
		row(R.string.library_resolution_title, getString(draft.resolution.labelRes)) {
			choose(R.string.library_resolution_title, resolutions.map { getString(it.labelRes) }, resolutions.indexOf(draft.resolution)) { draft.resolution = resolutions[it] }
		}
		val durations = listOf(null, 30, 60, 90, 120)
		row(R.string.runtime_filter_title, draft.runtimeMinutes?.let { getString(R.string.runtime_check_under, it) } ?: any) {
			choose(R.string.runtime_filter_title, durations.map { value -> value?.let { getString(R.string.runtime_check_under, it) } ?: any }, durations.indexOf(draft.runtimeMinutes)) { draft.runtimeMinutes = durations[it] }
		}
	}
	row(R.string.library_filter_reset) {
		draft.genre = null; draft.year = null; draft.minimumRating = null; draft.isFavoriteOnly = false; draft.isUnwatchedOnly = false
		draft.resolution = LibraryResolution.ANY; draft.runtimeMinutes = null
		redisplay()
	}
	menu.button(getString(android.R.string.cancel)) { menu.dismiss() }
	menu.button(getString(R.string.runtime_filter_apply), true) {
		menu.dismiss()
		if (!current()) return@button
		val controller = adapter.runtimeController
		if (allowResolution && controller != null) controller.applyDraft(draft, onChanged, ::redisplay)
		else { adapter.setFilters(draft); onChanged() }
	}
	menu.show(focus)
}
