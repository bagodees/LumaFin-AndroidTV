package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.PlaybackChoicePreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.core.context.GlobalContext

/** What remembered choices (#3) resolve to for one title. Null fields leave the normal defaults alone. */
class RememberedPlayback(val sourceId: String?, val audioIndex: Int?, val subtitleIndex: Int?)

private val choices get() = GlobalContext.get().get<PlaybackChoicePreferences>()

/**
 * Resolves against the remembered version when [useVersion] and it exists, otherwise against the
 * item's own source.
 */
@JvmOverloads
fun rememberedPlayback(item: BaseItemDto?, useVersion: Boolean = true): RememberedPlayback? {
	if (item == null) return null
	val levels = choices.levels(item).takeIf { it.isNotEmpty() } ?: return null
	val sources = item.mediaSources.orEmpty()
	val versionIndex = if (useVersion) PlaybackChoiceMatcher.versionIndex(levels, sources) else null
	val source = versionIndex?.let(sources::get)
		?: sources.firstOrNull { it.id?.toUUIDOrNull() == item.id }
		?: sources.firstOrNull()
	val streams = source?.mediaStreams.orEmpty()
	return RememberedPlayback(
		versionIndex?.let { source?.id },
		PlaybackChoiceMatcher.audioIndex(levels, streams),
		PlaybackChoiceMatcher.subtitleIndex(levels, streams),
	)
}

/** Resolves against the streams the server actually returned for playback. */
fun rememberedTracks(item: BaseItemDto?, source: MediaSourceInfo?): RememberedPlayback? {
	if (item == null || source == null) return null
	val levels = choices.levels(item).takeIf { it.isNotEmpty() } ?: return null
	val streams = source.mediaStreams.orEmpty()
	return RememberedPlayback(null, PlaybackChoiceMatcher.audioIndex(levels, streams), PlaybackChoiceMatcher.subtitleIndex(levels, streams))
}

/** Version index to open on the details screen, or null to keep the first version. */
fun rememberedVersionIndex(item: BaseItemDto?, sources: List<MediaSourceInfo>?): Int? {
	if (item == null || sources.isNullOrEmpty()) return null
	return PlaybackChoiceMatcher.versionIndex(choices.levels(item), sources)
}

/** Only call for choices the viewer made, never for automatic or server-side selections. */
object PlaybackChoiceRecorder {
	@JvmStatic
	fun audio(item: BaseItemDto?, streams: List<MediaStream>?, index: Int) =
		choices.recordAudio(item, streams?.firstOrNull { it.index == index })

	/** [index] of -1 remembers subtitles as off. */
	@JvmStatic
	fun subtitle(item: BaseItemDto?, streams: List<MediaStream>?, index: Int) {
		if (index == PlaybackChoiceMatcher.SUBTITLES_OFF) return choices.recordSubtitle(item, null)
		val stream = streams?.firstOrNull { it.index == index } ?: return
		choices.recordSubtitle(item, stream)
	}

	@JvmStatic
	fun version(item: BaseItemDto?, source: MediaSourceInfo?) = choices.recordVersion(item, source)
}
