package org.jellyfin.androidtv.data.eventhandling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.api.sockets.SocketConnectionState

private const val ID = "00000000-0000-0000-0000-000000000001"

class DecodableSocketConnectionFactoryTests : FunSpec({
	test("normal server messages pass through") {
		isDecodableSocketMessage("""{"MessageType":"ForceKeepAlive","MessageId":"$ID","Data":60}""") shouldBe true
	}

	test("a plugin install notice without an overview is left out") {
		val message = """{"MessageType":"PackageInstalling","MessageId":"$ID","Data":{"Guid":"$ID","Name":"SeerrFin",""" +
			""""Version":"1.0.0.0","PackageInfo":{"name":"SeerrFin","description":"","owner":"","category":"General",""" +
			""""guid":"$ID","versions":[],"imageUrl":null}}}"""
		isDecodableSocketMessage(message) shouldBe false
	}

	test("connection changes always pass through") {
		SocketConnectionState.Connecting.isReadable() shouldBe true
		SocketConnectionState.Disconnected().isReadable() shouldBe true
	}
})
