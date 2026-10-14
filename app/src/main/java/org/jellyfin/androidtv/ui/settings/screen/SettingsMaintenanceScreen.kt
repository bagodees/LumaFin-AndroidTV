package org.jellyfin.androidtv.ui.settings.screen

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun SettingsMaintenanceScreen() {
	val imageLoader = koinInject<ImageLoader>()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	var size by remember { mutableLongStateOf(imageLoader.diskCache?.size ?: 0L) }
	var clearing by remember { mutableStateOf(false) }
	var result by remember { mutableStateOf<Int?>(null) }
	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.maintenance_title)) },
			)
		}
		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.artwork_cache_size, Formatter.formatFileSize(context, size))) },
				captionContent = { Text(stringResource(R.string.artwork_clear_description)) },
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(if (clearing) R.string.artwork_clearing else R.string.artwork_clear_title)) },
				captionContent = result?.let { message -> { Text(stringResource(message)) } },
				modifier = Modifier.focusKey("clear_artwork"),
				onClick = {
					if (!clearing) {
						clearing = true
						result = null
						scope.launch {
							try {
								// Finish an initiated clear even if the user leaves this screen.
								withContext(NonCancellable + Dispatchers.IO) {
									imageLoader.memoryCache?.clear()
									imageLoader.diskCache?.clear()
								}
								result = R.string.artwork_cleared
							} catch (cancelled: CancellationException) {
								throw cancelled
							} catch (error: Exception) {
								Timber.e(error, "Unable to clear artwork cache")
								result = R.string.artwork_clear_failed
							} finally {
								size = imageLoader.diskCache?.size ?: 0L
								clearing = false
							}
						}
					}
				},
			)
		}
	}
}
