package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.PlaybackChoicePreferences
import androidx.compose.runtime.mutableStateOf
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.introNameRes
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val externalAppRepository = koinInject<ExternalAppRepository>()
	val userPreferences = koinInject<UserPreferences>()
	val mediaSegmentRepository = koinInject<MediaSegmentRepository>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback)) },
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_tv_play), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.playback_video_player)) },
				captionContent = { Text(stringResource(R.string.settings_search_player)) },
				trailingContent = {
					val iconDrawable = remember(context) {
						externalAppRepository.getCurrentExternalPlayerApp(context)?.loadIcon(context.packageManager)
					}
					Image(
						painter = if (iconDrawable == null) rememberAsyncImagePainter(R.mipmap.app_icon)
						else rememberAsyncImagePainter(iconDrawable),
						contentDescription = null,
						modifier = Modifier
							.size(24.dp)
							.clip(LocalShapes.current.small)
					)
				},
				onClick = { router.push(Routes.PLAYBACK_PLAYER) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PLAYER)
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_photo), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.photo_player)) },
				captionContent = { Text(stringResource(R.string.settings_search_photos)) },
				onClick = { router.push(Routes.PLAYBACK_PHOTO_PLAYER) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PHOTO_PLAYER)
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_next_up), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_autoplay)) },
				captionContent = { Text(stringResource(R.string.settings_search_autoplay)) },
				onClick = { router.push(Routes.PLAYBACK_AUTOPLAY) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_AUTOPLAY)
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_trailer), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_prerolls)) },
				captionContent = { Text(stringResource(R.string.settings_search_prerolls)) },
				onClick = { router.push(Routes.PLAYBACK_PREROLLS) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PREROLLS)
			)
		}

		item {
			val quality by rememberPreference(userPreferences, UserPreferences.streamingQuality)
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_select_quality), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_streaming_quality)) },
				captionContent = { Text(stringResource(quality.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_QUALITY) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_QUALITY),
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_subtitles), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_customization_subtitles)) },
				captionContent = { Text(stringResource(R.string.settings_search_subtitles)) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES)
			)
		}

		item {
			val introAction = mediaSegmentRepository.getDefaultSegmentTypeAction(MediaSegmentType.INTRO)
			ListButton(
				navigates = true,
				headingContent = { Text(stringResource(R.string.intro_skipping)) },
				captionContent = { Text(stringResource(introAction.introNameRes)) },
				onClick = { router.push(Routes.PLAYBACK_MEDIA_SEGMENT, parameters = mapOf("segmentType" to MediaSegmentType.INTRO.toString())) },
				modifier = Modifier.focusKey("intro_skipping"),
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_clapperboard), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.settings_intro_recap_credits)) },
				captionContent = { Text(stringResource(R.string.settings_search_segments)) },
				onClick = { router.push(Routes.PLAYBACK_MEDIA_SEGMENTS) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_MEDIA_SEGMENTS)
			)
		}

		item {
			val playbackChoices = koinInject<PlaybackChoicePreferences>()
			var hasChoices by remember { mutableStateOf(!playbackChoices.isEmpty()) }

			ListButton(
				headingContent = { Text(stringResource(R.string.playback_choices_clear)) },
				captionContent = { Text(stringResource(R.string.playback_choices_clear_description)) },
				enabled = hasChoices,
				onClick = {
					androidx.appcompat.app.AlertDialog.Builder(context)
						.setMessage(R.string.playback_choices_clear_confirm)
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(R.string.playback_choices_forget) { _, _ ->
							playbackChoices.clearAll()
							hasChoices = false
							android.widget.Toast.makeText(context, R.string.playback_choices_cleared, android.widget.Toast.LENGTH_SHORT).show()
						}
						.show()
				},
				modifier = Modifier.focusKey("playback_choices_clear")
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_more), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) },
				captionContent = { Text(stringResource(R.string.settings_search_advanced)) },
				onClick = { router.push(Routes.PLAYBACK_ADVANCED) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_ADVANCED)
			)
		}
	}
}
