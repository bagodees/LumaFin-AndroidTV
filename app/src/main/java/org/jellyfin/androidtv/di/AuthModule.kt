package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.AuthenticationRepositoryImpl
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerRepositoryImpl
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepositoryImpl
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryImpl
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.koin.dsl.module
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.userApi

val authModule = module {
	single { AuthenticationStore(get()) }
	single { AuthenticationPreferences(get()) }

	single<AuthenticationRepository> {
		AuthenticationRepositoryImpl(get(), get(), get(), get(), get(), get(defaultDeviceInfo))
	}
	single<ServerRepository> { ServerRepositoryImpl(get(), get()) }
	single<ServerUserRepository> { ServerUserRepositoryImpl(get(), get()) }
	single<SessionRepository> {
		val jellyfin = get<Jellyfin>()
		SessionRepositoryImpl(get(), get(), get(), get(), get(defaultDeviceInfo), get(), get(), get(), get(), get(),
			{ get<org.jellyfin.androidtv.ui.playback.MediaManager>().clearAudioQueue() },
			{ server, session, device ->
				val api = jellyfin.createApi(server.address, deviceInfo = device)
				api.update(accessToken = session.accessToken)
				api.userApi.getCurrentUser().content
			})
	}

	factory {
		val serverRepository = get<ServerRepository>()
		serverRepository.currentServer.value?.serverVersion ?: ServerRepository.minimumServerVersion
	}
}
