package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * All possible homesections, "synced" with jellyfin-web.
 *
 * https://github.com/jellyfin/jellyfin-web/blob/master/src/components/homesections/homesections.js
 */
enum class HomeSectionType(
	override val serializedName: String,
	override val nameRes: Int,
) : PreferenceEnum {
	LATEST_MEDIA("latestmedia", R.string.home_section_latest_media),
	LIBRARY_TILES_SMALL("smalllibrarytiles", R.string.home_section_library),
	LIBRARY_BUTTONS("librarybuttons", R.string.home_section_library_small),
	RESUME("resume", R.string.home_section_resume),
	RESUME_AUDIO("resumeaudio", R.string.home_section_resume_audio),
	RESUME_BOOK("resumebook", R.string.home_section_resume_book),
	ACTIVE_RECORDINGS("activerecordings", R.string.home_section_active_recordings),
	NEXT_UP("nextup", R.string.home_section_next_up),
	LIVE_TV("livetv", R.string.home_section_livetv),
	LATEST_MOVIES("latestmovies", R.string.home_section_latest_movies),
	LATEST_SHOWS("latestshows", R.string.home_section_latest_shows),
	BECAUSE_YOU_WATCHED("becauseyouwatched", R.string.home_section_because_you_watched),
	COLLECTIONS("collections", R.string.home_section_collections),
	WATCH_AGAIN("watchagain", R.string.home_section_watch_again),
	GENRES("genres", R.string.home_section_genres),
	CONTINUE_WATCHING_NEXT_UP("continuewatchingnextup", R.string.home_section_continue_watching_next_up),
	FAVORITES("favorites", R.string.home_section_favorites),
	RECENTLY_ADDED_MOVIES("recentlyaddedmovies", R.string.home_section_recently_added_movies),
	RECENTLY_ADDED_SHOWS("recentlyaddedshows", R.string.home_section_recently_added_shows),
	RECENTLY_ADDED_ALBUMS("recentlyaddedalbums", R.string.home_section_recently_added_albums),
	RECENTLY_ADDED_ARTISTS("recentlyaddedartists", R.string.home_section_recently_added_artists),
	RECENTLY_ADDED_MUSIC_VIDEOS("recentlyaddedmusicvideos", R.string.home_section_recently_added_music_videos),
	RECENTLY_ADDED_BOOKS("recentlyaddedbooks", R.string.home_section_recently_added_books),
	RECENTLY_ADDED_AUDIOBOOKS("recentlyaddedaudiobooks", R.string.home_section_recently_added_audiobooks),
	LATEST_ALBUMS("latestalbums", R.string.home_section_latest_albums),
	LATEST_MUSIC_VIDEOS("latestmusicvideos", R.string.home_section_latest_music_videos),
	LATEST_BOOKS("latestbooks", R.string.home_section_latest_books),
	LATEST_AUDIOBOOKS("latestaudiobooks", R.string.home_section_latest_audiobooks),
	NONE("none", R.string.home_section_none),
}
