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
 * Some Sony (MediaTek) TVs report a playback head that steps backwards while Dolby passthrough
 * starts. Media3 reads any drop as a wrap-around of the 32-bit counter and adds 2^32 frames, which
 * leaves its audio position about a day ahead. Every later audio timestamp is then rejected as
 * spurious and the video runs seconds ahead of the sound for the rest of the title.
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
		).also { Timber.i("Using guarded AudioTrack: encoding %d, %d Hz", config.encoding, config.sampleRate) }
	}

	private companion object {
		// Media3 polls the head every few milliseconds while playing, so a real jump is tiny.
		const val MAX_JUMP_SECONDS = 10L
	}
}

/**
 * Presents a playback head that only moves forward between flushes, the way Media3 expects.
 * A drop from the platform is absorbed into an offset so the head carries on from where it was;
 * a forward jump larger than [maxJumpFrames] is a bogus reading and repeats the last value.
 * Media3 only expects the head to return to zero after it flushes the track, see [reset].
 */
internal class PlaybackHeadGuard(private val maxJumpFrames: Long) {
	private var lastRaw = 0L
	private var offset = 0L

	/** Whether the last [filter] call had to correct the platform value. */
	var corrected = false
		private set

	fun filter(position: Int): Int {
		val raw = position.toLong() and 0xFFFFFFFFL
		corrected = true
		when {
			raw - lastRaw > maxJumpFrames -> return (lastRaw + offset).toInt()
			raw < lastRaw -> offset += lastRaw - raw
			else -> corrected = false
		}
		lastRaw = raw
		return (raw + offset).toInt()
	}

	fun reset() {
		lastRaw = 0
		offset = 0
		corrected = false
	}
}

private class GuardedAudioTrack(
	attributes: android.media.AudioAttributes,
	format: AudioFormat,
	bufferSize: Int,
	sessionId: Int,
	private val guard: PlaybackHeadGuard,
) : AudioTrack(attributes, format, bufferSize, MODE_STREAM, sessionId) {
	override fun getPlaybackHeadPosition(): Int {
		val raw = super.getPlaybackHeadPosition()
		val (filtered, corrected) = synchronized(guard) { guard.filter(raw) to guard.corrected }
		if (corrected) Timber.w("Corrected AudioTrack playback head %d to %d", raw, filtered)
		return filtered
	}

	override fun flush() {
		super.flush()
		synchronized(guard) { guard.reset() }
	}
}
