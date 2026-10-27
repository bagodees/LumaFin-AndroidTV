package org.jellyfin.androidtv.di

import android.os.Handler
import android.os.Looper
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
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.jellyfin.androidtv.auth.repository.ServerAddressRepository

val authModule = module {
	single { AuthenticationStore(get()) }
	single { AuthenticationPreferences(get()) }
	single {
		val jellyfin = get<Jellyfin>()
		ServerAddressRepository(get()) { address ->
			// Do not use the shared authenticated API or supply an access token.
			jellyfin.createApi(address).systemApi.getPublicSystemInfo().content.id?.toUUIDOrNull()
		}
	}

	single<AuthenticationRepository> {
		AuthenticationRepositoryImpl(get(), get(), get(), get(), get(), get(defaultDeviceInfo))
	}
	single<ServerRepository> { ServerRepositoryImpl(get(), get()) }
	single<ServerUserRepository> { ServerUserRepositoryImpl(get(), get()) }
	single<SessionRepository> {
		val jellyfin = get<Jellyfin>()
		SessionRepositoryImpl(get(), get(), get(), get(), get(defaultDeviceInfo), get(), get(), get(), get(), get(),
			{
				// Sessions are restored and destroyed on Dispatchers.IO, but the player must only be used on main.
				val clear = { get<org.jellyfin.androidtv.ui.playback.MediaManager>().clearAudioQueue() }
				if (Looper.myLooper() == Looper.getMainLooper()) clear()
				else Handler(Looper.getMainLooper()).post { clear() }
			},
			{ server, session, device ->
				val api = jellyfin.createApi(server.address, deviceInfo = device)
				api.update(accessToken = session.accessToken)
				api.userApi.getCurrentUser().content
			}, get())
	}

	factory {
		val serverRepository = get<ServerRepository>()
		serverRepository.currentServer.value?.serverVersion ?: ServerRepository.minimumServerVersion
	}
}
