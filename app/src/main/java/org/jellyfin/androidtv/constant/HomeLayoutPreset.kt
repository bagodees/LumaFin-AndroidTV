package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R

enum class HomeLayoutPreset(
	val nameRes: Int,
	val descriptionRes: Int,
	val sections: List<HomeSectionType>,
) {
	MOVIES_FIRST(
		R.string.home_preset_movies,
		R.string.home_preset_movies_description,
		listOf(
			HomeSectionType.RESUME,
			HomeSectionType.LATEST_MOVIES,
			HomeSectionType.RECENTLY_ADDED_MOVIES,
			HomeSectionType.COLLECTIONS,
			HomeSectionType.GENRES,
			HomeSectionType.FAVORITES,
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_SHOWS,
		),
	),
	TV_FIRST(
		R.string.home_preset_tv,
		R.string.home_preset_tv_description,
		listOf(
			HomeSectionType.RESUME,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_SHOWS,
			HomeSectionType.RECENTLY_ADDED_SHOWS,
			HomeSectionType.FAVORITES,
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.LATEST_MOVIES,
			HomeSectionType.COLLECTIONS,
			HomeSectionType.GENRES,
		),
	),
	MIXED_LIBRARY(
		R.string.home_preset_mixed,
		R.string.home_preset_mixed_description,
		listOf(
			HomeSectionType.RESUME,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_MOVIES,
			HomeSectionType.LATEST_SHOWS,
			HomeSectionType.BECAUSE_YOU_WATCHED,
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.WATCH_AGAIN,
			HomeSectionType.COLLECTIONS,
			HomeSectionType.GENRES,
		),
	),
}
