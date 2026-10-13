package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer

/** Keep the audio clock intact and shift video presentation relative to it. Positive means later audio. */
@OptIn(UnstableApi::class)
class AudioTimingRenderer(renderer: Renderer, private val audioDelayMs: () -> Long) : ForwardingRenderer(renderer) {
	override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
		super.render(positionUs + audioDelayMs() * 1000, elapsedRealtimeUs)
	}

	override fun resetPosition(positionUs: Long, sampleStreamIsResetToKeyFrame: Boolean) {
		super.resetPosition(positionUs + audioDelayMs() * 1000, sampleStreamIsResetToKeyFrame)
	}
}
