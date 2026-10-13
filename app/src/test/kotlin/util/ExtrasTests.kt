package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ExtraType
import java.util.UUID

class ExtrasTests : FunSpec({
	test("extras action is limited to movies and series with server-reported local material") {
		val movie = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, specialFeatureCount = 1)
		hasLocalExtras(movie) shouldBe true
		hasLocalExtras(movie.copy(type = BaseItemKind.SERIES, specialFeatureCount = 0, localTrailerCount = 1)) shouldBe true
		hasLocalExtras(movie.copy(specialFeatureCount = null)) shouldBe false
		hasLocalExtras(movie.copy(type = BaseItemKind.EPISODE)) shouldBe false
	}
	test("duplicates across special features and trailers appear only once") {
		val extra = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TRAILER, name = "Trailer")
		val interview = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.VIDEO, name = "Interview")
		combineExtras(listOf(interview, extra), listOf(extra)) shouldBe listOf(interview, extra)
		combineExtras(emptyList(), emptyList()) shouldBe emptyList()
	}
	test("known bonus types have readable labels and unknown types have a neutral fallback") {
		extraTypeLabel(ExtraType.DELETED_SCENE) shouldBe R.string.extra_deleted_scene
		extraTypeLabel(ExtraType.BEHIND_THE_SCENES) shouldBe R.string.extra_behind_scenes
		extraTypeLabel(ExtraType.TRAILER) shouldBe R.string.extra_trailer
		extraTypeLabel(null) shouldBe R.string.extras_bonus_material
		extraTypeLabel(ExtraType.UNKNOWN) shouldBe R.string.extras_bonus_material
	}
})
