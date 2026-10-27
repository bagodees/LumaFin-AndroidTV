package org.jellyfin.androidtv.ui.browsing

import android.app.AlertDialog
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.applyBulkWatch
import org.jellyfin.androidtv.data.repository.individualWatchTarget
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.context.GlobalContext
import java.util.UUID

private fun loadedWatchTargets(adapter: ObjectAdapter?): List<BaseItemDto> = buildList {
	if (adapter == null) return@buildList
	for (index in 0 until adapter.size()) when (val row = adapter[index]) {
		is BaseRowItem -> row.baseItem?.takeIf(::individualWatchTarget)?.let(::add)
		is ListRow -> addAll(loadedWatchTargets(row.adapter))
	}
}.distinctBy { it.id }

private class BulkWatchHost(val root: View, val owner: LifecycleOwner, val adapter: () -> ObjectAdapter?, val refreshed: () -> Unit) {
	var active: BulkWatchDialog? = null
	fun targets() = loadedWatchTargets(adapter())
	fun show(focused: UUID?) {
		if (!root.isAttachedToWindow || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
		active?.dismiss()
		val all = targets()
		if (all.size < 2) return
		val focusedIndex = all.indexOfFirst { it.id == focused }.coerceAtLeast(0)
		val start = focusedIndex / 100 * 100
		active = BulkWatchDialog(this, all.drop(start).take(100), all.size).also { it.show() }
	}
}

fun installBulkWatchHost(root: View, owner: LifecycleOwner, adapter: () -> ObjectAdapter?, refreshed: () -> Unit) {
	val host = BulkWatchHost(root, owner, adapter, refreshed)
	root.setTag(R.id.bulk_watch_host, host)
	owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
		override fun onDestroy(owner: LifecycleOwner) { host.active?.dismiss(); root.setTag(R.id.bulk_watch_host, null) }
	})
}

private fun bulkWatchHost(view: View?): BulkWatchHost? {
	var current = view
	while (current != null) {
		(current.getTag(R.id.bulk_watch_host) as? BulkWatchHost)?.let { return it }
		current = current.parent as? View
	}
	return null
}

fun canSelectWatchTitles(view: View?) = bulkWatchHost(view)?.targets()?.size?.let { it >= 2 } == true
fun showBulkWatchSelection(view: View, focused: UUID?) { bulkWatchHost(view)?.show(focused) }
fun addBulkWatchMenuItem(menu: PopupMenu, anchor: View?, focused: UUID) {
	val host = bulkWatchHost(anchor) ?: return
	if (host.targets().size < 2) return
	menu.menu.add(0, android.view.Menu.NONE, 210, R.string.bulk_watch_title).setOnMenuItemClickListener {
		host.root.post { host.show(focused) }; true
	}
}

private class BulkWatchDialog(private val host: BulkWatchHost, private val targets: List<BaseItemDto>, private val loaded: Int) : DefaultLifecycleObserver {
	private val context = host.root.context
	private val koin = GlobalContext.get()
	private val sessions = koin.get<SessionRepository>()
	private val servers = koin.get<ServerRepository>()
	private val session = sessions.currentSession.value
	private val server = servers.currentServer.value
	private val user = koin.get<UserRepository>().currentUser.value
	private val api = server?.let { koin.get<Jellyfin>().createApi(it.address).apply { update(accessToken = session?.accessToken) } }
	private val selected = linkedSetOf<UUID>()
	private var dialog: AlertDialog? = null
	private var closed = false
	private var stop = false
	private var running = false
	private var dirty = false
	private fun current() = !closed && session != null && server != null && user?.id == session.userId &&
		sessions.currentSession.value == session && servers.currentServer.value?.address == server.address &&
		host.root.isAttachedToWindow && host.owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

