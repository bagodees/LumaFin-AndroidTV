package org.jellyfin.androidtv.data.eventhandling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.api.sockets.SocketConnection
import org.jellyfin.sdk.api.sockets.SocketConnectionFactory
import org.jellyfin.sdk.api.sockets.SocketConnectionState
import timber.log.Timber

/**
 * The SDK reads live server messages in its own background task, where a message it can't decode
 * ends the app. A plugin's install notice without an overview did that (October 2026). Messages that
 * don't decode are left out here before the SDK reads them; the app doesn't use the ones seen so far.
 */
class DecodableSocketConnectionFactory(private val factory: SocketConnectionFactory) : SocketConnectionFactory {
	override fun create(clientOptions: HttpClientOptions, scope: CoroutineScope): SocketConnection {
		val connection = factory.create(clientOptions, scope)
		val initial = connection.state.value.takeIf { it.isReadable() } ?: SocketConnectionState.Disconnected()

		return object : SocketConnection by connection {
			// Unconfined so each message is checked as it arrives and none are skipped in a burst
			override val state: StateFlow<SocketConnectionState> = connection.state
				.filter { it.isReadable() }
				.stateIn(scope + Dispatchers.Unconfined, SharingStarted.Eagerly, initial)
		}
	}
}

internal fun SocketConnectionState.isReadable(): Boolean {
	if (this !is SocketConnectionState.Message) return true
	return isDecodableSocketMessage(message)
}

internal fun isDecodableSocketMessage(message: String): Boolean = try {
	ApiSerializer.decodeSocketMessage(message)
	true
} catch (e: IllegalArgumentException) {
	// Serialization errors are argument errors; the message itself isn't logged as it can hold user data
	Timber.w("Ignoring a server message the app can't read: %s", e.message?.lineSequence()?.firstOrNull())
	false
}
