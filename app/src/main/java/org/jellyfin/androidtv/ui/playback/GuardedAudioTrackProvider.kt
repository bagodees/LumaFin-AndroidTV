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
import timber.log.Timber

/**
 * Some Sony (MediaTek) TVs briefly report a playback head near 2^31 frames when Dolby passthrough
 * starts. When the next normal value comes back, Media3 counts that drop as a wrap-around of the
 * 32-bit counter, which leaves its audio position about a day ahead. Every later audio timestamp is
 * then rejected as spurious and the video runs seconds ahead of the sound for the rest of the title.
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
			PlaybackHeadGuard(maxJumpFrames = config.sampleRate.toLong() * MAX_JUMP_SECONDS),
		)
	}

	private companion object {
		// Media3 polls the head every few milliseconds while playing, so a real jump is tiny.
		const val MAX_JUMP_SECONDS = 10L
	}
}

/**
 * Keeps the last plausible playback head. Moving back is allowed (flush and seek reset the head);
 * a forward jump larger than [maxJumpFrames] is a bogus reading and repeats the last good value.
 */
internal class PlaybackHeadGuard(private val maxJumpFrames: Long) {
	private var last = 0L

	fun filter(position: Int): Int {
		val unsigned = position.toLong() and 0xFFFFFFFFL
		if (unsigned - last > maxJumpFrames) return last.toInt()
		last = unsigned
		return position
	}
}

private class GuardedAudioTrack(
	attributes: android.media.AudioAttributes,
	format: AudioFormat,
	bufferSize: Int,
	sessionId: Int,
	private val guard: PlaybackHeadGuard,
) : AudioTrack(attributes, format, bufferSize, MODE_STREAM, sessionId) {
	init {
		Timber.i("Using guarded AudioTrack for encoding %d", format.encoding)
	}

	override fun getPlaybackHeadPosition(): Int {
		val raw = super.getPlaybackHeadPosition()
		val filtered = synchronized(guard) { guard.filter(raw) }
		if (filtered != raw) Timber.w("Ignored bogus AudioTrack playback head %d, keeping %d", raw, filtered)
		return filtered
	}
}
