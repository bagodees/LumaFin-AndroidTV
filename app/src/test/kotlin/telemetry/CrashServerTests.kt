package org.jellyfin.androidtv.telemetry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class CrashServerTests : FunSpec({
	test("server addresses keep their scheme but lose the host") {
		CrashServer.scrub("GET https://jarvis.example.com:8920/Items/abc/PlaybackInfo") shouldBe
			"GET https://[server]/Items/abc/PlaybackInfo"
		CrashServer.scrub("socket wss://media.example.org/socket") shouldBe "socket wss://[server]/socket"
	}
	test("tokens and keys in query strings and headers are removed") {
		val scrubbed = CrashServer.scrub(
			"url ?api_key=0123456789abcdef&static=true X-Emby-Token: deadbeef Authorization: MediaBrowser Token=\"secret\""
		)
		scrubbed shouldNotContain "0123456789abcdef"
		scrubbed shouldNotContain "deadbeef"
		scrubbed shouldNotContain "secret"
	}
	test("IP addresses are removed") {
		CrashServer.scrub("connect to 192.168.1.20 failed") shouldBe "connect to [ip] failed"
	}
	test("stack traces stay readable") {
		val line = "\tat org.jellyfin.androidtv.ui.playback.PlaybackController.play(PlaybackController.java:512)"
		CrashServer.scrub(line) shouldBe line
	}
})
