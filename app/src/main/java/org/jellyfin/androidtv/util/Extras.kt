package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ExtraType

internal fun hasLocalExtras(item: BaseItemDto): Boolean =
	item.type in setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES) &&
		((item.specialFeatureCount ?: 0) > 0 || (item.localTrailerCount ?: 0) > 0)

internal fun combineExtras(features: List<BaseItemDto>, trailers: List<BaseItemDto>): List<BaseItemDto> =
	(features + trailers).distinctBy { it.id }

internal fun extraTypeLabel(type: ExtraType?): Int = when (type) {
	ExtraType.TRAILER -> R.string.extra_trailer
	ExtraType.BEHIND_THE_SCENES -> R.string.extra_behind_scenes
	ExtraType.DELETED_SCENE -> R.string.extra_deleted_scene
	ExtraType.INTERVIEW -> R.string.extra_interview
	ExtraType.CLIP -> R.string.extra_clip
	ExtraType.SCENE -> R.string.extra_scene
	ExtraType.SAMPLE -> R.string.extra_sample
	ExtraType.THEME_SONG -> R.string.extra_theme_song
	ExtraType.THEME_VIDEO -> R.string.extra_theme_video
	ExtraType.FEATURETTE -> R.string.extra_featurette
	ExtraType.SHORT -> R.string.extra_short
	else -> R.string.extras_bonus_material
}

object Extras {
	@JvmStatic fun available(item: BaseItemDto) = hasLocalExtras(item)
}
