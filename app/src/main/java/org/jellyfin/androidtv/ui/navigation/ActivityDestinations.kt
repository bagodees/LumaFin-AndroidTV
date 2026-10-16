package org.jellyfin.androidtv.ui.navigation

import android.content.Context
import android.content.Intent
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.ui.startup.AccountSelectionActivity
import org.jellyfin.androidtv.util.createBundle
import kotlin.time.Duration

object ActivityDestinations {
	fun accountSelection(context: Context, selectServer: Boolean, addUser: Boolean = false, serverId: java.util.UUID? = null) =
		Intent(context, AccountSelectionActivity::class.java).apply {
		putExtra(StartupActivity.EXTRA_HIDE_SPLASH, true)
		putExtra(StartupActivity.EXTRA_SELECT_SERVER, selectServer)
		putExtra(StartupActivity.EXTRA_ADD_USER, addUser)
		putExtra(StartupActivity.EXTRA_SERVER_ID, serverId?.toString())
	}
	fun externalPlayer(context: Context, position: Duration = Duration.ZERO) = Intent(context, ExternalPlayerActivity::class.java).apply {
		putExtras(
			createBundle {
				putLong(ExternalPlayerActivity.EXTRA_POSITION, position.inWholeMilliseconds)
			}
		)
	}

	fun startup(context: Context, hideSplash: Boolean = true, selectServer: Boolean = false) = Intent(context, StartupActivity::class.java).apply {
		putExtra(StartupActivity.EXTRA_HIDE_SPLASH, hideSplash)
		putExtra(StartupActivity.EXTRA_SELECT_SERVER, selectServer)
		// Remove history to prevent user going back to current activity
		addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
	}
}
