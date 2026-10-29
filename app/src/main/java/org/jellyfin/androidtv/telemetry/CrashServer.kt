package org.jellyfin.androidtv.telemetry

import android.content.Context
import android.os.Build
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.acra.sender.ReportSenderFactory
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.util.isLowMemoryDevice
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.util.UUID

/**
 * Sends crash and exit reports to LumaFin's own crash server, so crashes from test builds can be
 * seen without reading each tester's Jellyfin server log. Only builds made with a crash server
 * address (GitHub test builds) send anything; store builds have none. Server addresses and
 * tokens are removed from logs before they leave the device, and devices are told apart only by
 * a random id created on the device.
 */
object CrashServer {
	private const val KEY_INSTALL_ID = "crash_server_install_id"
	private const val CONNECT_TIMEOUT_MS = 10_000
	private const val READ_TIMEOUT_MS = 15_000

	private class Dsn(val storeUrl: String, val publicKey: String)

	private val dsn: Dsn? by lazy {
		val value = BuildConfig.CRASH_SERVER_DSN
		if (value.isBlank() || !BuildConfig.VERSION_NAME.contains("-test")) return@lazy null
		runCatching {
			val uri = URI(value)
			val projectId = uri.path.trim('/')
			val port = if (uri.port != -1) ":${uri.port}" else ""
			Dsn("${uri.scheme}://${uri.host}$port/api/$projectId/store/", uri.userInfo)
		}.getOrNull()
	}

	fun isEnabled(context: Context): Boolean = dsn != null && preferences(context).getBoolean(ACRA.PREF_ENABLE_ACRA, true)

	private fun preferences(context: Context) =
		context.getSharedPreferences(TelemetryPreferences.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

	private fun installId(context: Context): String {
		val preferences = preferences(context)
		return preferences.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
			preferences.edit().putString(KEY_INSTALL_ID, it).apply()
		}
	}

	/** Sends a crash the app caught itself, from its stack trace. */
	fun sendCrash(context: Context, stackTrace: String, logcat: String?, crashTime: String?) {
		val event = baseEvent(context, level = "fatal").apply {
			put("exception", JSONObject().put("values", parseExceptions(stackTrace)))
			put("extra", JSONObject().apply {
				put("stack_trace", scrub(stackTrace))
				if (logcat != null) put("logcat", scrub(logcat))
				if (crashTime != null) put("crash_time", crashTime)
			})
		}
		post(event)
	}

	/** Sends an exit the system caused (low memory, a native crash, not responding). */
	fun sendExit(
		context: Context,
		reason: String,
		description: String?,
		timestamp: Long,
		details: Map<String, String>,
		trace: String?,
		logcat: String?,
	) {
		val event = baseEvent(context, level = "fatal").apply {
			put("timestamp", Instant.ofEpochMilli(timestamp).toString())
			put("message", JSONObject().put("formatted", "App ended by the system: $reason"))
			put("fingerprint", JSONArray().put("system-exit").put(reason))
			getJSONObject("tags").put("exit_reason", reason)
			put("extra", JSONObject().apply {
				put("description", description ?: "none")
				details.forEach { (key, value) -> put(key, value) }
				if (trace != null) put("system_trace", scrub(trace))
				if (logcat != null) put("logcat", scrub(logcat))
			})
		}
		post(event)
	}

	private fun baseEvent(context: Context, level: String) = JSONObject().apply {
		put("event_id", UUID.randomUUID().toString().replace("-", ""))
		put("timestamp", Instant.now().toString())
		put("platform", "java")
		put("level", level)
		put("logger", "lumafin")
		put("release", "lumafin-androidtv@${BuildConfig.VERSION_NAME}")
		put("environment", "test")
		put("user", JSONObject().put("id", installId(context)))
		put("tags", JSONObject().apply {
			put("device_model", Build.MODEL)
			put("device_brand", Build.BRAND)
			put("android", Build.VERSION.RELEASE)
			put("low_memory_device", context.isLowMemoryDeviceSafe().toString())
		})
		put("contexts", JSONObject().apply {
			put("device", JSONObject().apply {
				put("type", "device")
				put("brand", Build.BRAND)
				put("model", Build.MODEL)
				put("manufacturer", Build.MANUFACTURER)
			})
			put("os", JSONObject().apply {
				put("type", "os")
				put("name", "Android")
				put("version", Build.VERSION.RELEASE)
				put("build", Build.DISPLAY)
			})
			put("app", JSONObject().apply {
				put("type", "app")
				put("app_version", BuildConfig.VERSION_NAME)
				put("app_build", BuildConfig.VERSION_CODE.toString())
			})
		})
	}

