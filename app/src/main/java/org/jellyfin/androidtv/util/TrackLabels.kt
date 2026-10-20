package org.jellyfin.androidtv.util

import android.content.Context
import androidx.core.os.ConfigurationCompat
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.Locale

internal data class TrackLabelText(
	val unknown: String, val mono: String, val stereo: String, val channels: String,
	val forced: String, val hearingImpaired: String, val default: String, val track: String,
)

private val languageCodes by lazy {
	Locale.getISOLanguages().associateBy { Locale(it).isO3Language } +
		mapOf("fre" to "fr", "ger" to "de", "chi" to "zh", "dut" to "nl", "cze" to "cs", "gre" to "el", "rum" to "ro")
}

// Exact container-generated names only: never discard descriptive or commentary titles.
private fun String.isGenericTrackTitle(): Boolean = trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) in setOf(
	"apple sound media handler", "soundhandler", "sound handler",
)

internal fun MediaStream.readableTrackLabel(locale: Locale, text: TrackLabelText): String {
	val code = language?.trim()?.lowercase(Locale.ROOT).orEmpty()
	val languageLocale = Locale.forLanguageTag(languageCodes[code] ?: code.replace('_', '-'))
	val languageName = languageLocale.getDisplayName(locale).takeIf {
		code.isNotBlank() && code != "und" && it.isNotBlank() && !it.equals(code, true)
	}
	val parts = mutableListOf(languageName ?: text.unknown)
	val customTitle = title?.trim()?.takeIf {
		it.isNotEmpty() && !it.isGenericTrackTitle() && !it.equals(code, true) && !it.equals(languageName, true)
	}
	if (customTitle != null) parts += customTitle
	if (type == MediaStreamType.AUDIO) {
		parts += when {
			channels == 1 -> text.mono
			channels == 2 -> text.stereo
			!channelLayout.isNullOrBlank() -> channelLayout!!
			channels != null && channels!! > 0 -> String.format(locale, text.channels, channels)
			else -> ""
		}
	}
	codec?.takeIf { it.isNotBlank() }?.let {
		parts += when (it.lowercase(Locale.ROOT)) {
			"ac3" -> "Dolby Digital"
			"eac3" -> "Dolby Digital Plus"
			"truehd" -> "Dolby TrueHD"
			"subrip" -> "SRT"
			else -> it.uppercase(Locale.ROOT)
		}
	}
	if (isForced) parts += text.forced
	if (isHearingImpaired) parts += text.hearingImpaired
	if (isDefault) parts += text.default
	// Preserve a server label when structured metadata cannot identify the track.
	if (languageName == null && customTitle == null && codec.isNullOrBlank()) {
		displayTitle?.takeIf { it.isNotBlank() && !it.isGenericTrackTitle() }?.let { parts[0] = it }
	}
	return parts.filter { it.isNotBlank() }.distinct().joinToString(" · ")
}

internal fun List<MediaStream>.readableTrackLabels(locale: Locale, text: TrackLabelText): Map<Int, String> {
	val labels = associate { it.index to it.readableTrackLabel(locale, text) }
	val duplicates = labels.values.groupingBy { it }.eachCount()
	return labels.mapValues { (index, label) ->
		if (duplicates.getValue(label) > 1) "$label · ${String.format(locale, text.track, index + 1)}" else label
	}
}

fun List<MediaStream>.trackLabels(context: Context): Map<Int, String> = readableTrackLabels(
	ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault(),
	TrackLabelText(
		context.getString(R.string.track_unknown_language), context.getString(R.string.track_mono),
		context.getString(R.string.track_stereo), context.getString(R.string.track_channels),
		context.getString(R.string.track_forced), context.getString(R.string.track_hearing_impaired),
		context.getString(R.string.track_default), context.getString(R.string.track_number),
	),
)

/** Compact details chip; full track titles, codecs and flags remain in the picker. */
fun MediaStream.audioTrackSummary(context: Context): String = listOf(copy(
	title = null, displayTitle = null, codec = null,
	isDefault = false, isForced = false, isHearingImpaired = false,
)).trackLabels(context).getValue(index)
