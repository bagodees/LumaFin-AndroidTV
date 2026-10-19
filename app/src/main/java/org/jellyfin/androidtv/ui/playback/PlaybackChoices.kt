package org.jellyfin.androidtv.ui.playback

import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.util.Locale

/**
 * Remembered audio, subtitle and version choices (#3). Choices describe a track by language and
 * type rather than by stream index, because episodes often order their tracks differently.
 */
@Serializable
data class AudioChoice(val language: String, val commentary: Boolean = false, val channels: Int? = null)

@Serializable
data class SubtitleChoice(
	val off: Boolean = false,
	val language: String? = null,
	val forced: Boolean = false,
	val hearingImpaired: Boolean = false,
)

@Serializable
data class VersionChoice(val name: String? = null, val height: Int? = null)

@Serializable
data class PlaybackChoices(
	val audio: AudioChoice? = null,
	val subtitle: SubtitleChoice? = null,
	val version: VersionChoice? = null,
) {
	val isEmpty get() = audio == null && subtitle == null && version == null

	/** Field-wise fallback: values here win, missing fields come from [other]. */
	infix fun over(other: PlaybackChoices?) = if (other == null) this else PlaybackChoices(
		audio ?: other.audio,
		subtitle ?: other.subtitle,
		version ?: other.version,
	)
}

/** Where an automatic choice is saved. Episodes default to their series, movies to themselves. */
enum class PlaybackChoiceScope { TITLE, SEASON, SERIES }

object PlaybackChoiceMatcher {
	const val SUBTITLES_OFF = -1

	// ISO 639-2 bibliographic codes and their terminology equivalents used by Locale.
	private val bibliographic = mapOf(
		"alb" to "sqi", "arm" to "hye", "baq" to "eus", "bur" to "mya", "chi" to "zho", "cze" to "ces",
		"dut" to "nld", "fre" to "fra", "geo" to "kat", "ger" to "deu", "gre" to "ell", "ice" to "isl",
		"mac" to "mkd", "mao" to "mri", "may" to "msa", "per" to "fas", "rum" to "ron", "slo" to "slk",
		"tib" to "bod", "wel" to "cym",
	)

	fun normalizeLanguage(language: String?): String? {
		val code = language?.trim()?.lowercase(Locale.ROOT)?.substringBefore('-')?.substringBefore('_')
		if (code.isNullOrEmpty() || code == "und" || code == "unk" || code == "mis" || code == "zxx") return null
		if (code.length == 2) return runCatching { Locale.forLanguageTag(code).isO3Language }.getOrNull()?.takeIf { it.isNotEmpty() }
		return bibliographic[code] ?: code
	}

	private fun isCommentary(stream: MediaStream) =
		listOfNotNull(stream.title, stream.displayTitle).any { it.contains("commentary", ignoreCase = true) }

	fun audioChoice(stream: MediaStream): AudioChoice? =
		normalizeLanguage(stream.language)?.let { AudioChoice(it, isCommentary(stream), stream.channels) }

	/** Null when the subtitle's language is unknown, since it could not be matched on another title. */
	fun subtitleChoice(stream: MediaStream?): SubtitleChoice? {
		if (stream == null) return SubtitleChoice(off = true)
		val language = normalizeLanguage(stream.language) ?: return null
		return SubtitleChoice(language = language, forced = stream.isForced, hearingImpaired = stream.isHearingImpaired)
	}

