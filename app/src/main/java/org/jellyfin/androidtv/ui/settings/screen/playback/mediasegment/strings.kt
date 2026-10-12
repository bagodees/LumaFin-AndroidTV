package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.sdk.model.api.MediaSegmentType

val MediaSegmentType.nameRes
	get() = when (this) {
		MediaSegmentType.UNKNOWN -> R.string.segment_type_unknown
		MediaSegmentType.COMMERCIAL -> R.string.segment_type_commercial
		MediaSegmentType.PREVIEW -> R.string.segment_type_preview
		MediaSegmentType.RECAP -> R.string.segment_type_recap
		MediaSegmentType.OUTRO -> R.string.segment_type_outro
		MediaSegmentType.INTRO -> R.string.segment_type_intro
	}

val MediaSegmentAction.introNameRes
	get() = when (this) {
		MediaSegmentAction.NOTHING -> R.string.intro_skip_never
		MediaSegmentAction.SKIP -> R.string.intro_skip_automatic
		MediaSegmentAction.ASK_TO_SKIP -> R.string.segment_action_ask_to_skip
	}

val MediaSegmentAction.introDescriptionRes
	get() = when (this) {
		MediaSegmentAction.NOTHING -> R.string.intro_skip_never_description
		MediaSegmentAction.SKIP -> R.string.intro_skip_automatic_description
		MediaSegmentAction.ASK_TO_SKIP -> R.string.intro_skip_ask_description
	}
