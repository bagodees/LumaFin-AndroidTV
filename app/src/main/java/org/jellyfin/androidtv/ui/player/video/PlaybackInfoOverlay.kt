package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.description
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.core.mediastream.mediaStream

@Composable
fun PlaybackInfoOverlay(
	playbackManager: PlaybackManager,
	modifier: Modifier = Modifier,
) {
	val entry by rememberQueueEntry(playbackManager)
	val currentEntry = entry ?: return
	val streamFlow = remember(currentEntry) { currentEntry.mediaStreamFlow }
	val mediaStream by streamFlow.collectAsState(currentEntry.mediaStream)
	val stream = mediaStream ?: return
	var diagnostics by remember(entry) { mutableStateOf(playbackManager.backend.getDiagnostics()) }
	LaunchedEffect(entry) {
		while (true) {
			diagnostics = playbackManager.backend.getDiagnostics()
			delay(1000)
		}
	}
	val context = androidx.compose.ui.platform.LocalContext.current
	val focus = remember { FocusRequester() }
	val scroll = rememberScrollState()
	val scope = rememberCoroutineScope()
	LaunchedEffect(Unit) { focus.requestFocus() }

	val videoTrack = stream.tracks.filterIsInstance<MediaStreamVideoTrack>().firstOrNull()
	val audioTrack = stream.tracks.filterIsInstance<MediaStreamAudioTrack>().firstOrNull()

	Column(
		verticalArrangement = Arrangement.spacedBy(4.dp),
		modifier = modifier
			.background(Color.Black.copy(alpha = 0.8f))
			.widthIn(max = 520.dp)
			.heightIn(max = 360.dp)
			.verticalScroll(scroll)
			.focusRequester(focus)
			.onPreviewKeyEvent { event ->
				if (event.key != Key.DirectionDown && event.key != Key.DirectionUp) false
				else {
					if (event.type == KeyEventType.KeyDown) scope.launch {
						scroll.animateScrollTo((scroll.value + if (event.key == Key.DirectionDown) 180 else -180)
							.coerceIn(0, scroll.maxValue))
					}
					true
				}
			}
			.focusable()
			.padding(16.dp)
	) {
		// Play method
		val methodStr = when (stream.conversionMethod) {
			MediaConversionMethod.None -> stringResource(R.string.playback_info_direct_play)
			MediaConversionMethod.Remux -> stringResource(R.string.playback_info_direct_stream)
			MediaConversionMethod.Transcode -> stringResource(R.string.playback_info_transcoding)
		}
		InfoText(stringResource(R.string.playback_info_play_method, methodStr))

		// Container
		InfoText(stringResource(R.string.playback_info_container, stream.container.format.uppercase()))

		// Video info
		if (videoTrack != null) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_video_title))
			InfoText(stringResource(R.string.playback_info_codec, videoTrack.codec.uppercase()))
			if (videoTrack.width > 0 && videoTrack.height > 0) {
				InfoText(stringResource(R.string.playback_info_resolution, videoTrack.width, videoTrack.height))
			}
			if (videoTrack.bitrate > 0) {
				InfoText(stringResource(R.string.playback_info_bitrate, formatBitrate(videoTrack.bitrate)))
			}
			videoTrack.videoRange?.let { InfoText(stringResource(R.string.playback_info_video_range, it)) }
		}

		// Audio info
		if (audioTrack != null) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_audio_title))
			InfoText(stringResource(R.string.playback_info_codec, audioTrack.codec.uppercase()))
			InfoText(stringResource(R.string.playback_info_channels, audioTrack.channels))
			if (audioTrack.bitrate > 0) {
				InfoText(stringResource(R.string.playback_info_bitrate, formatBitrate(audioTrack.bitrate)))
			}
		}

		// Transcoding note
		if (stream.conversionMethod == MediaConversionMethod.Transcode) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_transcoding_note))
		}
		InfoText("")
		InfoText(diagnostics.description(context))
	}
}

@Composable
private fun formatBitrate(bitrate: Int): String {
	val mbitStr = stringResource(R.string.bitrate_mbit, 0f).substringAfter("0")
	val kbitStr = stringResource(R.string.bitrate_kbit, 0f).substringAfter("0")
	return when {
		bitrate >= 1_000_000 -> "%.1f$mbitStr".format(bitrate / 1_000_000f)
		else -> "%.0f$kbitStr".format(bitrate / 1_000f)
	}
}

@Composable
private fun InfoText(text: String) {
	Text(
		text = text,
		style = TextStyle(
			color = Color.White,
			fontSize = 14.sp,
		)
	)
}
