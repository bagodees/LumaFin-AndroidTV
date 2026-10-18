package org.jellyfin.androidtv.ui.playback

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
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

/** A session-only editor shared by both video players. Dismissing cancels polling/loading. */
class VideoQueueDialog(private val context: Context, private val editor: VideoQueueEditor, private val onClosed: () -> Unit = {}) {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	private var dialog: AlertDialog? = null
	private var actions: AlertDialog? = null
	private var view: VideoQueueView? = null
	private var limit = 50
	private var refreshing = false
	private var selectedAfterEdit: Int? = null
	private lateinit var title: TextView
	private lateinit var adapter: ArrayAdapter<String>
	private var labels = emptyList<String>()

	fun show() {
		val padding = (24 * context.resources.displayMetrics.density).toInt()
		title = TextView(context).apply { textSize = 18f; setPadding(padding, padding, padding, padding / 2); text = context.getString(R.string.video_queue_loading) }
		adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, mutableListOf<String>())
		dialog = AlertDialog.Builder(context).setCustomTitle(title)
			.setAdapter(adapter) { _, _ -> } // Replace the auto-dismiss click listener after showing.
			.setPositiveButton(R.string.video_queue_close, null)
			.setNeutralButton(R.string.video_queue_more, null).create().apply {
				setOnDismissListener { actions?.dismiss(); actions = null; scope.cancel(); dialog = null; onClosed() }
				show()
				listView.setOnItemClickListener { _, _, position, _ ->
					val snapshot = view ?: return@setOnItemClickListener
					val row = snapshot.upcoming.getOrNull(position) ?: return@setOnItemClickListener
					if (snapshot.editable) showActions(snapshot, row, position)
				}
				getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
					if (view?.hasMore == true) limit += 50
					scope.launch { refresh() }
				}
			}
		scope.launch { while (true) { refresh(); delay(1000) } }
	}

	private suspend fun refresh() {
		if (refreshing || dialog?.isShowing != true) return
		refreshing = true
		try {
			val snapshot = withTimeout(5000) { editor.snapshot(limit) }
			view = snapshot
			title.text = context.getString(R.string.video_queue_title) + "\n" + context.getString(R.string.video_queue_scope) + "\n\n" +
				context.getString(R.string.video_queue_current, snapshot.currentTitle ?: context.getString(R.string.video_queue_none)) +
				if (snapshot.editable) "\n" + context.getString(R.string.video_queue_upcoming) else "\n" + context.getString(R.string.video_queue_order_note)
			val newLabels = snapshot.upcoming.mapIndexed { index, item -> "${index + 1}. ${item.title.ifBlank { context.getString(R.string.video_queue_untitled) }}" }
				.ifEmpty { listOf(context.getString(R.string.video_queue_empty)) }
			val list = dialog?.listView ?: return
			val followEdit = selectedAfterEdit != null
			val selection = selectedAfterEdit ?: list.selectedItemPosition.coerceAtLeast(0)
			if (newLabels != labels) {
				labels = newLabels
				adapter.clear(); adapter.addAll(labels); adapter.notifyDataSetChanged()
				list.setSelection(selection.coerceIn(labels.indices))
			}
			// Distinct queue entries may have identical labels. Focus still follows an edit in that case.
			if (followEdit) list.post {
				if (dialog?.isShowing == true) { list.requestFocus(); list.setSelection(selection.coerceIn(labels.indices)) }
			}
			selectedAfterEdit = null
			dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
				text = context.getString(R.string.video_queue_more)
				isEnabled = snapshot.hasMore
			}
		} catch (cancelled: CancellationException) {
			// A timeout is handled below; actual disposal must cancel the dialog's work.
			if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
			showFailure()
		} catch (_: Exception) { showFailure()
		} finally { refreshing = false }
	}

	private fun showFailure() {
		view = null
		title.text = context.getString(R.string.video_queue_failed)
		adapter.clear(); labels = emptyList()
		dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply { text = context.getString(R.string.video_queue_retry); isEnabled = true }
	}

	private fun showActions(snapshot: VideoQueueView, row: UpcomingVideo, position: Int) {
		val commands = buildList<Pair<Int, Int?>> {
			if (position > 0) add(R.string.video_queue_earlier to row.index - 1)
			if (position < snapshot.upcoming.lastIndex) add(R.string.video_queue_later to row.index + 1)
			add(R.string.video_queue_remove to null)
		}
		actions?.dismiss()
		actions = AlertDialog.Builder(context).setTitle(row.title)
			.setItems(commands.map { context.getString(it.first) }.toTypedArray()) { _, which ->
				val destination = commands[which].second
				if (snapshot.edit(row.index, destination)) {
					selectedAfterEdit = if (destination == null) position else position + destination - row.index
				} else Toast.makeText(context, R.string.video_queue_changed, Toast.LENGTH_LONG).show()
				scope.launch { refresh() }
			}.setNegativeButton(android.R.string.cancel, null).create().apply {
				setOnDismissListener { dialog?.listView?.requestFocus() }
				show()
			}
	}

	fun dismiss() { dialog?.dismiss(); scope.cancel() }
}
