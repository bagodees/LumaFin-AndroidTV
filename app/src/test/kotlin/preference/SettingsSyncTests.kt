package org.jellyfin.androidtv.preference

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jellyfin.androidtv.preference.SettingsSyncModel.Resolution

private class FakeRemote(var text: String? = null) : SettingsSyncRemote {
	var writes = 0
	var beforeWrite: (() -> Unit)? = null
	override suspend fun read() = text
	override suspend fun write(text: String?) {
		beforeWrite?.invoke()
		writes++
		this.text = text
	}
	fun settings() = text?.let(SettingsSyncModel::decode)?.settings
}

class SettingsSyncModelTests : FunSpec({
	val m = SettingsSyncModel
	val base = mapOf<String, Any>("subtitle_brightness" to 100, "pref_rating_badges_enabled" to true)

	test("synced settings are the 17 personal preferences and exclude per-TV layout") {
		m.syncedKeys.size shouldBe 17
		listOf("homeSectionOrder", "media_card_size", "app_theme", "subtitles_text_size", "subtitles_offset_position", "reduced_motion")
			.forEach { (it in m.syncedKeys) shouldBe false }
		m.fields.map { it.key }.toSet() shouldBe m.syncedKeys.toSet()
	}

	test("merge takes one-sided changes and flags only true conflicts") {
		val one = m.merge(base, base + ("subtitle_brightness" to 60), base)
		one.toLocal shouldBe emptyMap()
		one.toRemote?.get("subtitle_brightness") shouldBe 60
		val other = m.merge(base, base, base + ("subtitle_brightness" to 60))
		other.toLocal shouldBe mapOf("subtitle_brightness" to 60)
		other.toRemote shouldBe null
		m.merge(base, base + ("subtitle_brightness" to 60), base + ("subtitle_brightness" to 60)).conflicts shouldBe emptyMap()
		val both = m.merge(base, base + ("subtitle_brightness" to 60), base + ("subtitle_brightness" to 80) + ("pref_rating_badges_enabled" to false))
		both.conflicts shouldBe mapOf("subtitle_brightness" to (60 to 80))
		both.toLocal shouldBe mapOf("pref_rating_badges_enabled" to false)
		both.base["subtitle_brightness"] shouldBe 100
	}

	test("no remote uploads local; no base treats every difference as a conflict") {
		m.merge(null, base, null).toRemote shouldBe base
		m.merge(null, base, base + ("subtitle_brightness" to 50)).conflicts.keys shouldBe setOf("subtitle_brightness")
	}

	test("resolving keeps this TV's or uses the synced values") {
		val remote = base + ("subtitle_brightness" to 80)
		val merge = m.merge(base, base + ("subtitle_brightness" to 60), remote)
		val keep = m.resolve(merge, remote, Resolution.KEEP_THIS_TV)
		keep.toRemote?.get("subtitle_brightness") shouldBe 60
		keep.toLocal shouldBe emptyMap()
		val use = m.resolve(merge, remote, Resolution.USE_SYNCED)
		use.toLocal shouldBe mapOf("subtitle_brightness" to 80)
		use.toRemote shouldBe null
		use.base["subtitle_brightness"] shouldBe 80
	}

	test("envelopes validate format, version, types and ranges, and ignore unknown keys") {
		val text = m.encode(SettingsSyncModel.Envelope(3, "tv", base + ("app_theme" to "DARK")))
		val decoded = m.decode(text)
		decoded.revision shouldBe 3
		decoded.settings shouldBe base
		m.decode(text.replace("\"subtitle_brightness\":100", "\"subtitle_brightness\":100,\"future_setting\":1")).settings shouldBe base
		shouldThrow<IllegalArgumentException> { m.decode(text.replace("\"subtitle_brightness\":100", "\"subtitle_brightness\":5")) }
		shouldThrow<IllegalArgumentException> { m.decode(text.replace("\"version\":1", "\"version\":2")) }
		shouldThrow<IllegalArgumentException> { m.decode(text.replace("lumafin-settings-sync", "lumafin-settings")) }
		shouldThrow<IllegalArgumentException> { m.decode("[".repeat(20)) }
	}
})

