package org.jellyfin.androidtv.preference

import android.content.SharedPreferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.playback.AudioChoice
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceScope
import org.jellyfin.androidtv.ui.playback.PlaybackChoices
import org.jellyfin.androidtv.ui.playback.SubtitleChoice
import org.jellyfin.androidtv.ui.playback.source
import org.jellyfin.androidtv.ui.playback.stream
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.UserDto
import java.util.UUID

class PlaybackChoicePreferencesTests : FunSpec({
	fun storage(values: MutableMap<String, String>): SharedPreferences {
		val preferences = mockk<SharedPreferences>()
		val editor = mockk<SharedPreferences.Editor>()
		every { preferences.getString(any(), any()) } answers { values[firstArg()] ?: secondArg() }
		every { preferences.edit() } returns editor
		every { editor.putString(any(), any()) } answers { values[firstArg()] = secondArg(); editor }
		every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
		every { editor.apply() } returns Unit
		return preferences
	}
	fun server() = mockk<Server>().also { every { it.id } returns UUID.randomUUID() }
	fun user() = mockk<UserDto>().also { every { it.id } returns UUID.randomUUID() }

	val series = UUID.randomUUID()
	val season1 = UUID.randomUUID()
	val season2 = UUID.randomUUID()
	fun episode(season: UUID = season1) = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seriesId = series, seasonId = season)
	val japanese = stream(2, MediaStreamType.AUDIO, "jpn")
	val english = stream(1, MediaStreamType.AUDIO, "eng")
	val englishSubs = stream(3, MediaStreamType.SUBTITLE, "eng")

	fun setup(): Triple<PlaybackChoicePreferences, MutableStateFlow<UserDto?>, () -> PlaybackChoicePreferences> {
		val values = mutableMapOf<String, String>()
		val shared = storage(values)
		val servers = mockk<ServerRepository>()
		val users = mockk<UserRepository>()
		val activeUser = MutableStateFlow<UserDto?>(user())
		every { servers.currentServer } returns MutableStateFlow(server())
		every { users.currentUser } returns activeUser
		return Triple(PlaybackChoicePreferences(shared, servers, users), activeUser) { PlaybackChoicePreferences(shared, servers, users) }
	}

	test("an episode choice is remembered for the whole series and survives recreation") {
		val (prefs, _, recreate) = setup()
		val first = episode()
		prefs.scope(first) shouldBe PlaybackChoiceScope.SERIES
		prefs.recordAudio(first, japanese)
		prefs.recordSubtitle(first, null)
		val later = episode(season2)
		recreate().levels(later) shouldBe listOf(PlaybackChoices(audio = AudioChoice("jpn"), subtitle = SubtitleChoice(off = true)))
	}

	test("a movie choice is remembered only for that movie") {
		val (prefs) = setup()
		val movie = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)
		prefs.scope(movie) shouldBe PlaybackChoiceScope.TITLE
		prefs.recordAudio(movie, english)
		prefs.levels(movie).single().audio shouldBe AudioChoice("eng")
		prefs.levels(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)) shouldBe emptyList()
	}

	test("choices are isolated per user and nothing is stored while signed out") {
		val (prefs, activeUser) = setup()
		val ep = episode()
		prefs.recordAudio(ep, japanese)
		val original = activeUser.value
		activeUser.value = user()
		prefs.levels(ep) shouldBe emptyList()
		activeUser.value = null
		prefs.recordAudio(ep, english)
		prefs.levels(ep) shouldBe emptyList()
		activeUser.value = original
		prefs.levels(ep).single().audio shouldBe AudioChoice("jpn")
	}

	test("narrowing to one episode moves the choice there and stops the series using it") {
		val (prefs) = setup()
		val ep = episode()
		val sibling = episode()
		prefs.recordAudio(ep, japanese)
		prefs.moveTo(ep, PlaybackChoiceScope.TITLE)
		prefs.scope(ep) shouldBe PlaybackChoiceScope.TITLE
		prefs.recordAudio(ep, english)
		prefs.levels(ep).map { it.audio?.language } shouldBe listOf("eng")
		prefs.levels(sibling) shouldBe emptyList()
	}

	test("moving to season copies what applies and removes the other levels") {
		val (prefs) = setup()
		val ep = episode()
		prefs.recordSubtitle(ep, englishSubs)
		prefs.moveTo(ep, PlaybackChoiceScope.TITLE)
		prefs.recordAudio(ep, japanese)
		prefs.moveTo(ep, PlaybackChoiceScope.SEASON)
		prefs.scope(ep) shouldBe PlaybackChoiceScope.SEASON
		val sameSeason = episode()
		prefs.levels(sameSeason).first() shouldBe PlaybackChoices(audio = AudioChoice("jpn"), subtitle = SubtitleChoice(language = "eng"))
		prefs.levels(episode(season2)) shouldBe emptyList()
		prefs.moveTo(ep, PlaybackChoiceScope.SERIES)
		prefs.levels(episode(season2)).single().audio shouldBe AudioChoice("jpn")
	}

	test("reset forgets the title, season and series; clear all forgets everything") {
		val (prefs) = setup()
		val ep = episode()
		prefs.recordAudio(ep, japanese)
		prefs.moveTo(ep, PlaybackChoiceScope.SEASON)
		prefs.hasChoices(ep) shouldBe true
		prefs.reset(ep)
		prefs.hasChoices(ep) shouldBe false
		prefs.recordAudio(ep, japanese)
		prefs.isEmpty() shouldBe false
		prefs.clearAll()
		prefs.isEmpty() shouldBe true
	}

	test("versions are only remembered for titles with more than one") {
		val (prefs) = setup()
		val a = source(UUID.randomUUID(), "1080p", 1080)
		val b = source(UUID.randomUUID(), "4K", 2160)
		val single = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, mediaSources = listOf(a))
		prefs.recordVersion(single, a)
		prefs.hasChoices(single) shouldBe false
		val multi = single.copy(mediaSources = listOf(a, b))
		prefs.recordVersion(multi, b)
		prefs.levels(multi).single().version?.height shouldBe 2160
	}

	test("old entries are dropped beyond the limit, keeping the most recently changed") {
		val (prefs) = setup()
		val first = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)
		prefs.recordAudio(first, english)
		repeat(PlaybackChoicePreferences.MAX_ENTRIES) { prefs.recordAudio(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE), english) }
		prefs.hasChoices(first) shouldBe false
	}
})
