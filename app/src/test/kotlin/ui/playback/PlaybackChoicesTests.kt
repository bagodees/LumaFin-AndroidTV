package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.UUID

internal fun stream(
	index: Int,
	type: MediaStreamType,
	language: String? = null,
	title: String? = null,
	forced: Boolean = false,
	hearingImpaired: Boolean = false,
	default: Boolean = false,
	channels: Int? = null,
	height: Int? = null,
): MediaStream = mockk(relaxed = true) {
	every { this@mockk.index } returns index
	every { this@mockk.type } returns type
	every { this@mockk.language } returns language
	every { this@mockk.title } returns title
	every { displayTitle } returns null
	every { isForced } returns forced
	every { isHearingImpaired } returns hearingImpaired
	every { isDefault } returns default
	every { this@mockk.channels } returns channels
	every { this@mockk.height } returns height
}

internal fun source(id: UUID, name: String?, height: Int?): MediaSourceInfo = mockk(relaxed = true) {
	every { this@mockk.id } returns id.toString().replace("-", "")
	every { this@mockk.name } returns name
	every { mediaStreams } returns listOfNotNull(height?.let { stream(0, MediaStreamType.VIDEO, height = it) })
}

class PlaybackChoicesTests : FunSpec({
	val audio = MediaStreamType.AUDIO
	val sub = MediaStreamType.SUBTITLE
	val m = PlaybackChoiceMatcher

	test("languages normalize across two-letter, bibliographic and terminology codes") {
		m.normalizeLanguage("de") shouldBe "deu"
		m.normalizeLanguage("ger") shouldBe "deu"
		m.normalizeLanguage("DEU") shouldBe "deu"
		m.normalizeLanguage("fre") shouldBe m.normalizeLanguage("fr")
		m.normalizeLanguage("en-US") shouldBe "eng"
		m.normalizeLanguage("und") shouldBe null
		m.normalizeLanguage(null) shouldBe null
	}

	test("audio matches language even when track order differs, and never swaps in a commentary") {
		val episode1 = listOf(stream(1, audio, "eng"), stream(2, audio, "jpn"))
		val episode2 = listOf(stream(1, audio, "jpn", title = "Commentary"), stream(2, audio, "eng"), stream(3, audio, "jpn"))
		val choice = m.audioChoice(episode1[1])!!
		m.matchAudio(episode2, choice)?.index shouldBe 3
		m.matchAudio(listOf(stream(1, audio, "eng"), stream(2, audio, "jpn", title = "Director commentary")), choice) shouldBe null
	}

	test("audio prefers the same channel count, then the default track") {
		val streams = listOf(stream(1, audio, "eng", channels = 2), stream(2, audio, "eng", channels = 6), stream(3, audio, "eng", channels = 2, default = true))
		m.matchAudio(streams, AudioChoice("eng", channels = 6))?.index shouldBe 2
		m.matchAudio(streams, AudioChoice("eng", channels = 8))?.index shouldBe 3
	}

	test("audio of unknown language is not remembered") {
		m.audioChoice(stream(1, audio, "und")) shouldBe null
		m.audioChoice(stream(1, audio, null)) shouldBe null
	}

	test("subtitles keep forced and full subtitles apart but let hearing-impaired stand in") {
		val streams = listOf(stream(3, sub, "eng", forced = true), stream(4, sub, "eng", hearingImpaired = true), stream(5, sub, "spa"))
		m.matchSubtitle(streams, SubtitleChoice(language = "eng")) shouldBe 4
		m.matchSubtitle(streams, SubtitleChoice(language = "eng", forced = true)) shouldBe 3
		m.matchSubtitle(listOf(stream(3, sub, "eng", forced = true)), SubtitleChoice(language = "eng")) shouldBe null
		m.matchSubtitle(streams, SubtitleChoice(language = "fra")) shouldBe null
	}

	test("subtitles off is always honored and remembered as off") {
		m.subtitleChoice(null) shouldBe SubtitleChoice(off = true)
		m.matchSubtitle(emptyList(), SubtitleChoice(off = true)) shouldBe PlaybackChoiceMatcher.SUBTITLES_OFF
	}

	test("versions match by name first, then by height, and need more than one version") {
		val sources = listOf(source(UUID.randomUUID(), "1080p", 1080), source(UUID.randomUUID(), "4K Director's Cut", 2160))
		m.matchVersion(sources, VersionChoice("4k director's cut", 2160)) shouldBe 1
		m.matchVersion(sources, VersionChoice("2160p HEVC", 2160)) shouldBe 1
		m.matchVersion(sources, VersionChoice("720p", 720)) shouldBe null
		m.matchVersion(sources.take(1), VersionChoice("1080p", 1080)) shouldBe null
	}

	test("the most specific level wins per field and an unmatched level falls through") {
		val streams = listOf(stream(1, audio, "eng"), stream(2, audio, "jpn"), stream(3, sub, "eng"))
		val title = PlaybackChoices(audio = AudioChoice("fra"))
		val season = PlaybackChoices(subtitle = SubtitleChoice(off = true))
		val series = PlaybackChoices(audio = AudioChoice("jpn"), subtitle = SubtitleChoice(language = "eng"))
		val levels = listOf(title, season, series)
		m.audioIndex(levels, streams) shouldBe 2
		m.subtitleIndex(levels, streams) shouldBe PlaybackChoiceMatcher.SUBTITLES_OFF
		m.subtitleIndex(listOf(series), streams) shouldBe 3
		m.audioIndex(emptyList(), streams) shouldBe null
	}

	test("episode keys cover title, season and series; versions of one title share a key") {
		val series = UUID.randomUUID()
		val season = UUID.randomUUID()
		val a = UUID.fromString("00000000-0000-0000-0000-00000000000a")
		val b = UUID.fromString("00000000-0000-0000-0000-00000000000b")
		val sources = listOf(source(b, "4K", 2160), source(a, "1080p", 1080))
		val episode = BaseItemDto(id = b, type = BaseItemKind.EPISODE, seriesId = series, seasonId = season, mediaSources = sources)
		PlaybackChoiceKeys.forItem(episode).map { it.first } shouldBe listOf(PlaybackChoiceScope.TITLE, PlaybackChoiceScope.SEASON, PlaybackChoiceScope.SERIES)
		PlaybackChoiceKeys.title(episode) shouldBe PlaybackChoiceKeys.title(episode.copy(id = a))
		PlaybackChoiceKeys.title(episode) shouldBe "title:$a"
		val movie = BaseItemDto(id = a, type = BaseItemKind.MOVIE)
		PlaybackChoiceKeys.forItem(movie).map { it.first } shouldBe listOf(PlaybackChoiceScope.TITLE)
		PlaybackChoiceKeys.title(movie) shouldBe PlaybackChoiceKeys.title(movie.copy(mediaSources = listOf(source(a, null, null))))
		PlaybackChoiceKeys.supports(BaseItemDto(id = a, type = BaseItemKind.TV_CHANNEL)) shouldBe false
	}
})
