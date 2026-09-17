package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

interface HomeFragmentRow {
	// Suspends so rows that need an async prerequisite fetch (e.g. Because You Watched,
	// Genres) can be awaited by the caller in order, instead of racing other rows to
	// append themselves whenever their fetch happens to complete.
	suspend fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>)
}
