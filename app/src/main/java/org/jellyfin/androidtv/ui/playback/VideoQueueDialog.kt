package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog

/** A session-only editor shared by both video players. Dismissing cancels polling/loading. */
class VideoQueueDialog(private val context: Context, private val editor: VideoQueueEditor, private val onClosed: () -> Unit = {}) {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	private var dialog: LumaFinMenuDialog? = null
	private var actions: LumaFinMenuDialog? = null
	private var more: TextView? = null
	private var view: VideoQueueView? = null
	private var limit = 50
	private var refreshing = false
	private var selectedAfterEdit: Int? = null
	private var labels: List<String>? = null

	fun show() {
		dialog = LumaFinMenuDialog(context, null, context.getString(R.string.video_queue_title), context.getString(R.string.video_queue_loading)).apply {
			more = button(context.getString(R.string.video_queue_more)) {
				if (view?.hasMore == true) limit += 50
				scope.launch { refresh() }
			}
			button(context.getString(R.string.video_queue_close), primary = true) { dismiss() }
			onDismiss { actions?.dismiss(); actions = null; scope.cancel(); dialog = null; onClosed() }
			show(fullHeight = true)
		}
		setMoreEnabled(false)
		scope.launch { while (true) { refresh(); delay(1000) } }
	}

	private fun setMoreEnabled(enabled: Boolean) = more?.apply { isEnabled = enabled; alpha = if (enabled) 1f else 0.4f }

	private suspend fun refresh() {
		val menu = dialog ?: return
		if (refreshing || !menu.isShowing) return
		refreshing = true
		try {
			val snapshot = withTimeout(5000) { editor.snapshot(limit) }
			view = snapshot
			menu.description(context.getString(R.string.video_queue_scope) + "\n\n" +
				context.getString(R.string.video_queue_current, snapshot.currentTitle ?: context.getString(R.string.video_queue_none)) + "\n" +
				context.getString(if (snapshot.editable) R.string.video_queue_upcoming else R.string.video_queue_order_note))
			val newLabels = snapshot.upcoming.mapIndexed { index, item -> "${index + 1}. ${item.title.ifBlank { context.getString(R.string.video_queue_untitled) }}" }
			val followEdit = selectedAfterEdit
			if (newLabels != labels) {
				val firstFill = labels == null
				val focused = menu.focusedRow()
				labels = newLabels
				menu.clearRows()
				if (newLabels.isEmpty()) menu.row(context.getString(R.string.video_queue_empty), navigates = false, enabled = false) {}
				else snapshot.upcoming.forEachIndexed { position, row ->
					menu.row(newLabels[position], navigates = snapshot.editable) {
						val current = view ?: return@row
						current.upcoming.getOrNull(position)?.takeIf { current.editable }?.let { showActions(current, it, position) }
					}
				}
				when {
					followEdit != null -> menu.focusRow(followEdit)
					focused >= 0 -> menu.focusRow(focused)
					firstFill && newLabels.isNotEmpty() -> menu.focusRow(0)
				}
			} else if (followEdit != null) menu.focusRow(followEdit)
			selectedAfterEdit = null
			more?.text = context.getString(R.string.video_queue_more)
			setMoreEnabled(snapshot.hasMore)
		} catch (cancelled: CancellationException) {
			// A timeout is handled below; actual disposal must cancel the dialog's work.
			if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
			showFailure()
		} catch (_: Exception) { showFailure()
		} finally { refreshing = false }
	}

	private fun showFailure() {
		view = null
		labels = null
		dialog?.apply { description(context.getString(R.string.video_queue_failed)); clearRows() }
		more?.text = context.getString(R.string.video_queue_retry)
		setMoreEnabled(true)
	}

	private fun showActions(snapshot: VideoQueueView, row: UpcomingVideo, position: Int) {
		val commands = buildList<Pair<Int, Int?>> {
			if (position > 0) add(R.string.video_queue_earlier to row.index - 1)
			if (position < snapshot.upcoming.lastIndex) add(R.string.video_queue_later to row.index + 1)
			add(R.string.video_queue_remove to null)
		}
		actions?.dismiss()
		actions = LumaFinMenuDialog(context, null, row.title.ifBlank { context.getString(R.string.video_queue_untitled) }, fitContent = true).apply {
			commands.forEach { (label, destination) ->
				row(context.getString(label), navigates = false) {
					dismiss()
					if (snapshot.edit(row.index, destination)) {
						selectedAfterEdit = if (destination == null) position else position + destination - row.index
					} else Toast.makeText(context, R.string.video_queue_changed, Toast.LENGTH_LONG).show()
					scope.launch { refresh() }
				}
			}
			button(context.getString(android.R.string.cancel)) { dismiss() }
			onDismiss { actions = null; dialog?.focusRow(position) }
			show()
		}
	}

	fun dismiss() { dialog?.dismiss(); scope.cancel() }
}
