package org.jellyfin.androidtv.diagnostics

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.BufferLength
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import org.jellyfin.androidtv.preference.constant.StreamingQuality

enum class DiagnosticProblem(val label: Int) {
	CONNECTION(R.string.export_problem_connection), BUFFERING(R.string.export_problem_buffering),
	VIDEO(R.string.export_problem_video), AUDIO(R.string.export_problem_audio), SUBTITLES(R.string.pref_subtitles), OTHER(R.string.export_problem_other)
}

enum class DiagnosticTrigger(val label: Int) {
	START(R.string.export_trigger_start), PLAYING(R.string.export_trigger_playing), SEEK(R.string.export_trigger_seek),
	RESUME(R.string.export_trigger_resume), BROWSING(R.string.export_trigger_browsing)
}

data class DiagnosticSection(val heading: String, val body: String)

/** Only allowlisted, typed settings enter this report. Never accept logs, URLs, device IDs or account objects. */
data class DiagnosticSnapshot(
	val appVersion: String,
	val androidApi: Int,
	val serverVersion: String?,
	val networkConnected: Boolean?,
	val networkValidated: Boolean?,
	val encryptedServerAddress: Boolean?,
	val quality: StreamingQuality,
	val customBitrateMbps: Int?,
	val buffer: BufferLength,
	val externalPlayer: Boolean,
	val audio: AudioBehavior,
	val refreshRate: RefreshRateSwitchingBehavior,
	val softwareCodecs: Boolean,
	val burnSubtitles: Boolean,
	val avcDecoder: Boolean?,
	val hevcDecoder: Boolean?,
	val av1Decoder: Boolean?,
)

// Reject the whole field on unexpected data, rather than trying to scrub arbitrary server-controlled text.
internal fun diagnosticVersion(value: String?): String = value?.takeIf {
	it.length <= 48 && Regex("[0-9]{1,4}(?:\\.[0-9]{1,4}){1,3}(?:-(?:dev|debug|beta|rc)(?:\\.[0-9]{1,8})?)?").matches(it)
} ?: "Unavailable (omitted)"

fun diagnosticReport(snapshot: DiagnosticSnapshot, problem: DiagnosticProblem, trigger: DiagnosticTrigger): List<DiagnosticSection> {
	fun Boolean?.status() = when (this) { true -> "Yes"; false -> "No"; null -> "Unavailable" }
	val bitrate = if (snapshot.quality == StreamingQuality.CUSTOM)
		snapshot.customBitrateMbps?.takeIf { it in 1..1000 }?.toString() ?: "Unavailable" else "Not used"
	return listOf(
		DiagnosticSection("LumaFin diagnostic report v1", "This report contains selected technical settings only. No raw logs, names, account/server/device IDs, addresses, media titles, file paths, credentials or tokens are collected. Nothing is uploaded automatically."),
		DiagnosticSection("Problem context", "Problem: ${problem.name}\nOccurs during: ${trigger.name}\nAdd exact steps, expected behavior and actual behavior to the saved file before sharing. Avoid private details and credentials."),
		DiagnosticSection("Versions", "App: ${diagnosticVersion(snapshot.appVersion)}\nAndroid API: ${snapshot.androidApi.coerceIn(1, 100)}\nJellyfin: ${diagnosticVersion(snapshot.serverVersion)}"),
		DiagnosticSection("Connection snapshot", "Active network: ${snapshot.networkConnected.status()}\nAndroid reports internet validated: ${snapshot.networkValidated.status()}\nServer address uses HTTPS: ${snapshot.encryptedServerAddress.status()}\nThis does not test server reachability, certificate validity or connection speed. No network addresses or Wi-Fi names are included."),
		DiagnosticSection("Playback settings", "Quality: ${snapshot.quality.name}\nCustom limit (Mbit/s): $bitrate\nBuffer preset: ${snapshot.buffer.name}\nExternal player: ${snapshot.externalPlayer.status()}\nAudio output: ${snapshot.audio.name}\nRefresh rate switching: ${snapshot.refreshRate.name}\nSoftware codecs allowed: ${snapshot.softwareCodecs.status()}\nBurn subtitles during transcoding: ${snapshot.burnSubtitles.status()}"),
		DiagnosticSection("Platform decoder support", "AVC/H.264 advertised: ${snapshot.avcDecoder.status()}\nHEVC/H.265 advertised: ${snapshot.hevcDecoder.status()}\nAV1 advertised: ${snapshot.av1Decoder.status()}\nAdvertised support does not guarantee a particular profile, HDR format, resolution or audio passthrough. Overrides, the selected player, display and receiver also affect playback."),
		DiagnosticSection("Metric limitations", "Live buffer, frame and stream metrics are not captured from this settings screen. View Playback information while playing for metrics supported by that player. Use Test streaming speed for a separate server-to-TV sample; neither is a playback guarantee. This export does not start playback or a network test."),
	)
}

fun List<DiagnosticSection>.diagnosticText() = joinToString("\n\n") { "${it.heading}\n${it.body}" } + "\n"
