package org.jellyfin.androidtv.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.repository.Session
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.ui.shared.toolbar.QuickUserSwitchController
import org.jellyfin.androidtv.ui.shared.toolbar.UserSwitchResult
import java.util.UUID

class QuickUserSwitchTests : FunSpec({
	val server = UUID(0, 1)
	val first = UUID(0, 2)
	val second = PrivateUser(UUID(0, 3), server, "Second", "saved-token", null, 0)

	fun fixture(alwaysAuthenticate: Boolean = false): Pair<SessionRepository, QuickUserSwitchController> {
		val sessions = mockk<SessionRepository>()
		every { sessions.currentSession } returns MutableStateFlow(Session(first, server, "first-token"))
		val preferences = mockk<AuthenticationPreferences>()
		every { preferences[AuthenticationPreferences.alwaysAuthenticate] } returns alwaysAuthenticate
		return sessions to QuickUserSwitchController(sessions, preferences)
	}

	test("opening the picker and choosing the current user never ends or switches the session") {
		val (sessions, controller) = fixture()
		controller.result.value shouldBe UserSwitchResult.IDLE
		controller.switch(second.copy(id = first))
		controller.result.value shouldBe UserSwitchResult.IDLE
		coVerify(exactly = 0) { sessions.switchCurrentSession(any(), any()) }
	}
	test("users without a token and always authenticate users need explicit sign in") {
		for (always in listOf(false, true)) {
			val (sessions, controller) = fixture(always)
			controller.switch(if (always) second else second.copy(accessToken = null))
			controller.result.value shouldBe UserSwitchResult.SIGN_IN_REQUIRED
			coVerify(exactly = 0) { sessions.switchCurrentSession(any(), any()) }
		}
	}
	test("a successful saved user switch completes once") {
		val (sessions, controller) = fixture()
		coEvery { sessions.switchCurrentSession(server, second.id) } returns true
		controller.switch(second) shouldBe true
		controller.result.value shouldBe UserSwitchResult.SWITCHED
		controller.switch(second) shouldBe false
		coVerify(exactly = 1) { sessions.switchCurrentSession(server, second.id) }
	}
	test("failed validation remains retryable") {
		val (sessions, controller) = fixture()
		coEvery { sessions.switchCurrentSession(server, second.id) } returnsMany listOf(false, true)
		controller.switch(second)
		controller.result.value shouldBe UserSwitchResult.FAILED
		controller.switch(second)
		controller.result.value shouldBe UserSwitchResult.SWITCHED
	}
	test("unexpected server failure leaves a usable picker") {
		val (sessions, controller) = fixture()
		coEvery { sessions.switchCurrentSession(server, second.id) } throws IllegalStateException("offline")
		controller.switch(second)
		controller.result.value shouldBe UserSwitchResult.FAILED
	}
	test("repeated clicks while validation is pending cannot start another switch") {
		val (sessions, controller) = fixture()
		val entered = CompletableDeferred<Unit>()
		val release = CompletableDeferred<Unit>()
		coEvery { sessions.switchCurrentSession(server, second.id) } coAnswers {
			entered.complete(Unit)
			release.await()
			true
		}
		coroutineScope {
			val job = launch { controller.switch(second) }
			entered.await()
			controller.result.value shouldBe UserSwitchResult.SWITCHING
			controller.switch(second) shouldBe false
			release.complete(Unit)
			job.join()
		}
		coVerify(exactly = 1) { sessions.switchCurrentSession(server, second.id) }
	}
})