	private fun Context.isLowMemoryDeviceSafe() = runCatching { isLowMemoryDevice() }.getOrDefault(false)

	private fun post(event: JSONObject) {
		val dsn = dsn ?: return
		val connection = URL(dsn.storeUrl).openConnection() as HttpURLConnection
		try {
			connection.connectTimeout = CONNECT_TIMEOUT_MS
			connection.readTimeout = READ_TIMEOUT_MS
			connection.requestMethod = "POST"
			connection.doOutput = true
			connection.setRequestProperty("Content-Type", "application/json")
			connection.setRequestProperty(
				"X-Sentry-Auth",
				"Sentry sentry_version=7, sentry_client=lumafin-androidtv/${BuildConfig.VERSION_NAME}, sentry_key=${dsn.publicKey}",
			)
			connection.outputStream.use { it.write(event.toString().toByteArray()) }
			val code = connection.responseCode
			if (code !in 200..299) throw ReportSenderException("Crash server answered $code")
		} finally {
			connection.disconnect()
		}
	}

	private val FRAME = Regex("""^\s*at ([\w$.<>\-]+)\.([\w$<>\-]+)\((.*)\)\s*$""")

	/** Turns a Java stack trace (with its "Caused by" chain) into the crash server's exception list, oldest cause first. */
	internal fun parseExceptions(stackTrace: String): JSONArray {
		val blocks = mutableListOf<Pair<String, MutableList<String>>>()
		for (line in stackTrace.lineSequence()) {
			val header = when {
				blocks.isEmpty() && line.isNotBlank() -> line
				line.startsWith("Caused by: ") -> line.removePrefix("Caused by: ")
				else -> null
			}
			if (header != null) blocks += header to mutableListOf()
			else if (FRAME.matches(line)) blocks.lastOrNull()?.second?.add(line)
		}

		val values = JSONArray()
		for ((header, lines) in blocks.asReversed()) {
			val type = header.substringBefore(':').trim()
			val value = header.substringAfter(':', "").trim()
			val frames = JSONArray()
			// The crash server lists frames oldest call first
			for (line in lines.asReversed()) {
				val match = FRAME.matchEntire(line) ?: continue
				val (module, function, location) = match.destructured
				frames.put(JSONObject().apply {
					put("module", module)
					put("function", function)
					put("filename", location.substringBefore(':'))
					location.substringAfter(':', "").toIntOrNull()?.let { put("lineno", it) }
					put("in_app", module.startsWith("org.jellyfin") || module.startsWith("io.github.bagodees"))
				})
			}
			values.put(JSONObject().apply {
				put("type", type)
				put("value", scrub(value))
				if (frames.length() > 0) put("stacktrace", JSONObject().put("frames", frames))
			})
		}
		return values
	}

	private val URL_HOST = Regex("""(https?|wss?)://[^\s/"'<>]+""", RegexOption.IGNORE_CASE)
	private val SECRET_PARAM = Regex("""((?:api_?key|access_?token|token|x-emby-token|x-mediabrowser-token|deviceid|device_id)["']?\s*[=:]\s*["']?)[^\s&"',;]+""", RegexOption.IGNORE_CASE)
	private val AUTH_HEADER = Regex("""(authorization\s*[:=]\s*).*""", RegexOption.IGNORE_CASE)
	private val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")

	/** Removes server addresses, IP addresses and tokens from text before it's sent. */
	internal fun scrub(text: String): String = text
		.replace(AUTH_HEADER) { it.groupValues[1] + "[removed]" }
		.replace(SECRET_PARAM) { it.groupValues[1] + "[removed]" }
		.replace(URL_HOST) { it.groupValues[1] + "://[server]" }
		.replace(IPV4, "[ip]")

	class SenderFactory : ReportSenderFactory {
		override fun create(context: Context, config: CoreConfiguration): ReportSender = Sender()
		override fun enabled(config: CoreConfiguration): Boolean = dsn != null
	}

	class Sender : ReportSender {
		override fun send(context: Context, errorContent: CrashReportData) = try {
			val includeLogs = preferences(context).getBoolean(TelemetryPreferences.crashReportIncludeLogs.key, true)
			sendCrash(
				context = context,
				stackTrace = errorContent.getString(ReportField.STACK_TRACE).orEmpty(),
				logcat = if (includeLogs) errorContent.getString(ReportField.LOGCAT) else null,
				crashTime = errorContent.getString(ReportField.USER_CRASH_DATE),
			)
		} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
			throw ReportSenderException("Unable to send crash report to the crash server", e)
		}
	}
}
