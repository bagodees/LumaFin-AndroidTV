package org.jellyfin.androidtv.auth.model

import org.jellyfin.androidtv.R
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import java.io.IOException
import javax.net.ssl.SSLException

/** Safe, actionable messages: never display exception text, URLs, headers or tokens. */
enum class ConnectionFailure(val title: Int, val message: Int, val retryable: Boolean) {
	UNREACHABLE(R.string.connection_unreachable_title, R.string.connection_unreachable_message, true),
	SIGN_IN(R.string.connection_sign_in_title, R.string.connection_sign_in_message, false),
	PERMISSION(R.string.connection_permission_title, R.string.connection_permission_message, false),
	SECURE_CONNECTION(R.string.connection_secure_title, R.string.connection_secure_message, false),
	ADDRESS(R.string.connection_address_title, R.string.connection_address_message, false),
	SERVER(R.string.connection_server_title, R.string.connection_server_message, true),
	RATE_LIMIT(R.string.connection_rate_title, R.string.connection_rate_message, true),
	UNKNOWN(R.string.connection_unknown_title, R.string.connection_unknown_message, true),
}

fun classifyConnectionFailure(error: Throwable?): ConnectionFailure {
	val causes = mutableListOf<Throwable>()
	var current = error
	while (current != null && causes.size < 8 && causes.none { it === current }) {
		causes += current
		current = current.cause
	}
	val status = causes.filterIsInstance<InvalidStatusException>().firstOrNull()?.status
	return when {
		status == 401 -> ConnectionFailure.SIGN_IN
		status == 403 -> ConnectionFailure.PERMISSION
		status == 404 || status == 410 -> ConnectionFailure.ADDRESS
		status == 429 -> ConnectionFailure.RATE_LIMIT
		status != null && status in 500..599 -> ConnectionFailure.SERVER
		causes.any { it is SecureConnectionException || it is SSLException } -> ConnectionFailure.SECURE_CONNECTION
		causes.any { it is TimeoutException || it is IOException } -> ConnectionFailure.UNREACHABLE
		else -> ConnectionFailure.UNKNOWN
	}
}
