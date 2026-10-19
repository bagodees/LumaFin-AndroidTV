package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.preference.HomeShortcutPreferences
import org.jellyfin.androidtv.preference.RecommendationPreferences
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.preference.AccountPreferences
import androidx.lifecycle.lifecycleScope
import org.koin.dsl.module

val preferenceModule = module {
	single { PreferencesRepository(get(), get()) }

	single { LiveTvPreferences(get()) }
	single { AccountPreferences(get()) }
	single { UserSettingPreferences(get(), get<AccountPreferences>().preferences) }
	single { RecommendationPreferences(get(), get(), get()) }
	single { HomeShortcutPreferences(get<android.content.Context>(), get(), get()) }
	single { org.jellyfin.androidtv.preference.WatchLaterPreferences(get<android.content.Context>(), get(), get()) }
	single { org.jellyfin.androidtv.preference.PlaybackChoicePreferences(get<android.content.Context>(), get(), get()) }
	single {
		val sessions = get<org.jellyfin.androidtv.auth.repository.SessionRepository>()
		val servers = get<org.jellyfin.androidtv.auth.repository.ServerRepository>()
		val jellyfin = get<org.jellyfin.sdk.Jellyfin>()
		val device = androidx.preference.PreferenceManager.getDefaultSharedPreferences(get<android.content.Context>())
		// Random per install; identifies which TV last wrote the synced copy without revealing anything about it.
		val writer = device.getString("settings_sync_writer", null)
			?: java.util.UUID.randomUUID().toString().also { device.edit().putString("settings_sync_writer", it).apply() }
		org.jellyfin.androidtv.preference.SettingsSync(
			preferences = get<AccountPreferences>().preferences,
			identity = { sessions.currentSession.value?.let { "${it.serverId}_${it.userId}" } },
			remoteFor = remote@{ identity ->
				val session = sessions.currentSession.value?.takeIf { "${it.serverId}_${it.userId}" == identity } ?: return@remote null
				val server = servers.currentServer.value?.takeIf { it.id == session.serverId } ?: return@remote null
				// Bound to the captured account, so a later account switch cannot redirect a pending request.
				val api = jellyfin.createApi(server.address).apply { update(accessToken = session.accessToken) }
				org.jellyfin.androidtv.preference.DisplayPreferencesSyncRemote(api)
			},
			writer = writer,
			scope = androidx.lifecycle.ProcessLifecycleOwner.get().lifecycleScope,
		)
	}
	single { UserPreferences(get(), get<AccountPreferences>().preferences) }
	single { SystemPreferences(get()) }
	single { TelemetryPreferences(get()) }
}
