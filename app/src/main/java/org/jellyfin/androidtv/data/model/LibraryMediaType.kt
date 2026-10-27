package org.jellyfin.androidtv.data.model

import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType

/**
 * The Type filter for libraries that hold both videos and photos (#86). It hides the other kind of
 * item rather than asking for one media type, so folders stay browsable.
 */
enum class LibraryMediaType(val labelRes: Int, val excluded: Set<BaseItemKind>) {
	ALL(R.string.library_filter_any, emptySet()),
	VIDEOS(R.string.library_type_videos, setOf(BaseItemKind.PHOTO, BaseItemKind.PHOTO_ALBUM)),
	PHOTOS(R.string.library_type_photos, setOf(BaseItemKind.VIDEO, BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.MUSIC_VIDEO));

	companion object {
		/** Home videos, photo and mixed-content libraries; movie, show and music libraries hold only one kind. */
		@JvmStatic
		fun appliesTo(collectionType: CollectionType?) =
			collectionType == null || collectionType == CollectionType.HOMEVIDEOS || collectionType == CollectionType.PHOTOS ||
				collectionType == CollectionType.FOLDERS || collectionType == CollectionType.UNKNOWN
	}
}
