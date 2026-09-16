package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.base.Badge
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Seekbar
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.design.Tokens
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.koin.compose.koinInject
import java.util.Locale

@Composable
@Stable
fun ItemCardBaseItemOverlay(
	item: BaseItemDto,
	footer: (@Composable () -> Unit)? = null,
) = Box(
	modifier = Modifier
		.fillMaxSize()
		.padding(Tokens.Space.spaceXs)
) {
	Column(
		modifier = Modifier.align(Alignment.TopStart),
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)
	) {
		StateIndicator(item = item)
		RatingBadges(item = item)
	}

	WatchIndicator(
		item = item,
		modifier = Modifier.align(Alignment.TopEnd)
	)

	Column(
		modifier = Modifier.align(Alignment.BottomCenter),
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)
	) {
		ProgressIndicator(
			item = item,
		)

		if (footer != null) footer()
	}
}

@Composable
@Stable
private fun StateIndicator(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	val isFavorited = item.userData?.isFavorite == true
	val isRecording = item.timerId?.takeIf { item.type == BaseItemKind.LIVE_TV_PROGRAM || item.type == BaseItemKind.PROGRAM } != null
	val isRecordingActive = item.seriesTimerId != null && isRecording

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)
	) {
		if (isRecording) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_record_series),
				contentDescription = null,
				tint = if (isRecordingActive) Tokens.Color.colorRed600 else Tokens.Color.colorGrey100,
				modifier = modifier
					.size(24.dp)
			)
		}

		if (isFavorited) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_heart),
				contentDescription = null,
				tint = Tokens.Color.colorRed500,
				modifier = modifier
					.size(24.dp)
			)
		}
	}
}

// Item types that show rating badges, matching the webui rating badges mod
private val ratedItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
private const val CRITIC_RATING_FRESH = 60f

@Composable
@Stable
private fun RatingBadges(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	if (item.type !in ratedItemTypes) return

	val communityRating = item.communityRating
	val criticRating = item.criticRating
	if (communityRating == null && criticRating == null) return

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)
	) {
		if (criticRating != null) {
			val fresh = criticRating >= CRITIC_RATING_FRESH

			RatingBadge(
				containerColor = if (fresh) Tokens.Color.colorGreen600 else Tokens.Color.colorRed600,
				contentColor = Tokens.Color.colorGrey25,
				icon = if (fresh) R.drawable.ic_rt_fresh else R.drawable.ic_rt_rotten,
				iconTint = Color.Unspecified,
				text = "${criticRating.toInt()}%",
			)
		}

		if (communityRating != null) {
			RatingBadge(
				containerColor = Tokens.Color.colorYellow400,
				contentColor = Tokens.Color.colorGrey975,
				icon = R.drawable.ic_star,
				iconTint = Tokens.Color.colorGrey975,
				text = String.format(Locale.getDefault(), "%.1f", communityRating),
			)
		}
	}
}

@Composable
private fun RatingBadge(
	containerColor: Color,
	contentColor: Color,
	icon: Int,
	iconTint: Color,
	text: String,
) = Badge(
	shape = RoundedCornerShape(Tokens.Space.spaceXs),
	containerColor = containerColor,
	contentColor = contentColor,
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			imageVector = ImageVector.vectorResource(icon),
			contentDescription = null,
			tint = iconTint,
			modifier = Modifier.size(14.dp)
		)

		Text(text = text)
	}
}

@Composable
@Stable
private fun WatchIndicator(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	val userPreferences = koinInject<UserPreferences>()
	val watchedIndicatorBehavior = userPreferences[UserPreferences.watchedIndicatorBehavior]

	if (watchedIndicatorBehavior == WatchedIndicatorBehavior.NEVER) return
	if (watchedIndicatorBehavior == WatchedIndicatorBehavior.EPISODES_ONLY && item.type != BaseItemKind.EPISODE) return

	val isPlayed = item.userData?.played == true
	val unplayedItems = item.userData?.unplayedItemCount?.takeIf { it > 0 }

	if (isPlayed) {
		Badge(
			modifier = modifier
				.size(24.dp),
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_watch),
				contentDescription = null,
				modifier = Modifier.size(12.dp)
			)
		}
	} else if (unplayedItems != null) {
		if (watchedIndicatorBehavior == WatchedIndicatorBehavior.HIDE_UNWATCHED) return

		Badge(
			modifier = modifier
				.sizeIn(minWidth = 24.dp, minHeight = 24.dp),
		) {
			Text(
				text = unplayedItems.toString(),
			)
		}
	}
}

// Media types that need to show a user progress bar (based on playedPercentage in userData)
private val progressMediaTypes = setOf(MediaType.VIDEO, MediaType.BOOK)

@Composable
private fun ProgressIndicator(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	val playbackManager = koinInject<PlaybackManager>()
	val playState by playbackManager.state.playState.collectAsState()
	val currentQueueEntry by rememberQueueEntry(playbackManager)

	val playedPercentage = when {
		playState == PlayState.PLAYING && currentQueueEntry?.baseItem?.id == item.id -> rememberPlayerProgress(playbackManager).value
		item.mediaType in progressMediaTypes -> item.userData?.playedPercentage?.toFloat()?.div(100f)?.coerceIn(0f, 1f)

		else -> null
	}

	if (playedPercentage != null) {
		Box(modifier = modifier.padding(Tokens.Space.spaceXs)) {
			Seekbar(
				progress = playedPercentage,
				enabled = false,
				modifier = Modifier
					.fillMaxWidth()
					.height(4.dp)
			)
		}
	}
}