	fun versionChoice(source: MediaSourceInfo): VersionChoice? {
		val height = source.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }?.height
		val name = source.name?.trim()?.takeIf { it.isNotEmpty() }
		return if (name == null && height == null) null else VersionChoice(name, height)
	}

	/** Same language and commentary type; never substitutes a commentary for the main track or vice versa. */
	fun matchAudio(streams: List<MediaStream>, choice: AudioChoice?): MediaStream? {
		if (choice == null) return null
		val language = normalizeLanguage(choice.language) ?: return null
		return streams
			.filter { it.type == MediaStreamType.AUDIO && normalizeLanguage(it.language) == language && isCommentary(it) == choice.commentary }
			.sortedWith(compareBy<MediaStream>({ choice.channels != null && it.channels != choice.channels }, { !it.isDefault }, { it.index }))
			.firstOrNull()
	}

	/**
	 * Returns a stream index, [SUBTITLES_OFF], or null when nothing suitable exists. Forced-only and
	 * full subtitles are never swapped; hearing-impaired and regular subtitles may stand in for each other.
	 */
	fun matchSubtitle(streams: List<MediaStream>, choice: SubtitleChoice?): Int? {
		if (choice == null) return null
		if (choice.off) return SUBTITLES_OFF
		val language = normalizeLanguage(choice.language) ?: return null
		return streams
			.filter { it.type == MediaStreamType.SUBTITLE && normalizeLanguage(it.language) == language && it.isForced == choice.forced }
			.sortedWith(compareBy<MediaStream>({ it.isHearingImpaired != choice.hearingImpaired }, { !it.isDefault }, { it.index }))
			.firstOrNull()?.index
	}

	/** Index in [sources] of the remembered version: same name first, then same video height. */
	fun matchVersion(sources: List<MediaSourceInfo>, choice: VersionChoice?): Int? {
		if (choice == null || sources.size < 2) return null
		choice.name?.let { name -> sources.indexOfFirst { it.name?.trim().equals(name, ignoreCase = true) }.takeIf { it >= 0 }?.let { return it } }
		choice.height?.let { height ->
			sources.indexOfFirst { source -> source.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }?.height == height }
				.takeIf { it >= 0 }?.let { return it }
		}
		return null
	}

	/** Most specific level first. A level whose choice no longer matches falls through to the next. */
	fun <T : Any> resolve(levels: List<PlaybackChoices>, pick: (PlaybackChoices) -> T?, match: (T) -> Int?): Int? =
		levels.firstNotNullOfOrNull { level -> pick(level)?.let(match) }

	fun audioIndex(levels: List<PlaybackChoices>, streams: List<MediaStream>) =
		resolve(levels, PlaybackChoices::audio) { matchAudio(streams, it)?.index }

	fun subtitleIndex(levels: List<PlaybackChoices>, streams: List<MediaStream>) =
		resolve(levels, PlaybackChoices::subtitle) { matchSubtitle(streams, it) }

	fun versionIndex(levels: List<PlaybackChoices>, sources: List<MediaSourceInfo>) =
		resolve(levels, PlaybackChoices::version) { matchVersion(sources, it) }
}

/** Keys for the levels that apply to [item], most specific first. */
object PlaybackChoiceKeys {
	/**
	 * Versions of one title are separate items sharing the same media sources, so the title key uses
	 * the lowest media source ID rather than the ID of whichever version happens to be open.
	 */
	fun title(item: BaseItemDto): String {
		// Source IDs are undashed UUIDs; normalize so a title fetched without sources keys the same.
		val id = item.mediaSources?.mapNotNull { it.id?.toUUIDOrNull()?.toString() }?.minOrNull() ?: item.id.toString()
		return "title:$id"
	}

	fun season(item: BaseItemDto) = item.seasonId?.takeIf { item.type == BaseItemKind.EPISODE }?.let { "season:$it" }
	fun series(item: BaseItemDto) = item.seriesId?.takeIf { item.type == BaseItemKind.EPISODE }?.let { "series:$it" }

	fun forItem(item: BaseItemDto): List<Pair<PlaybackChoiceScope, String>> = listOfNotNull(
		PlaybackChoiceScope.TITLE to title(item),
		season(item)?.let { PlaybackChoiceScope.SEASON to it },
		series(item)?.let { PlaybackChoiceScope.SERIES to it },
	)

	fun supports(item: BaseItemDto?) = item?.type == BaseItemKind.MOVIE || item?.type == BaseItemKind.EPISODE
}
