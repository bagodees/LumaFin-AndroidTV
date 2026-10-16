package org.jellyfin.androidtv.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.model.classifyConnectionFailure
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ConnectionFailureTests : FunSpec({
	test("expired credentials and refused access have distinct non-retry explanations") {
		classifyConnectionFailure(InvalidStatusException(401)) shouldBe ConnectionFailure.SIGN_IN
		classifyConnectionFailure(InvalidStatusException(403)) shouldBe ConnectionFailure.PERMISSION
		ConnectionFailure.SIGN_IN.retryable shouldBe false
		ConnectionFailure.PERMISSION.retryable shouldBe false
	}
	test("network errors including wrapped DNS failures offer a manual retry") {
		classifyConnectionFailure(TimeoutException()) shouldBe ConnectionFailure.UNREACHABLE
		classifyConnectionFailure(ApiClientException("private URL must never be shown", UnknownHostException())) shouldBe
			ConnectionFailure.UNREACHABLE
		ConnectionFailure.UNREACHABLE.retryable shouldBe true
	}
	test("TLS failures do not suggest repeatedly retrying or bypassing certificate checks") {
		classifyConnectionFailure(SecureConnectionException()) shouldBe ConnectionFailure.SECURE_CONNECTION
		classifyConnectionFailure(ApiClientException(cause = SSLHandshakeException("private certificate details"))) shouldBe
			ConnectionFailure.SECURE_CONNECTION
		ConnectionFailure.SECURE_CONNECTION.retryable shouldBe false
	}
	test("wrong endpoints server faults and rate limiting have different recovery messages") {
		for (status in listOf(404, 410)) classifyConnectionFailure(InvalidStatusException(status)) shouldBe ConnectionFailure.ADDRESS
		for (status in listOf(500, 502, 503, 504)) classifyConnectionFailure(InvalidStatusException(status)) shouldBe ConnectionFailure.SERVER
		classifyConnectionFailure(InvalidStatusException(429)) shouldBe ConnectionFailure.RATE_LIMIT
	}
	test("unknown failures have a stable generic message independent of sensitive error text") {
		classifyConnectionFailure(null) shouldBe ConnectionFailure.UNKNOWN
		classifyConnectionFailure(IllegalStateException("https://user:secret@private.example/?api_key=secret")) shouldBe
			ConnectionFailure.UNKNOWN
	}
	test("cyclic exception causes cannot hang classification") {
		val first = Exception()
		val second = Exception(first)
		first.initCause(second)
		classifyConnectionFailure(first) shouldBe ConnectionFailure.UNKNOWN
	}
})
