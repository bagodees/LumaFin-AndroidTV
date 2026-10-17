package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.repository.NoSpeedTestMedia
import org.jellyfin.androidtv.data.repository.StreamingSpeedResult
import org.jellyfin.androidtv.data.repository.StreamingSpeedTest
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.StreamingQuality
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.Jellyfin
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun SettingsStreamingSpeedScreen() {
	val sessions = koinInject<SessionRepository>()
	val servers = koinInject<ServerRepository>()
	val jellyfin = koinInject<Jellyfin>()
	val preferences = koinInject<UserPreferences>()
	val session by sessions.currentSession.collectAsState()
	val server by servers.currentServer.collectAsState()
	val scope = rememberCoroutineScope()
	val tester = remember { StreamingSpeedTest() }
	var job by remember { mutableStateOf<Job?>(null) }
	var running by remember { mutableStateOf(false) }
	var generation by remember { mutableIntStateOf(0) }
	var result by remember { mutableStateOf<StreamingSpeedResult?>(null) }
	var message by remember { mutableStateOf<Int?>(null) }
	val quality = preferences[UserPreferences.streamingQuality]
	val limit = when (quality) {
		StreamingQuality.ORIGINAL -> null
		StreamingQuality.CUSTOM -> preferences[UserPreferences.maxBitrate].toDoubleOrNull()
		else -> quality.bitrate?.div(1_000_000.0)
	}
	LaunchedEffect(session, server?.address) {
		generation++
		job?.cancel()
		running = false
		result = null
		message = null
	}
	fun number(value: Double) = String.format(Locale.getDefault(), "%.1f", value)
	SettingsColumn {
		item {
			ListSection(overlineContent = { Text(stringResource(R.string.settings_group_help).uppercase()) },
				headingContent = { Text(stringResource(R.string.speed_test_title)) },
				captionContent = { Text(stringResource(R.string.speed_test_description)) })
		}
		item {
			ListSection(headingContent = { Text(server?.name ?: stringResource(R.string.speed_test_no_session)) },
				captionContent = { Text(stringResource(R.string.speed_test_scope)) })
		}
		item {
			ListButton(headingContent = { Text(stringResource(if (running) android.R.string.cancel else R.string.speed_test_start)) },
				captionContent = { Text(stringResource(if (running) R.string.speed_test_running else R.string.speed_test_bounds)) },
				modifier = Modifier.focusKey("speed_test_action"), onClick = {
					if (running) {
						generation++
						job?.cancel()
						message = R.string.speed_test_cancelled
						running = false
					} else {
						val selectedSession = session
						val selectedServer = server
						result = null
						message = null
						if (selectedSession == null || selectedServer == null) message = R.string.speed_test_no_session
						else {
							val attempt = ++generation
							running = true
							job = scope.launch {
								try {
									val api = jellyfin.createApi(selectedServer.address).apply { update(accessToken = selectedSession.accessToken) }
									val measured = withTimeout(25_000) { tester.run(api, selectedSession.userId) }
									if (attempt == generation) result = measured
								} catch (_: TimeoutCancellationException) {
									if (attempt == generation) message = R.string.speed_test_failed
								} catch (cancelled: CancellationException) {
									throw cancelled
								} catch (_: NoSpeedTestMedia) {
									if (attempt == generation) message = R.string.speed_test_no_media
								} catch (_: Exception) {
									if (attempt == generation) message = R.string.speed_test_failed
								} finally {
									if (attempt == generation) running = false
								}
							}
						}
					}
				})
		}
		message?.let { text -> item { ListSection(headingContent = { Text(stringResource(text)) }) } }
		result?.let { completed ->
			val measurement = completed.measurement
			item {
				SpeedResultSection(headingContent = { Text(stringResource(R.string.speed_test_average, number(measurement.mbps))) },
					captionContent = { Text(stringResource(R.string.speed_test_sample, completed.title,
						number(measurement.bytes / 1048576.0), number(measurement.seconds))) })
			}
			if (measurement.shortSample) item { SpeedResultSection(headingContent = {}, captionContent = { Text(stringResource(R.string.speed_test_short)) }) }
			if (measurement.intervals.size >= 2) item {
				SpeedResultSection(headingContent = {}, captionContent = { Text(stringResource(R.string.speed_test_variation,
					number(measurement.intervals.min()), number(measurement.intervals.max()))) })
			}
			completed.bitrate?.takeIf { it > 0 }?.let { bitrate -> item {
				val required = bitrate / 1_000_000.0
				SpeedResultSection(headingContent = { Text(stringResource(R.string.speed_test_source_bitrate, number(required))) },
					captionContent = { Text(stringResource(if (measurement.mbps >= required * 1.5)
						R.string.speed_test_headroom else R.string.speed_test_limited)) })
			} }
			if (limit != null) item {
				SpeedResultSection(headingContent = {}, captionContent = {
					Text(stringResource(if (measurement.mbps >= limit * 1.5)
						R.string.speed_test_quality_headroom else R.string.speed_test_quality_limited))
				})
			}
		}
		item {
			SpeedResultSection(headingContent = { Text(stringResource(R.string.speed_test_quality)) }, captionContent = {
				Text(if (limit == null) stringResource(R.string.speed_test_original)
					else stringResource(R.string.speed_test_limit, number(limit)))
			})
		}
		item { SpeedResultSection(headingContent = {}, captionContent = { Text(stringResource(R.string.speed_test_caveat)) }) }
	}
}

@Composable
private fun SpeedResultSection(headingContent: @Composable () -> Unit, captionContent: @Composable () -> Unit) {
	var focused by remember { mutableStateOf(false) }
	ListSection(headingContent = headingContent, captionContent = captionContent,
		modifier = Modifier.background(if (focused) Color(0xFF343840) else Color.Transparent, RoundedCornerShape(16.dp))
			.onFocusChanged { focused = it.isFocused }.focusable())
}
