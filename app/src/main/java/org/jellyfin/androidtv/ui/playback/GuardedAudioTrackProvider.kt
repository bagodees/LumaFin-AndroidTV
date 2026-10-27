package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackProvider

/**
 * Some Sony (MediaTek) TVs briefly report a negative playback head when Dolby passthrough starts.
 * Media3 reads the head as an unsigned counter and counts that as a wrap-around, which leaves its
 * audio position about a day ahead. Every later audio timestamp is then rejected as spurious and the
 * video runs seconds ahead of the sound for the rest of the title.
 */
@OptIn(UnstableApi::class)
class GuardedAudioTrackProvider : DefaultAudioSink.AudioTrackProvider {
	private val default = DefaultAudioTrackProvider()

	override fun getAudioTrack(
		config: AudioSink.AudioTrackConfig,
		audioAttributes: AudioAttributes,
		audioSessionId: Int,
		context: Context?,
	): AudioTrack {
		// Tunneled and offloaded tracks need builder options the plain constructor cannot set.
		if (config.tunneling || config.offload) return default.getAudioTrack(config, audioAttributes, audioSessionId, context)

		return GuardedAudioTrack(
			audioAttributes.platformAudioAttributes,
			Util.getAudioFormat(config.sampleRate, config.channelConfig, config.encoding),
			config.bufferSize,
			audioSessionId,
		)
	}
}

/** A real head only goes negative after about 12 hours of continuous 48 kHz audio. */
internal fun guardPlaybackHead(position: Int): Int = position.coerceAtLeast(0)

private class GuardedAudioTrack(
	attributes: android.media.AudioAttributes,
	format: AudioFormat,
	bufferSize: Int,
	sessionId: Int,
) : AudioTrack(attributes, format, bufferSize, MODE_STREAM, sessionId) {
	override fun getPlaybackHeadPosition(): Int = guardPlaybackHead(super.getPlaybackHeadPosition())
}
