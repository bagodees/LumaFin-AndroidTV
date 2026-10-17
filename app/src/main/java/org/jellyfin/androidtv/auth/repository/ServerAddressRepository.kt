package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.model.classifyConnectionFailure
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import java.net.URI
import java.util.UUID

sealed interface BackupAddressResult {
	data class Saved(val address: String) : BackupAddressResult
	data object Removed : BackupAddressResult
	data object InvalidAddress : BackupAddressResult
	data object SameAddress : BackupAddressResult
	data object DifferentServer : BackupAddressResult
	data object MissingServerIdentity : BackupAddressResult
	data object ServerNotFound : BackupAddressResult
	data class Unavailable(val failure: ConnectionFailure) : BackupAddressResult
}

/** Accept an explicit address without embedded credentials, query tokens or fragments. */
fun normalizeServerAddress(input: String): String? = try {
	val uri = URI(input.trim())
	if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() ||
		uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null || uri.port !in -1..65535 || uri.port == 0
	) null
	else URI("${uri.scheme.lowercase()}://${uri.rawAuthority.lowercase()}${uri.rawPath.orEmpty()}").toASCIIString().trimEnd('/')
} catch (_: IllegalArgumentException) {
	null
} catch (_: java.net.URISyntaxException) {
	null
}

/** The probe must use an unauthenticated client, including on subsequent re-verification. */
class ServerAddressRepository(
	private val store: AuthenticationStore,
	private val probeIdentity: suspend (String) -> UUID?,
) {
	private val mutex = Mutex()

	suspend fun saveBackup(serverId: UUID, input: String): BackupAddressResult = mutex.withLock {
		val server = store.getServer(serverId) ?: return@withLock BackupAddressResult.ServerNotFound
		if (input.isBlank()) {
			store.putServer(serverId, server.copy(backupAddress = null))
			return@withLock BackupAddressResult.Removed
		}
		val address = normalizeServerAddress(input) ?: return@withLock BackupAddressResult.InvalidAddress
		if (address == normalizeServerAddress(server.address)) return@withLock BackupAddressResult.SameAddress
		val identity = try {
			probeIdentity(address)
		} catch (err: CancellationException) {
			throw err
		} catch (err: Exception) {
			return@withLock BackupAddressResult.Unavailable(classifyConnectionFailure(err))
		}
		if (identity == null) return@withLock BackupAddressResult.MissingServerIdentity
		if (identity != serverId) return@withLock BackupAddressResult.DifferentServer
		// Re-read after the network call: never overwrite a concurrent login/update,
		// and never recreate a server that was removed while verification was pending.
		val latest = store.getServer(serverId) ?: return@withLock BackupAddressResult.ServerNotFound
		store.putServer(serverId, latest.copy(backupAddress = address))
		BackupAddressResult.Saved(address)
	}

	suspend fun verifyBackup(serverId: UUID): BackupAddressResult {
		val server = store.getServer(serverId) ?: return BackupAddressResult.ServerNotFound
		val address = server.backupAddress ?: return BackupAddressResult.InvalidAddress
		return try {
			val normalized = normalizeServerAddress(address) ?: return BackupAddressResult.InvalidAddress
			when (probeIdentity(normalized)) {
				null -> BackupAddressResult.MissingServerIdentity
				serverId -> BackupAddressResult.Saved(normalized)
				else -> BackupAddressResult.DifferentServer
			}
		} catch (err: CancellationException) {
			throw err
		} catch (err: Exception) {
			BackupAddressResult.Unavailable(classifyConnectionFailure(err))
		}
	}
}
