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
	single { UserPreferences(get(), get<AccountPreferences>().preferences) }
	single { SystemPreferences(get()) }
	single { TelemetryPreferences(get()) }
}
