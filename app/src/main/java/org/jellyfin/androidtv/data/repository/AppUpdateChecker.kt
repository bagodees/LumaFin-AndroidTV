package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks up the latest LumaFin release from the update feed published by getlumafin.com.
 * The app is sideloaded, so without this users would never hear about new versions.
 */
class AppUpdateChecker {
	companion object {
		private const val VERSION_FEED_URL = "https://getlumafin.com/version.json"
		private const val TIMEOUT_MS = 10_000

		/**
		 * Whether [latest] is a newer version than [current]. Versions are compared by their
		 * numeric major.minor.patch core; a stable release is newer than a pre-release
		 * (e.g. "-beta.1") with the same core.
		 */
		fun isNewer(latest: String, current: String): Boolean {
			fun core(version: String) = version.removePrefix("v")
				.substringBefore('-')
				.split('.')
				.map { it.toIntOrNull() ?: 0 }
				.let { parts -> List(3) { parts.getOrElse(it) { 0 } } }

			val latestCore = core(latest)
			val currentCore = core(current)
			for (i in 0 until 3) {
				if (latestCore[i] != currentCore[i]) return latestCore[i] > currentCore[i]
			}

			return '-' in current && '-' !in latest
		}
	}

	/** The latest Android TV version, or null when the feed can't be reached or read. */
	suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
		runCatching {
			val connection = URL(VERSION_FEED_URL).openConnection() as HttpURLConnection
			try {
				connection.connectTimeout = TIMEOUT_MS
				connection.readTimeout = TIMEOUT_MS
				if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null

				val body = connection.inputStream.bufferedReader().use { it.readText() }
				Json.parseToJsonElement(body)
					.jsonObject["android_tv"]
					?.jsonObject?.get("version")
					?.jsonPrimitive?.content
			} finally {
				connection.disconnect()
			}
		}.onFailure { Timber.w(it, "Unable to check for app updates") }.getOrNull()
	}
}
