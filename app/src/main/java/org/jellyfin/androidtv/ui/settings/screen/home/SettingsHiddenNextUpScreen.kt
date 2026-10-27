package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.preference.HiddenNextUp
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.ItemFields
import org.koin.compose.koinInject
import timber.log.Timber
import java.time.ZoneId
import java.util.UUID

/**
 * Shows hidden from Next Up (#97), each with Unhide. A show watched since it was hidden says so, since
 * watching it doesn't bring it back on its own.
 */
@Composable
fun SettingsHiddenNextUpScreen() {
	val api = koinInject<ApiClient>()
	val messages = koinInject<CustomMessageRepository>()
	var hidden by remember { mutableStateOf(HiddenNextUp.shows) }
	var watchedSince by remember { mutableStateOf(emptySet<String>()) }
	LaunchedEffect(Unit) {
		val ids = hidden.keys.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
		if (ids.isEmpty()) return@LaunchedEffect
		watchedSince = runCatching {
			withContext(Dispatchers.IO) {
				api.itemsApi.getItems(ids = ids, fields = setOf(ItemFields.DATE_LAST_MEDIA_ADDED)).content.items
			}
		}.onFailure { Timber.w(it, "Failed to check hidden Next Up shows") }.getOrDefault(emptyList()).filter { series ->
			val hiddenAt = hidden[series.id.toString()]?.hiddenAt ?: return@filter false
			val lastPlayed = series.userData?.lastPlayedDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: return@filter false
			lastPlayed > hiddenAt
		}.map { it.id.toString() }.toSet()
	}
	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.home_prefs).uppercase()) },
				headingContent = { Text(stringResource(R.string.hidden_next_up)) },
				captionContent = { Text(stringResource(R.string.hidden_next_up_hint)) },
			)
		}
		if (hidden.isEmpty()) item {
			ListSection(headingContent = { Text(stringResource(R.string.hidden_next_up_none)) })
		}
		items(hidden.entries.sortedBy { it.value.name }, key = { it.key }) { (id, show) ->
			ListButton(
				headingContent = { Text(show.name) },
				captionContent = {
					Text(stringResource(if (id in watchedSince) R.string.hidden_next_up_watched_since else R.string.hidden_next_up_unhide))
				},
				onClick = {
					HiddenNextUp.unhide(id)
					hidden = HiddenNextUp.shows
					messages.pushMessage(CustomMessage.NextUpChanged)
				},
				modifier = Modifier.focusKey("unhide_next_up_$id"),
			)
		}
	}
}
