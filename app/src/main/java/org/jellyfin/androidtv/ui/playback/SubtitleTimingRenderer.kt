package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer

/** A positive delay makes subtitles appear later without moving audio or video. */
@OptIn(UnstableApi::class)
class SubtitleTimingRenderer(renderer: Renderer, private val delayMs: () -> Long) : ForwardingRenderer(renderer) {
	override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
		super.render(positionUs - delayMs() * 1000, elapsedRealtimeUs)
	}

	override fun resetPosition(positionUs: Long, sampleStreamIsResetToKeyFrame: Boolean) {
		super.resetPosition(positionUs - delayMs() * 1000, sampleStreamIsResetToKeyFrame)
	}
}
