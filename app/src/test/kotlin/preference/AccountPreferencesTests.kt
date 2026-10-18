package org.jellyfin.androidtv.preference

import android.content.SharedPreferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID
import android.content.Context
import io.mockk.mockk
import org.jellyfin.androidtv.constant.HomeSectionType

class AccountPreferencesTests : FunSpec({
	val serverA = UUID(0, 1)
	val serverB = UUID(0, 2)
	val userA = UUID(0, 3)
	val userB = UUID(0, 4)

	test("setup presentation survives restart separately for each user and server") {
		val legacy = MemoryPreferences()
		val files = mutableMapOf<String, MemoryPreferences>()
		fun create() = AccountPreferences(legacy, { files.getOrPut(it) { MemoryPreferences() } })
		val accounts = create()
		val prefs = UserPreferences(mockk<Context>(), accounts.preferences)
		accounts.select(serverA, userA)
		prefs[UserPreferences.setupGuideSeen] shouldBe false
		prefs[UserPreferences.setupGuideSeen] = true
		accounts.select(serverA, userB)
		prefs[UserPreferences.setupGuideSeen] shouldBe false
		accounts.select(serverB, userA)
		prefs[UserPreferences.setupGuideSeen] shouldBe false
		val recreated = create()
		recreated.select(serverA, userA)
		UserPreferences(mockk<Context>(), recreated.preferences)[UserPreferences.setupGuideSeen] shouldBe true
	}

	test("presenting or skipping setup changes only its local seen flag") {
		val accounts = AccountPreferences(MemoryPreferences(), { MemoryPreferences() })
		accounts.select(serverA, userA)
		val prefs = UserPreferences(mockk<Context>(), accounts.preferences)
		prefs[UserPreferences.subtitleBrightness] = 60
		prefs[UserPreferences.mediaQueuingEnabled] = false
		val before = accounts.preferences.all
		prefs[UserPreferences.setupGuideSeen] = true
		accounts.preferences.all shouldBe before + (UserPreferences.setupGuideSeen.key to true)
	}

	test("legacy preferences migrate once and user/server settings survive recreation independently") {
		val legacy = MemoryPreferences()
		legacy.edit().putString("homeSectionOrder", "resume,nextup").putInt("skipBackLength", 15000)
			.putString("access_token", "must-not-copy").commit()
		val files = mutableMapOf<String, MemoryPreferences>()
		fun create() = AccountPreferences(legacy, { files.getOrPut(it) { MemoryPreferences() } })
		val accounts = create()
		accounts.select(serverA, userA)
		accounts.preferences.getInt("skipBackLength", 10000) shouldBe 15000
		accounts.preferences.contains("access_token") shouldBe false
		accounts.preferences.edit().putInt("skipBackLength", 20000).apply()
		accounts.select(serverA, userB)
		accounts.preferences.getInt("skipBackLength", 10000) shouldBe 10000
		accounts.preferences.contains("homeSectionOrder") shouldBe false
		accounts.preferences.edit().putInt("skipBackLength", 5000).apply()
		accounts.select(serverB, userA)
		accounts.preferences.getInt("skipBackLength", 10000) shouldBe 10000
		val recreated = create()
		recreated.select(serverA, userA)
		recreated.preferences.getInt("skipBackLength", 10000) shouldBe 20000
		recreated.select(serverA, userB)
		recreated.preferences.getInt("skipBackLength", 10000) shouldBe 5000
	}

	test("an editor created before a switch cannot write into the next account") {
		val files = mutableMapOf<String, MemoryPreferences>()
		val accounts = AccountPreferences(MemoryPreferences(), { files.getOrPut(it) { MemoryPreferences() } })
		accounts.select(serverA, userA)
		val pending = accounts.preferences.edit().putString("streaming_quality", "CUSTOM")
		accounts.select(serverA, userB)
		pending.apply()
		accounts.preferences.getString("streaming_quality", "ORIGINAL") shouldBe "ORIGINAL"
		accounts.select(serverA, userA)
		accounts.preferences.getString("streaming_quality", "ORIGINAL") shouldBe "CUSTOM"
	}
	test("real home and playback stores read the selected account rather than retaining prior defaults") {
		val files = mutableMapOf<String, MemoryPreferences>()
		val accounts = AccountPreferences(MemoryPreferences(), { files.getOrPut(it) { MemoryPreferences() } })
		val context = mockk<Context>()
		val playback = UserPreferences(context, accounts.preferences)
		val home = UserSettingPreferences(context, accounts.preferences)
		accounts.select(serverA, userA)
		home.activeHomesections = listOf(HomeSectionType.RESUME)
		playback[UserPreferences.subtitleBrightness] = 55
		home[UserSettingPreferences.latestMoviesItemLimit] = 20
		accounts.select(serverA, userB)
		home.activeHomesections shouldBe UserSettingPreferences.defaultHomeSectionOrder
		playback[UserPreferences.subtitleBrightness] shouldBe 100
		home[UserSettingPreferences.latestMoviesItemLimit] shouldBe 50
		accounts.select(serverA, userA)
		home.activeHomesections shouldBe listOf(HomeSectionType.RESUME)
		playback[UserPreferences.subtitleBrightness] shouldBe 55
		home[UserSettingPreferences.latestMoviesItemLimit] shouldBe 20
	}

	test("device capability overrides stay shared while user values and sign-out remain isolated") {
		val legacy = MemoryPreferences()
		val files = mutableMapOf<String, MemoryPreferences>()
		val accounts = AccountPreferences(legacy, { files.getOrPut(it) { MemoryPreferences() } })
		accounts.select(serverA, userA)
		accounts.preferences.edit().putString("hdr10_override", "DISABLE").putInt("subtitle_brightness", 60).apply()
		accounts.select(serverA, userB)
		accounts.preferences.getString("hdr10_override", "AUTO") shouldBe "DISABLE"
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 100
		accounts.select(null, null)
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 100
		accounts.preferences.edit().putInt("subtitle_brightness", 50).apply()
		accounts.select(serverA, userA)
		accounts.preferences.getInt("subtitle_brightness", 100) shouldBe 60
		legacy.getString("hdr10_override", "AUTO") shouldBe "DISABLE"
	}

	test("listeners refresh on switching and ignore late notifications from an old account") {
		val old = MemoryPreferences()
		val next = MemoryPreferences()
		val preferences = SwitchingAccountPreferences(MemoryPreferences(), old, emptySet())
		val events = mutableListOf<String?>()
		preferences.registerOnSharedPreferenceChangeListener { _, key -> events.add(key) }
		preferences.select(next)
		old.edit().putInt("subtitle_brightness", 30).apply()
		next.edit().putInt("subtitle_brightness", 80).apply()
		events shouldBe listOf(null, "subtitle_brightness")
	}
})

