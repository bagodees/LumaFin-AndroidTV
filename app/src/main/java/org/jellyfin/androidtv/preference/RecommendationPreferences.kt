package org.jellyfin.androidtv.preference

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository

class RecommendationPreferences(context: Context, private val servers: ServerRepository, private val users: UserRepository) {
	private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
	private val key: String?
		get() {
			val server = servers.currentServer.value?.id ?: return null
			val user = users.currentUser.value?.id ?: return null
			return "hidden_recommendations_${server}_$user"
		}
	val hiddenRows: Map<String, String>
		get() = key?.let { runCatching { Json.decodeFromString<Map<String, String>>(preferences.getString(it, "{}") ?: "{}") }.getOrDefault(emptyMap()) }.orEmpty()
	fun hide(id: String, name: String) = save(hiddenRows + (id to name))
	fun restore(id: String) = save(hiddenRows - id)
	private fun save(rows: Map<String, String>) {
		val storageKey = key ?: return
		preferences.edit().putString(storageKey, Json.encodeToString(rows)).apply()
	}
}
