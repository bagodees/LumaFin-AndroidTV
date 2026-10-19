package org.jellyfin.androidtv.ui.presentation

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.rememberAccessibilityOptions
import org.jellyfin.androidtv.ui.browsing.compactDurationMinutes
import org.jellyfin.androidtv.ui.browsing.compactProgress
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** One native Leanback focus target per row; artwork and progress cannot steal D-pad focus. */
@Composable
internal fun CompactLibraryRow(item: BaseRowItem?, focused: Boolean) {
	if (item == null) return
	val context = LocalContext.current
	val density = LocalDensity.current
	val preferences = koinInject<UserPreferences>()
	val hideArtwork by preferences.episodeArtworkChanges.collectAsState(initial = preferences[UserPreferences.hideUnwatchedEpisodeArtwork])
	val image = remember(item, hideArtwork) { item.getImage(ImageType.THUMB) }
	val baseItem = item.baseItem
	val progress = baseItem?.let(::compactProgress)
	val metadata = buildList {
		baseItem?.productionYear?.takeIf { it > 0 }?.let { add(it.toString()) }
		baseItem?.let(::compactDurationMinutes)?.let { add(stringResource(R.string.library_list_minutes, it)) }
		if (baseItem?.userData?.played == true) add(stringResource(R.string.library_list_watched))
		else progress?.let { add(stringResource(R.string.library_list_progress, (it * 100).roundToInt())) }
	}.joinToString(" · ")
	val shape = RoundedCornerShape(8.dp)
	val colors = JellyfinTheme.colorScheme
	val accessibility = rememberAccessibilityOptions()
	Row(
		modifier = Modifier.width((LocalConfiguration.current.screenWidthDp - 80).coerceAtLeast(240).dp)
			.height(96.dp).background(if (focused) colors.listButtonFocused else colors.surface, shape)
			.border(if (accessibility.strongFocus) 3.dp else 2.dp,
				if (!focused) Color.Transparent else if (accessibility.strongFocus) Color.White else colors.inputFocused, shape).padding(10.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(16.dp),
	) {
		Box(Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(4.dp)).background(colors.button), contentAlignment = Alignment.Center) {
			if (image != null) {
				val api = koinInject<ApiClient>()
				AsyncImage(url = image.getUrl(api, maxWidth = with(density) { 120.dp.roundToPx() }, maxHeight = with(density) { 68.dp.roundToPx() }),
					blurHash = image.blurHash, aspectRatio = 16f / 9f, scaleType = ImageView.ScaleType.CENTER_CROP,
					modifier = Modifier.fillMaxSize())
			} else Image(painterResource(R.drawable.ic_clapperboard), contentDescription = null, modifier = Modifier.size(32.dp))
		}
		Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Text(item.getFullName(context) ?: stringResource(R.string.lbl_bracket_unknown), color = Color.White, fontSize = 20.sp, lineHeight = 23.sp,
				fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
			if (metadata.isNotEmpty()) Text(metadata, color = colors.listCaption, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
			if (progress != null) Box(Modifier.fillMaxWidth().height(3.dp).background(colors.rangeControlBackground)) {
				Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(colors.rangeControlFill))
			}
		}
	}
}
