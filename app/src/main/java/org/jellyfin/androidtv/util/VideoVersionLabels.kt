package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType

internal fun MediaSourceInfo.versionVideoDetails(): String {
	val video = mediaStreams.orEmpty().firstOrNull { it.type == MediaStreamType.VIDEO } ?: return ""
	val width = video.width ?: 0
	val height = video.height ?: 0
	val resolution = when {
		width >= 7000 || height >= 4000 -> "8K"
		width >= 3800 || height >= 2100 -> "4K"
		width >= 1900 || height >= 1080 -> "1080p"
		width >= 1200 || height >= 720 -> "720p"
		width > 0 && height > 0 -> "$width × $height"
		else -> null
	}
	val range = when (video.videoRangeType) {
		VideoRangeType.DOVI_INVALID, VideoRangeType.UNKNOWN, null -> when (video.videoRange) {
			VideoRange.HDR -> "HDR"
			VideoRange.SDR -> "SDR"
			else -> null
		}
		VideoRangeType.DOVI -> "Dolby Vision"
		VideoRangeType.DOVI_WITH_HDR10 -> "Dolby Vision / HDR10"
		VideoRangeType.DOVI_WITH_HLG -> "Dolby Vision / HLG"
		VideoRangeType.DOVI_WITH_SDR -> "Dolby Vision / SDR"
		VideoRangeType.DOVI_WITH_EL -> "Dolby Vision"
		VideoRangeType.DOVI_WITH_HDR10_PLUS, VideoRangeType.DOVI_WITH_ELHDR10_PLUS -> "Dolby Vision / HDR10+"
		VideoRangeType.HDR10_PLUS -> "HDR10+"
		else -> video.videoRangeType?.name
	}
	return listOfNotNull(resolution, range).joinToString(" · ")
}

object VideoVersionLabels {
	@JvmStatic
	fun labels(context: Context, sources: List<MediaSourceInfo>): Array<String> {
		val names = sources.mapIndexed { index, source ->
			source.name?.trim()?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.video_version_number, index + 1)
		}
		val counts = names.groupingBy { it }.eachCount()
		return sources.mapIndexed { index, source ->
			val name = names[index].let {
				if (counts.getValue(it) > 1) "$it (${index + 1})" else it
			}
			val audioStreams = source.mediaStreams.orEmpty().filter { it.type == MediaStreamType.AUDIO }
			val audio = audioStreams.firstOrNull { it.index == source.defaultAudioStreamIndex }
				?: audioStreams.firstOrNull { it.isDefault } ?: audioStreams.firstOrNull()
			val audioLabel = audio?.let { audioStreams.trackLabels(context)[it.index] }.orEmpty()
			val details = listOf(source.versionVideoDetails(), audioLabel).filter { it.isNotBlank() }.joinToString(" · ")
			"$name\n${details.ifBlank { context.getString(R.string.video_version_details_unknown) }}"
		}.toTypedArray()
	}
}
