package org.jellyfin.androidtv.ui.itemdetail

import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.TextUnderButton
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.androidtv.ui.home.SeerrFin
import org.jellyfin.androidtv.ui.home.SeerrItem
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.android.ext.android.inject

/**
 * A show that's only partly in the library (#100): when Seerr can still get aired seasons, [button] is
 * shown and requests them. It stays hidden without SeerrFin, with the Seerr switch off, or when Seerr
 * hasn't matched the show.
 */
fun FullDetailsFragment.populateSeerrSeasonsButton(button: TextUnderButton) {
	val api by inject<ApiClient>()
	val preferences by inject<UserPreferences>()
	val series = mBaseItem ?: return
	if (!SeerrFin.available.value || !preferences[UserPreferences.seerrRows]) return
	val tmdbId = series.providerIds?.get("Tmdb")?.toIntOrNull() ?: return

	lifecycleScope.launch {
		val missing = SeerrFin.missingSeasons(api, tmdbId).orEmpty()
		if (missing.isEmpty() || !isAdded || mBaseItem?.id != series.id) return@launch
		val context = requireContext()
		button.setLabel(
			if (missing.size == 1) context.getString(R.string.seerr_request_season, missing.single())
			else context.getString(R.string.seerr_request_missing_seasons)
		)
		button.setOnClickListener {
			val seasons = if (missing.size == 1) context.getString(R.string.seerr_season, missing.single())
			else context.getString(R.string.seerr_seasons, missing.dropLast(1).joinToString(", ") + " & " + missing.last())
			val item = SeerrItem(name = series.name.orEmpty(), mediaType = "tv", mediaId = tmdbId, imageUrl = null, year = series.productionYear)
			LumaFinMenuDialog(context, viewLifecycleOwner, item.name, seasons, fitContent = true).apply {
				body(context.getString(R.string.seerr_missing_seasons_detail, seasons))
				button(context.getString(R.string.seerr_request), primary = true) {
					dismiss()
					lifecycleScope.launch {
						val ok = SeerrFin.request(api, item, missing)
						Toast.makeText(context, context.getString(if (ok) R.string.seerr_requested else R.string.seerr_request_failed, item.name), Toast.LENGTH_LONG).show()
						if (ok && isAdded) {
							button.isVisible = false
							showMoreButtonIfNeeded()
						}
					}
				}
				val cancel = button(context.getString(android.R.string.cancel)) { dismiss() }
				show()
				// Opens on Cancel, so a stray OK never sends a request
				cancel.requestFocus()
			}
		}
		button.isVisible = true
		showMoreButtonIfNeeded()
	}
}