/** Android-compatible buffered editor semantics, including listener notification after apply. */
internal class MemoryPreferences : SharedPreferences {
	var failNextCommit = false
	private val values = mutableMapOf<String, Any?>()
	private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
	override fun getAll(): Map<String, *> = values.toMap()
	override fun contains(key: String?) = values.containsKey(key)
	override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
	@Suppress("UNCHECKED_CAST")
	override fun getStringSet(key: String?, defValues: Set<String>?) = values[key] as? Set<String> ?: defValues
	override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
	override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
	override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
	override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
	override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.add(listener) }
	override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) { listeners.remove(listener) }
	override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
		private val pending = mutableMapOf<String, Any?>()
		private var clear = false
		private fun put(key: String?, value: Any?) = apply { pending[requireNotNull(key)] = value }
		override fun putString(key: String?, value: String?) = put(key, value)
		override fun putStringSet(key: String?, values: Set<String>?) = put(key, values?.toSet())
		override fun putInt(key: String?, value: Int) = put(key, value)
		override fun putLong(key: String?, value: Long) = put(key, value)
		override fun putFloat(key: String?, value: Float) = put(key, value)
		override fun putBoolean(key: String?, value: Boolean) = put(key, value)
		override fun remove(key: String?) = put(key, null)
		override fun clear() = apply { clear = true }
		override fun commit(): Boolean {
			if (clear) values.clear()
			pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
			pending.keys.forEach { key -> listeners.toList().forEach { it.onSharedPreferenceChanged(this@MemoryPreferences, key) } }
			pending.clear()
			if (failNextCommit) { failNextCommit = false; return false }
			return true
		}
		override fun apply() { commit() }
	}
}
