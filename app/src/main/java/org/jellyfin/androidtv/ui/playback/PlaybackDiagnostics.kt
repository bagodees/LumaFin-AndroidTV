package org.jellyfin.androidtv.ui.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import org.jellyfin.androidtv.R
import org.jellyfin.playback.core.model.PlaybackDiagnostics

@OptIn(UnstableApi::class)
internal fun PlaybackController.diagnostics(): PlaybackDiagnostics {
	val manager = mVideoManager ?: return PlaybackDiagnostics()
	val player = manager.mExoPlayer ?: return PlaybackDiagnostics()
	val policy = manager.configuredBufferLength
	val counters = player.videoDecoderCounters
	counters?.ensureUpdated()
	return PlaybackDiagnostics(
		bufferedDurationMs = player.totalBufferedDuration.takeIf { it >= 0 },
		droppedVideoFrames = counters?.droppedBufferCount,
		renderedVideoFrames = counters?.renderedOutputBufferCount,
		buffering = player.playbackState == Player.STATE_BUFFERING,
		minBufferMs = policy.minBufferDuration?.inWholeMilliseconds ?: DefaultLoadControl.DEFAULT_MIN_BUFFER_MS.toLong(),
		maxBufferMs = policy.maxBufferDuration?.inWholeMilliseconds ?: DefaultLoadControl.DEFAULT_MAX_BUFFER_MS.toLong(),
		startupBufferMs = policy.bufferForPlaybackDuration?.inWholeMilliseconds ?: DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS.toLong(),
		rebufferMs = policy.bufferForPlaybackAfterRebufferDuration?.inWholeMilliseconds
			?: DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS.toLong(),
	)
}

internal fun diagnosticSeconds(milliseconds: Long?): Double? = milliseconds?.takeIf { it >= 0 }?.div(1000.0)

fun PlaybackDiagnostics.description(context: Context): String {
	val unavailable = context.getString(R.string.diagnostics_unavailable)
	fun seconds(value: Long?) = diagnosticSeconds(value)?.let { context.getString(R.string.diagnostics_seconds, it) } ?: unavailable
	return buildString {
		appendLine(context.getString(R.string.diagnostics_title))
		appendLine(context.getString(R.string.diagnostics_buffered, seconds(bufferedDurationMs)))
		appendLine(context.getString(R.string.diagnostics_dropped, droppedVideoFrames?.toString() ?: unavailable))
		appendLine(context.getString(R.string.diagnostics_rendered, renderedVideoFrames?.toString() ?: unavailable))
		appendLine(context.getString(R.string.diagnostics_buffering, when (buffering) {
			true -> context.getString(R.string.diagnostics_yes)
			false -> context.getString(R.string.diagnostics_no)
			null -> unavailable
		}))
		appendLine(context.getString(R.string.diagnostics_policy, seconds(minBufferMs), seconds(maxBufferMs)))
		appendLine(context.getString(R.string.diagnostics_startup, seconds(startupBufferMs), seconds(rebufferMs)))
		appendLine(context.getString(R.string.diagnostics_note))
		append(context.getString(R.string.diagnostics_speed_hint))
	}
}