class SettingsSyncTests : FunSpec({
	fun setup(identity: () -> String? = { "server_user" }): Triple<SettingsSync, MemoryPreferences, FakeRemote> {
		val account = MemoryPreferences()
		val prefs = SwitchingAccountPreferences(MemoryPreferences(), account, emptySet())
		val remote = FakeRemote()
		val sync = SettingsSync(prefs, identity, { remote }, "writer", CoroutineScope(Dispatchers.Unconfined), now = { 1000L }, debounceMs = 0)
		return Triple(sync, account, remote)
	}

	test("sync stays off until enabled, and enabling with no synced copy uploads this TV's values") {
		val (sync, account, remote) = setup()
		sync.sync() shouldBe SettingsSync.Outcome.Disabled
		account.edit().putInt("subtitle_brightness", 70).commit()
		sync.enable() shouldBe SettingsSync.Outcome.Synced
		sync.state.value.enabled shouldBe true
		remote.settings()?.get("subtitle_brightness") shouldBe 70
		remote.settings()?.containsKey("media_card_size") shouldBe false
	}

	test("enabling against a different synced copy asks first and changes nothing until answered") {
		val (sync, account, remote) = setup()
		remote.text = SettingsSyncModel.encode(SettingsSyncModel.Envelope(4, "other", SettingsSyncModel.snapshot { null } + ("subtitle_brightness" to 40)))
		val outcome = sync.enable()
		outcome.shouldBeInstanceOf<SettingsSync.Outcome.NeedsReview>()
		sync.state.value.enabled shouldBe false
		account.getInt("subtitle_brightness", 100) shouldBe 100
		remote.writes shouldBe 0
		sync.resolve(outcome.review, Resolution.USE_SYNCED) shouldBe SettingsSync.Outcome.Synced
		account.getInt("subtitle_brightness", 100) shouldBe 40
		sync.state.value.enabled shouldBe true
		remote.writes shouldBe 0
	}

	test("replace on enable uploads this TV's values") {
		val (sync, account, remote) = setup()
		account.edit().putInt("subtitle_brightness", 90).commit()
		remote.text = SettingsSyncModel.encode(SettingsSyncModel.Envelope(4, "other", SettingsSyncModel.snapshot { null } + ("subtitle_brightness" to 40)))
		val review = (sync.enable() as SettingsSync.Outcome.NeedsReview).review
		sync.resolve(review, Resolution.KEEP_THIS_TV) shouldBe SettingsSync.Outcome.Synced
		remote.settings()?.get("subtitle_brightness") shouldBe 90
		SettingsSyncModel.decode(remote.text!!).revision shouldBe 5
	}

	test("two TVs merge changes to different settings without asking") {
		val (tvA, accountA, remote) = setup()
		tvA.enable()
		val accountB = MemoryPreferences()
		val tvB = SettingsSync(SwitchingAccountPreferences(MemoryPreferences(), accountB, emptySet()), { "server_user" }, { remote }, "b", CoroutineScope(Dispatchers.Unconfined))
		tvB.enable() shouldBe SettingsSync.Outcome.Synced
		accountA.edit().putInt("subtitle_brightness", 55).commit()
		accountB.edit().putBoolean("pref_rating_badges_enabled", false).commit()
		tvA.sync() shouldBe SettingsSync.Outcome.Synced
		tvB.sync() shouldBe SettingsSync.Outcome.Synced
		tvA.sync() shouldBe SettingsSync.Outcome.Synced
		accountB.getInt("subtitle_brightness", 100) shouldBe 55
		accountA.getBoolean("pref_rating_badges_enabled", true) shouldBe false
	}

	test("a real conflict is reviewed while other changes still sync") {
		val (tvA, accountA, remote) = setup()
		tvA.enable()
		val accountB = MemoryPreferences()
		val tvB = SettingsSync(SwitchingAccountPreferences(MemoryPreferences(), accountB, emptySet()), { "server_user" }, { remote }, "b", CoroutineScope(Dispatchers.Unconfined))
		tvB.enable()
		accountA.edit().putInt("subtitle_brightness", 60).commit()
		tvA.sync()
		accountB.edit().putInt("subtitle_brightness", 30).putBoolean("pref_rating_badges_enabled", false).commit()
		val outcome = tvB.sync()
		outcome.shouldBeInstanceOf<SettingsSync.Outcome.NeedsReview>()
		outcome.review.differences shouldBe mapOf("subtitle_brightness" to (30 to 60))
		remote.settings()?.get("pref_rating_badges_enabled") shouldBe false
		remote.settings()?.get("subtitle_brightness") shouldBe 60
		accountB.getInt("subtitle_brightness", 100) shouldBe 30
		tvB.resolve(outcome.review, Resolution.KEEP_THIS_TV) shouldBe SettingsSync.Outcome.Synced
		remote.settings()?.get("subtitle_brightness") shouldBe 30
		tvA.sync()
		accountA.getInt("subtitle_brightness", 100) shouldBe 30
	}

	test("a copy that changes between read and upload is merged again instead of overwritten") {
		val (sync, account, remote) = setup()
		sync.enable()
		account.edit().putInt("subtitle_brightness", 60).commit()
		var raced = false
		val original = remote.text
		remote.beforeWrite = null
		// Another TV writes after this sync's first read.
		val racing = object : SettingsSyncRemote {
			override suspend fun read(): String? {
				val text = remote.text
				if (!raced && text == original) {
					raced = true
					remote.text = SettingsSyncModel.encode(SettingsSyncModel.decode(text!!).let { it.copy(revision = it.revision + 1, settings = it.settings + ("pref_rating_badges_enabled" to false)) })
				}
				return text
			}
			override suspend fun write(text: String?) = remote.write(text)
		}
		val other = SettingsSync(SwitchingAccountPreferences(MemoryPreferences(), account, emptySet()), { "server_user" }, { racing }, "a", CoroutineScope(Dispatchers.Unconfined))
		other.sync() shouldBe SettingsSync.Outcome.Synced
		remote.settings()?.get("pref_rating_badges_enabled") shouldBe false
		remote.settings()?.get("subtitle_brightness") shouldBe 60
	}

	test("an account change during sync discards the result; signed out does nothing") {
		var identity: String? = "server_user"
		val account = MemoryPreferences()
		val remote = object : SettingsSyncRemote {
			override suspend fun read(): String? { identity = "server_other"; return null }
			override suspend fun write(text: String?) = error("must not write")
		}
		val sync = SettingsSync(SwitchingAccountPreferences(MemoryPreferences(), account, emptySet()), { identity }, { remote }, "a", CoroutineScope(Dispatchers.Unconfined))
		sync.enable() shouldBe SettingsSync.Outcome.AccountChanged
		identity = null
		sync.enable() shouldBe SettingsSync.Outcome.AccountChanged
	}

	test("a malformed synced copy fails without touching local settings or the server") {
		val (sync, account, remote) = setup()
		account.edit().putInt("subtitle_brightness", 70).commit()
		remote.text = "{\"format\":\"lumafin-settings-sync\",\"version\":1,\"revision\":1,\"writer\":\"x\",\"settings\":{\"subtitle_brightness\":\"bright\"}}"
		sync.enable().shouldBeInstanceOf<SettingsSync.Outcome.Failed>()
		account.getInt("subtitle_brightness", 0) shouldBe 70
		remote.writes shouldBe 0
		sync.state.value.enabled shouldBe false
	}

	test("disable keeps local values and the server copy; remove deletes the copy") {
		val (sync, account, remote) = setup()
		account.edit().putInt("subtitle_brightness", 70).commit()
		sync.enable()
		sync.disable()
		sync.state.value.enabled shouldBe false
		account.getInt("subtitle_brightness", 0) shouldBe 70
		(remote.text != null) shouldBe true
		sync.enable()
		sync.removeRemote() shouldBe SettingsSync.Outcome.Synced
		remote.text shouldBe null
		sync.state.value.enabled shouldBe false
	}
})
