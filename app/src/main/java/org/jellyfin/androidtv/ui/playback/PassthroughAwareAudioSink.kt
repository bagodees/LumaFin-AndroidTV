package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import timber.log.Timber

/**
 * When the audio output's capabilities change, the player chooses its tracks again, which
 * restarts the video and audio decoders. That matters on devices like the Nvidia Shield, where
 * passthrough only becomes available after a refresh rate switch. But Sony (MediaTek) TVs report
 * a change each time the HDMI ARC output starts, about a tenth of a second into playback, which
 * cost every start about 1.6 seconds. A change is only passed on when it changes how the playing
 * format is supported, such as passthrough becoming available or going away.
 */
@OptIn(UnstableApi::class)
class PassthroughAwareAudioSink(sink: AudioSink) : ForwardingAudioSink(sink) {
	private var format: Format? = null
	private var formatSupport = AudioSink.SINK_FORMAT_UNSUPPORTED

	override fun configure(config: AudioSink.AudioSinkConfig) {
		super.configure(config)
		format = config.format
		formatSupport = getFormatSupport(config.format)
	}

	// Every event is passed on, so this needs a new override when Media3 adds one to the listener
	override fun setListener(listener: AudioSink.Listener) = super.setListener(object : AudioSink.Listener {
		override fun onPositionDiscontinuity() = listener.onPositionDiscontinuity()
		override fun onPositionAdvancing(playoutStartSystemTimeMs: Long) = listener.onPositionAdvancing(playoutStartSystemTimeMs)
		override fun onUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) =
			listener.onUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
		override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) = listener.onSkipSilenceEnabledChanged(skipSilenceEnabled)
		override fun onOffloadBufferEmptying() = listener.onOffloadBufferEmptying()
		override fun onOffloadBufferFull() = listener.onOffloadBufferFull()
		override fun onAudioSinkError(audioSinkError: Exception) = listener.onAudioSinkError(audioSinkError)
		override fun onAudioCapabilitiesChanged() {
			if (supportChanged()) listener.onAudioCapabilitiesChanged()
		}
		override fun onAudioTrackInitialized(audioTrackConfig: AudioSink.AudioTrackConfig) = listener.onAudioTrackInitialized(audioTrackConfig)
		override fun onAudioTrackReleased(audioTrackConfig: AudioSink.AudioTrackConfig) = listener.onAudioTrackReleased(audioTrackConfig)
		override fun onSilenceSkipped() = listener.onSilenceSkipped()
		override fun onAudioSessionIdChanged(audioSessionId: Int) = listener.onAudioSessionIdChanged(audioSessionId)
	})

	private fun supportChanged(): Boolean {
		val format = format ?: return true
		val support = getFormatSupport(format)
		if (support == formatSupport) {
			Timber.i("Audio output changed, %s support unchanged (%d); keeping the current tracks", format.sampleMimeType, support)
			return false
		}

		Timber.i("Audio output changed, %s support %d -> %d; choosing tracks again", format.sampleMimeType, formatSupport, support)
		formatSupport = support
		return true
	}
}
