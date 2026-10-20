package org.jellyfin.androidtv.ui.itemdetail

import android.content.DialogInterface
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import java.util.function.BiConsumer

/** Shared details menu, with selection kept separate from remote focus. */
internal class DetailsMenu(private val anchor: View, private val title: String, private val description: String? = null) {
	private data class Entry(val label: String, val selected: Boolean?, val navigates: Boolean, val action: () -> Unit)
	private val entries = mutableListOf<Entry>()
	fun item(label: String, selected: Boolean? = null, navigates: Boolean = false, action: () -> Unit) {
		entries += Entry(label, selected, navigates, action)
	}
	fun showIfNotEmpty(): LumaFinMenuDialog? {
		val owner = anchor.findViewTreeLifecycleOwner() ?: return null
		if (entries.isEmpty() || !anchor.isAttachedToWindow || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return null
		return LumaFinMenuDialog(anchor.context, owner, title, description, fitContent = true).apply {
			entries.forEach { entry -> row(entry.label, selected = entry.selected, navigates = entry.navigates) {
				dismiss()
				if (anchor.isAttachedToWindow && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) entry.action()
			} }
			button(anchor.context.getString(android.R.string.cancel)) { dismiss() }
			onDismiss { if (anchor.isAttachedToWindow) anchor.requestFocus() }
			show(entries.indexOfFirst { it.selected == true }.coerceAtLeast(0))
		}
	}
}

/** Java bridge retains the existing version selection callback and its asynchronous completion. */
fun showDetailsVersions(anchor: View, labels: Array<String>, selected: Int, onSelected: BiConsumer<Int, DialogInterface>) {
	val owner = anchor.findViewTreeLifecycleOwner() ?: return
	val menu = LumaFinMenuDialog(anchor.context, owner, anchor.context.getString(R.string.select_version), fitContent = true)
	labels.forEachIndexed { index, label -> menu.row(label, selected = index == selected) { onSelected.accept(index, menu) } }
	menu.button(anchor.context.getString(android.R.string.cancel)) { menu.dismiss() }
	menu.onDismiss { if (anchor.isAttachedToWindow) anchor.requestFocus() }
	menu.show(selected)
}
