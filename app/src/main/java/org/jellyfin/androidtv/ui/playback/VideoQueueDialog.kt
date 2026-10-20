package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
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
	private var close: TextView? = null
	private var view: VideoQueueView? = null
	private var limit = 50
	private var refreshing = false
	private var selectedAfterEdit: Int? = null
	private var labels: List<String>? = null

	fun show() {
		dialog = LumaFinMenuDialog(context, null, context.getString(R.string.video_queue_title), context.getString(R.string.video_queue_loading), fitContent = true).apply {
			more = button(context.getString(R.string.video_queue_more)) {
				if (view?.hasMore == true) limit += 50
				scope.launch { refresh() }
			}
			more?.isVisible = false
			close = button(context.getString(R.string.video_queue_close), primary = true) { dismiss() }
			onDismiss { actions?.dismiss(); actions = null; scope.cancel(); dialog = null; onClosed() }
			show()
		}
		close?.requestFocus()
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
			val previousTitle = view?.currentTitle
			val previousEditable = view?.editable
			view = snapshot
			menu.description(context.getString(R.string.video_queue_scope) + "\n\n" +
				context.getString(R.string.video_queue_current, snapshot.currentTitle ?: context.getString(R.string.video_queue_none)) + "\n" +
				if (snapshot.upcoming.isEmpty()) "" else context.getString(if (snapshot.editable) R.string.video_queue_upcoming else R.string.video_queue_order_note))
			val newLabels = snapshot.upcoming.mapIndexed { index, item -> "${index + 1}. ${item.title.ifBlank { context.getString(R.string.video_queue_untitled) }}" }
			val layoutChanged = newLabels != labels || previousTitle != snapshot.currentTitle || previousEditable != snapshot.editable
			val followEdit = selectedAfterEdit
			if (newLabels != labels) {
				val firstFill = labels == null
				val focused = menu.focusedRow()
				labels = newLabels
				menu.clearRows()
				if (newLabels.isEmpty()) menu.emptyState(context.getString(R.string.video_queue_empty), context.getString(R.string.video_queue_empty_help))
				else snapshot.upcoming.forEachIndexed { position, row ->
					menu.row(newLabels[position], navigates = snapshot.editable) {
						val current = view ?: return@row
						current.upcoming.getOrNull(position)?.takeIf { current.editable }?.let { showActions(current, it, position) }
					}
				}
				when {
					newLabels.isEmpty() -> close?.requestFocus()
					followEdit != null -> menu.focusRow(followEdit)
					focused >= 0 -> menu.focusRow(focused)
					firstFill && newLabels.isNotEmpty() -> menu.focusRow(0)
				}
			} else if (followEdit != null) menu.focusRow(followEdit)
			selectedAfterEdit = null
			more?.text = context.getString(R.string.video_queue_more)
			more?.isVisible = snapshot.hasMore
			setMoreEnabled(snapshot.hasMore)
			// Full height is useful for a queue; a zero-item state should fit its message.
			if (layoutChanged) menu.updateSize(fitToContents = newLabels.isEmpty(), fullHeight = newLabels.isNotEmpty())
		} catch (cancelled: CancellationException) {
			// A timeout is handled below; actual disposal must cancel the dialog's work.
			if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
			showFailure()
		} catch (_: Exception) { showFailure()
		} finally { refreshing = false }
	}

	private fun showFailure() {
		val firstFailure = labels != null || view != null
		view = null
		labels = null
		dialog?.apply { description(context.getString(R.string.video_queue_failed)); clearRows() }
		more?.text = context.getString(R.string.video_queue_retry)
		more?.isVisible = true
		setMoreEnabled(true)
		dialog?.updateSize(fitToContents = true)
		if (firstFailure || close?.hasFocus() == true) more?.requestFocus()
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