	fun show() {
		if (!current()) return
		host.owner.lifecycle.addObserver(this)
		choose()
	}
	override fun onStop(owner: LifecycleOwner) { stop = true; if (!running) dismiss() }
	override fun onDestroy(owner: LifecycleOwner) { dismiss() }
	fun dismiss() {
		if (closed) return
		val refresh = dirty && current()
		closed = true
		stop = true
		dialog?.setOnDismissListener(null)
		dialog?.dismiss()
		host.owner.lifecycle.removeObserver(this)
		if (host.active === this) host.active = null
		if (refresh) host.refreshed()
	}
	private fun replace(next: AlertDialog) {
		if (!current()) { dismiss(); return }
		dialog?.setOnDismissListener(null)
		dialog?.dismiss()
		dialog = next
		next.setOnDismissListener { if (running) stop = true else dismiss() }
		next.show()
	}
	private fun label(item: BaseItemDto) = listOfNotNull(item.seriesName, item.name).distinct().joinToString(" — ").ifBlank { item.id.toString() }
	private fun choose() {
		val list = ListView(context).apply {
			choiceMode = ListView.CHOICE_MODE_MULTIPLE
			adapter = ArrayAdapter(context, android.R.layout.simple_list_item_multiple_choice, targets.map(::label))
		}
		val body = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(24, 8, 24, 8)
			addView(TextView(context).apply {
				text = context.getString(if (loaded > targets.size) R.string.bulk_watch_window else R.string.bulk_watch_scope, targets.size, loaded)
				setPadding(0, 0, 0, 12)
			})
			addView(list, LinearLayout.LayoutParams(-1, (context.resources.displayMetrics.heightPixels * .5).toInt()))
		}
		val next = AlertDialog.Builder(context).setTitle(R.string.bulk_watch_title).setView(body)
			.setPositiveButton(context.getString(R.string.bulk_watch_review, selected.size)) { _, _ -> action() }
			.setNegativeButton(android.R.string.cancel, null).create()
		replace(next)
		fun update() {
			next.getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = selected.isNotEmpty(); text = context.getString(R.string.bulk_watch_review, selected.size) }
		}
		targets.forEachIndexed { index, item -> list.setItemChecked(index, item.id in selected) }
		list.setOnItemClickListener { _, _, index, _ -> if (list.isItemChecked(index)) selected += targets[index].id else selected -= targets[index].id; update() }
		update()
		list.requestFocus()
	}
	private fun action() = replace(AlertDialog.Builder(context).setTitle(context.getString(R.string.bulk_watch_choose_action, selected.size))
		.setItems(arrayOf(context.getString(R.string.bulk_watch_watched), context.getString(R.string.bulk_watch_unwatched))) { _, index -> confirm(index == 0) }
		.setNegativeButton(R.string.bulk_watch_edit) { _, _ -> choose() }.create())
	private fun confirm(watched: Boolean) {
		val chosen = targets.filter { it.id in selected }
		if (chosen.isEmpty()) return
		val message = context.getString(if (watched) R.string.bulk_watch_confirm_watched else R.string.bulk_watch_confirm_unwatched,
			chosen.size, user?.name.orEmpty(), server?.name.orEmpty()) + "\n\n" + chosen.joinToString("\n") { label(it) }
		replace(AlertDialog.Builder(context).setTitle(if (watched) R.string.bulk_watch_watched else R.string.bulk_watch_unwatched)
			.setMessage(message).setPositiveButton(R.string.bulk_watch_apply) { _, _ -> apply(chosen, watched) }
			.setNegativeButton(android.R.string.cancel) { _, _ -> choose() }.create())
	}
	private fun apply(chosen: List<BaseItemDto>, watched: Boolean) {
		if (!current() || api == null) { dismiss(); return }
		running = true
		stop = false
		val busy = AlertDialog.Builder(context).setTitle(R.string.bulk_watch_title).setMessage(context.getString(R.string.bulk_watch_progress, chosen.size))
			.setNegativeButton(R.string.bulk_watch_stop, null).create()
		replace(busy)
		fun stopRemaining() {
			stop = true
			busy.setMessage(context.getString(R.string.bulk_watch_stopping))
			busy.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
		}
		busy.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { stopRemaining() }
		busy.setCancelable(false)
		busy.setOnKeyListener { _, key, event ->
			if (key == android.view.KeyEvent.KEYCODE_BACK) {
				if (event.action == android.view.KeyEvent.ACTION_UP) stopRemaining()
				true
			} else false
		}
		host.owner.lifecycleScope.launch {
			try {
				val result = applyBulkWatch(chosen, watched,
					getItem = { api.userLibraryApi.getItem(itemId = it, userId = session!!.userId).content },
					setPlayed = { id, value ->
						val response = if (value) api.playStateApi.markPlayedItem(itemId = id, userId = session!!.userId) else api.playStateApi.markUnplayedItem(itemId = id, userId = session!!.userId)
						check(response.content.played == value && response.content.playbackPositionTicks == 0L && (value || response.content.playCount == 0))
					},
					isCurrent = ::current, shouldStop = { stop }, mayHaveChanged = {
						dirty = true
						koin.get<DataRefreshService>().playbackReportFinished(it)
					})
				running = false
				if (!current()) { dismiss(); return@launch }
				val unfinished = result.failed + result.unconfirmed + result.notAttempted
				val message = context.getString(R.string.bulk_watch_result, result.changed.size, result.unchanged.size, result.failed.size, result.unconfirmed.size, result.notAttempted.size) +
					if (unfinished.isEmpty()) "" else "\n\n" + context.getString(R.string.bulk_watch_unfinished) + "\n" + chosen.filter { it.id in unfinished }.joinToString("\n") { label(it) }
				replace(AlertDialog.Builder(context).setTitle(R.string.bulk_watch_title).setMessage(message).setPositiveButton(android.R.string.ok, null).create())
			} catch (error: CancellationException) { running = false; dismiss(); throw error }
			catch (_: Exception) {
				running = false
				if (current()) replace(AlertDialog.Builder(context).setTitle(R.string.bulk_watch_title).setMessage(R.string.bulk_watch_error).setPositiveButton(android.R.string.ok, null).create()) else dismiss()
			}
		}
	}
}
