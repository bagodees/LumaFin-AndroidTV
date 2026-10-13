package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import java.util.Locale

internal fun readableTranscodeReason(reason: String): String = when (reason) {
	"DirectPlayError" -> "The device could not play the original file"
	"ContainerNotSupported" -> "The file container is not supported"
	"VideoCodecNotSupported" -> "The video format is not supported"
	"AudioCodecNotSupported" -> "The audio format is not supported"
	"SubtitleCodecNotSupported" -> "The subtitle format is not supported"
	"VideoBitrateNotSupported", "ContainerBitrateExceedsLimit" -> "The video bitrate exceeds the playback limit"
	"AudioBitrateNotSupported" -> "The audio bitrate exceeds the playback limit"
	"AudioChannelsNotSupported" -> "The audio channel count is not supported"
	"VideoResolutionNotSupported" -> "The video resolution is not supported"
	"VideoProfileNotSupported" -> "The video profile is not supported"
	"VideoLevelNotSupported" -> "The video level is not supported"
	"VideoRangeTypeNotSupported" -> "The video dynamic range is not supported"
	"VideoBitDepthNotSupported" -> "The video bit depth is not supported"
	else -> reason.replace(Regex("([a-z])([A-Z])"), "$1 $2")
}

private fun codecName(mime: String?, fallback: String?): String = when (mime) {
	"video/avc" -> "H.264"
	"video/hevc" -> "HEVC"
	"video/av01" -> "AV1"
	"audio/mp4a-latm" -> "AAC"
	"audio/ac3" -> "AC3"
	"audio/eac3", "audio/eac3-joc" -> "EAC3"
	else -> (mime?.substringAfter('/') ?: fallback ?: "Unknown").uppercase(Locale.ROOT)
}

@OptIn(UnstableApi::class)
fun PlaybackController.playbackInformation(context: Context): String {
	val stream = currentStreamInfo ?: return context.getString(R.string.legacy_playback_info_unavailable)
	val source = currentMediaSource
	val sourceVideo = source?.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
	val sourceAudio = source?.mediaStreams?.firstOrNull { it.type == MediaStreamType.AUDIO && it.index == audioStreamIndex }
	val player = mVideoManager?.mExoPlayer
	val video = player?.videoFormat
	val audio = player?.audioFormat
	val subtitle = source?.mediaStreams?.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.index == subtitleStreamIndex }
	val method = when (stream.playMethod) {
		PlayMethod.DIRECT_PLAY -> context.getString(R.string.legacy_playback_info_direct_play)
		PlayMethod.DIRECT_STREAM -> context.getString(R.string.legacy_playback_info_direct_stream)
		PlayMethod.TRANSCODE -> context.getString(R.string.legacy_playback_info_transcoding)
	}
	val explanation = when (stream.playMethod) {
		PlayMethod.DIRECT_PLAY -> context.getString(R.string.legacy_playback_info_direct_play_description)
		PlayMethod.DIRECT_STREAM -> context.getString(R.string.legacy_playback_info_direct_stream_description)
		PlayMethod.TRANSCODE -> context.getString(R.string.legacy_playback_info_transcoding_description)
	}
	val reasons = Uri.parse(stream.mediaUrl.orEmpty()).getQueryParameter("TranscodeReasons")
		?.split(',')?.filter { it.isNotBlank() }?.distinct().orEmpty()
	fun resolution(width: Int?, height: Int?) = if (width != null && height != null && width > 0 && height > 0) "$width × $height" else context.getString(R.string.legacy_playback_info_unknown)
	return buildString {
		appendLine(method)
		appendLine(explanation)
		if (stream.playMethod != PlayMethod.DIRECT_PLAY) {
			appendLine()
			appendLine(context.getString(R.string.legacy_playback_info_reason))
			if (reasons.isEmpty()) appendLine(context.getString(R.string.legacy_playback_info_reason_unknown))
			else reasons.forEach { appendLine("• ${readableTranscodeReason(it)}") }
		}
		appendLine()
		appendLine(context.getString(R.string.legacy_playback_info_playing))
		appendLine("${context.getString(R.string.legacy_playback_info_video)}: ${codecName(video?.sampleMimeType, null)} · ${resolution(video?.width, video?.height)}")
		val frameRate = video?.frameRate?.takeIf { it > 0 }
		if (frameRate != null) appendLine(context.getString(R.string.legacy_playback_info_fps, frameRate))
		val channels = audio?.channelCount?.takeIf { it > 0 }?.toString() ?: context.getString(R.string.legacy_playback_info_unknown)
		appendLine(context.getString(R.string.legacy_playback_info_audio, codecName(audio?.sampleMimeType, null), channels))
		appendLine(context.getString(R.string.legacy_playback_info_subtitles, subtitle?.displayTitle ?: context.getString(R.string.lbl_none)))
		appendLine()
		appendLine(context.getString(R.string.legacy_playback_info_original))
		appendLine("${context.getString(R.string.legacy_playback_info_video)}: ${codecName(null, sourceVideo?.codec)} · ${resolution(sourceVideo?.width, sourceVideo?.height)}")
		if (sourceAudio != null) appendLine(context.getString(R.string.legacy_playback_info_original_audio, codecName(null, sourceAudio.codec)))
		val bitrate = source?.bitrate?.takeIf { it > 0 }
		if (bitrate != null) appendLine(context.getString(R.string.legacy_playback_info_bitrate, bitrate / 1_000_000.0))
	}.trim()
}
