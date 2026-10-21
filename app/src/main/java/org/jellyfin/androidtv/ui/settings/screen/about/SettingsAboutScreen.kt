package org.jellyfin.androidtv.ui.settings.screen.about

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.settings.composable.SettingsButton as ListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsSection as ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.util.copyAction
import org.jellyfin.androidtv.util.AppVersionLabel

@Composable
fun SettingsAboutScreen(launchedFromLogin: Boolean = false) {
	val router = LocalRouter.current

	SettingsColumn {
		if (launchedFromLogin) item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
			)
		} else item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
			)
		}

		item {
			val heading = "LumaFin app version"
			val caption = AppVersionLabel.format(BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.NEXT_VERSION)
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_jellyfin), contentDescription = null, modifier = Modifier.size(24.dp)) },
				headingContent = { Text(heading) },
				captionContent = { Text(caption) },
				onClick = copyAction(ClipData.newPlainText(heading, caption)),
				modifier = Modifier.focusKey("version")
			)
		}

		item {
			val heading = stringResource(R.string.pref_device_model)
			val caption = "${Build.MANUFACTURER} ${Build.MODEL}"
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_tv), contentDescription = null) },
				headingContent = { Text(heading) },
				captionContent = { Text(caption) },
				onClick = copyAction(ClipData.newPlainText(heading, caption)),
				modifier = Modifier.focusKey("device_model")
			)
		}

		item {
			val context = LocalContext.current
			val heading = stringResource(R.string.privacy_policy)
			val url = stringResource(R.string.privacy_policy_url)
			val copy = copyAction(ClipData.newPlainText(heading, url))
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
				headingContent = { Text(heading) },
				captionContent = { Text(url.removePrefix("https://")) },
				// Many TVs have no browser; the address stays visible and is copied when nothing can open it.
				onClick = {
					val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
					if (runCatching { context.startActivity(intent) }.isFailure) copy()
				},
				modifier = Modifier.focusKey("privacy_policy")
			)
		}

		item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.licenses_link)) },
				onClick = { router.push(Routes.LICENSES) },
				modifier = Modifier.focusKey(Routes.LICENSES)
			)
		}

		if (!launchedFromLogin) item {
			ListButton(
				navigates = true,
				leadingContent = { Icon(painterResource(R.drawable.ic_flask), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_developer_link)) },
				onClick = { router.push(Routes.DEVELOPER) },
				modifier = Modifier.focusKey(Routes.DEVELOPER)
			)
		}
	}
}
