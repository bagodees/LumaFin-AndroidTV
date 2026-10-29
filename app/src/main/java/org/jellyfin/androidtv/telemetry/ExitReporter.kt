package org.jellyfin.androidtv.telemetry

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.acra.ACRA
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.util.appendCodeBlock
import org.jellyfin.androidtv.util.appendItem
import org.jellyfin.androidtv.util.appendSection
import org.jellyfin.androidtv.util.appendValue
import org.jellyfin.androidtv.util.buildMarkdown
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Crash reports only cover crashes the app catches itself. When the system ends the app instead
 * (a crash in a native decoder, a freeze long enough to be closed as not responding, or running
 * out of memory) nothing is sent. Android 11 and newer keep a record of why the app last closed;
 * on the next start this sends any such exit to the server like a crash report, with the
 * system's trace where it has one and the app's last log lines before it ended.
 */
object ExitReporter {
	private const val KEY_LAST_REPORTED = "last_reported_exit"
	private const val MAX_TRACE_CHARS = 120_000
	private const val LOG_LINES = 400

	// The first start with this feature also sends exits from the day before, so a crash seen
	// on the previous build still reaches the server
	private val FIRST_RUN_WINDOW_MS = TimeUnit.DAYS.toMillis(1)

	fun reportPreviousExits(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
		val appContext = context.applicationContext
		thread(name = "exit-reporter", isDaemon = true) {
			runCatching { report(appContext) }.onFailure { Timber.w(it, "Couldn't report the previous exit") }
		}
	}

	@RequiresApi(Build.VERSION_CODES.R)
	private fun report(context: Context) {
		val preferences = context.getSharedPreferences(TelemetryPreferences.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
		if (!preferences.getBoolean(ACRA.PREF_ENABLE_ACRA, true)) return
		val url = preferences.getString(TelemetryPreferences.crashReportUrl.key, null)
		val token = preferences.getString(TelemetryPreferences.crashReportToken.key, null)
		if (url.isNullOrBlank() || token.isNullOrBlank()) return
		val includeLogs = preferences.getBoolean(TelemetryPreferences.crashReportIncludeLogs.key, true)

		val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
		val exits = activityManager.getHistoricalProcessExitReasons(context.packageName, 0, 10)
			.filter { it.processName == context.packageName }
		val lastReported = preferences.getLong(KEY_LAST_REPORTED, 0L)
		val since = if (lastReported == 0L) System.currentTimeMillis() - FIRST_RUN_WINDOW_MS else lastReported
		// Only while the app was on screen; Fire OS often ends apps in the background to free memory
		val toReport = exits.filter { it.timestamp > since && it.reason in REPORTED_REASONS && it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE }.sortedBy { it.timestamp }.takeLast(3)

		// Remembered before sending, so a report that fails to send isn't retried on every start
		exits.maxOfOrNull { it.timestamp }?.let { newest ->
			if (newest > lastReported) preferences.edit().putLong(KEY_LAST_REPORTED, newest).apply()
		}

		for (exit in toReport) {
			val report = buildReport(exit, if (includeLogs) logLines(exit.pid) else null)
			runCatching { TelemetryService.postReport(url, token, report) }
				.onSuccess { Timber.i("Reported the previous exit (%s) to the server", reasonName(exit.reason)) }
				.onFailure { Timber.w(it, "Couldn't send the exit report") }
		}
	}

	private val REPORTED_REASONS = setOf(
		ApplicationExitInfo.REASON_CRASH_NATIVE,
		ApplicationExitInfo.REASON_ANR,
		ApplicationExitInfo.REASON_LOW_MEMORY,
		ApplicationExitInfo.REASON_SIGNALED,
		ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
		ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
		ApplicationExitInfo.REASON_UNKNOWN,
	)

	private fun reasonName(reason: Int) = when (reason) {
		ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
		ApplicationExitInfo.REASON_ANR -> "not responding (ANR)"
		ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
		ApplicationExitInfo.REASON_SIGNALED -> "killed by a signal"
		ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource use"
		ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization failure"
		ApplicationExitInfo.REASON_UNKNOWN -> "unknown"
		else -> "reason $reason"
	}

	/**
	 * The system's trace when it's text, like the dump of a freeze (ANR). Native crash tombstones
	 * from Android 12 on are binary and would only bury the log, so those are left out.
	 */
	@RequiresApi(Build.VERSION_CODES.R)
	private fun trace(exit: ApplicationExitInfo): String? = runCatching {
		exit.traceInputStream?.use { stream ->
			val text = stream.bufferedReader().readText()
			val unreadable = text.count { it == '�' || (it < ' ' && it !in "\n\r\t") }
			when {
				unreadable > text.length / 50 -> null
				text.length > MAX_TRACE_CHARS -> text.take(MAX_TRACE_CHARS) + "\n[trimmed]"
				else -> text
			}
		}
	}.getOrNull()

	/**
	 * The app's log lines from the process that ended, while they're still in the system's buffer.
	 * Android 13 and newer ask the user before an app reads the log, so there the report goes without.
	 */
	@RequiresApi(Build.VERSION_CODES.R)
	private fun logLines(pid: Int): String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) null else runCatching {
		val process = ProcessBuilder("logcat", "-d", "-v", "time", "--pid=$pid", "-t", LOG_LINES.toString())
			.redirectErrorStream(true)
			.start()
		val text = process.inputStream.bufferedReader().readText()
		process.waitFor(5, TimeUnit.SECONDS)
		text.takeIf { it.isNotBlank() }
	}.getOrNull()

	@RequiresApi(Build.VERSION_CODES.R)
	private fun buildReport(exit: ApplicationExitInfo, logs: String?): String = buildMarkdown {
		appendLine("---")
		appendLine("client: LumaFin for Android TV")
		appendLine("client_version: ${BuildConfig.VERSION_NAME}")
		appendLine("client_repository: https://github.com/bagodees/LumaFin-Android")
		appendLine("type: crash_report")
		appendLine("format: markdown")
		appendLine("---")
		appendLine()

		appendSection("App ended by the system") {
			appendItem("Reason") { appendValue(reasonName(exit.reason)) }
			appendItem("Description") { appendValue(exit.description ?: "none") }
			appendItem("Status") { appendValue(exit.status.toString()) }
			appendItem("Time") { appendValue(Instant.ofEpochMilli(exit.timestamp).toString()) }
			appendItem("Was in the foreground") { appendValue((exit.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND).toString()) }
			appendItem("Memory (PSS / RSS, KB)") { appendValue("${exit.pss} / ${exit.rss}") }
		}

		appendSection("Logs") {
			appendItem("Logcat") {
				if (logs != null) appendCodeBlock("log", logs) else append("Logs are disabled or no longer available")
			}
			appendItem("System trace") {
				val trace = trace(exit)
				if (trace != null) appendCodeBlock("log", trace) else append("None in text form for this kind of exit")
			}
		}

		appendSection("App information") {
			appendItem("App version") { appendValue("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") }
		}

		appendSection("Device information") {
			appendItem("Android version") { appendValue(Build.VERSION.RELEASE) }
			appendItem("Device brand") { appendValue(Build.BRAND) }
			appendItem("Device model") { appendValue(Build.MODEL) }
		}
	}
}
