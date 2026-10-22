package org.jellyfin.androidtv.data.repository

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.data.model.AppNotification
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.util.isTvDevice
import org.jellyfin.sdk.model.ServerVersion

interface NotificationsRepository {
	val notifications: StateFlow<List<AppNotification>>

	fun dismissNotification(item: AppNotification)
	fun addDefaultNotifications()
	fun updateServerNotifications(server: Server?)
}

class NotificationsRepositoryImpl(
	private val context: Context,
	private val systemPreferences: SystemPreferences,
	private val appUpdateChecker: AppUpdateChecker,
) : NotificationsRepository {
	override val notifications = MutableStateFlow(emptyList<AppNotification>())
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	init {
		addDefaultNotifications()
	}

	override fun dismissNotification(item: AppNotification) {
		notifications.value = notifications.value.filter { it != item }
		item.dismiss()
	}

	override fun addDefaultNotifications() {
		addUiModeNotification()
		addBetaNotification()
		addUpdateAvailableNotification()
	}

	private fun addNotification(
		message: String,
		public: Boolean = false,
		highlight: Boolean = false,
		dismiss: () -> Unit = {}
	): AppNotification {
		val notification = AppNotification(message, dismiss, public, highlight)
		notifications.value += notification
		return notification
	}

	private fun removeNotification(notification: AppNotification) {
		notifications.value -= notification
	}

	private fun addUiModeNotification() {
		val disableUiModeWarning = systemPreferences[SystemPreferences.disableUiModeWarning]

		if (!context.isTvDevice() && !disableUiModeWarning) {
			addNotification(
				context.getString(R.string.app_notification_uimode_invalid),
				public = true
			)
		}
	}

	private fun addBetaNotification() {
		val dismissedVersion = systemPreferences[SystemPreferences.dismissedBetaNotificationVersion]
		val currentVersion = BuildConfig.VERSION_NAME
		val isBeta = currentVersion.lowercase().contains("beta")

		if (isBeta && currentVersion != dismissedVersion) {
			addNotification(context.getString(R.string.app_notification_beta, currentVersion)) {
				systemPreferences[SystemPreferences.dismissedBetaNotificationVersion] =
					currentVersion
			}
		}
	}

	private fun addUpdateAvailableNotification() {
		// Development builds have no real version to compare against
		if (BuildConfig.DEVELOPMENT) return
		// Play manages its own releases, which may differ from the website channel.
		// Installer source does not tell us whether automatic updates are enabled.
		val installer = runCatching {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
			} else {
				@Suppress("DEPRECATION")
				context.packageManager.getInstallerPackageName(context.packageName)
			}
		}.getOrNull()
		if (installer == "com.android.vending") return

		scope.launch {
			val latestVersion = appUpdateChecker.getLatestVersion() ?: return@launch
			val dismissedVersion = systemPreferences[SystemPreferences.dismissedUpdateNotificationVersion]

			if (latestVersion != dismissedVersion && AppUpdateChecker.isNewer(latestVersion, BuildConfig.VERSION_NAME)) {
				addNotification(context.getString(R.string.app_notification_update_available, latestVersion), highlight = true) {
					systemPreferences[SystemPreferences.dismissedUpdateNotificationVersion] = latestVersion
				}
			}
		}
	}

	// Update server notification
	private var _updateServerNotification: AppNotification? = null
	override fun updateServerNotifications(server: Server?) {
		// Remove current update notification
		_updateServerNotification?.let(::removeNotification)

		val currentServerVersion = server?.version?.let(ServerVersion::fromString) ?: return
		if (currentServerVersion < ServerRepository.upcomingMinimumServerVersion) {
			_updateServerNotification =
				addNotification(
					message = context.getString(
						R.string.app_notification_update_soon,
						currentServerVersion,
						ServerRepository.upcomingMinimumServerVersion
					),
				)
		}
	}
}
