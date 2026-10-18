package org.jellyfin.androidtv.diagnostics

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.UserPreferences
import java.io.IOException

fun collectDiagnosticSnapshot(context: Context, preferences: UserPreferences, serverVersion: String?, encrypted: Boolean?): DiagnosticSnapshot {
	val network = runCatching {
		val manager = context.getSystemService(ConnectivityManager::class.java)
		manager.getNetworkCapabilities(manager.activeNetwork)
	}
	val codecs = runCatching { MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filterNot { it.isEncoder } }
	fun decoder(mime: String) = codecs.getOrNull()?.any { codec -> codec.supportedTypes.any { it.equals(mime, true) } }
	return DiagnosticSnapshot(
		appVersion = BuildConfig.VERSION_NAME, androidApi = Build.VERSION.SDK_INT, serverVersion = serverVersion,
		networkConnected = if (network.isSuccess) network.getOrNull() != null else null,
		networkValidated = if (network.isSuccess) network.getOrNull()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false else null,
		encryptedServerAddress = encrypted,
		quality = preferences[UserPreferences.streamingQuality],
		customBitrateMbps = preferences[UserPreferences.maxBitrate].toIntOrNull(),
		buffer = preferences[UserPreferences.bufferLength], externalPlayer = preferences[UserPreferences.useExternalPlayer],
		audio = preferences[UserPreferences.audioBehaviour], refreshRate = preferences[UserPreferences.refreshRateSwitchingBehavior],
		softwareCodecs = preferences[UserPreferences.softwareCodecsEnabled], burnSubtitles = preferences[UserPreferences.subtitlesBurnDuringTranscode],
		avcDecoder = decoder("video/avc"), hevcDecoder = decoder("video/hevc"), av1Decoder = decoder("video/av01"),
	)
}

fun writeDiagnosticExport(resolver: ContentResolver, uri: Uri, text: String) {
	val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
	stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
}

/** A pending Downloads entry is published only after the complete reviewed snapshot has been written. */
@RequiresApi(Build.VERSION_CODES.Q)
fun saveDiagnosticDownload(resolver: ContentResolver, filename: String, text: String) {
	val values = ContentValues().apply {
		put(MediaStore.Downloads.DISPLAY_NAME, filename)
		put(MediaStore.Downloads.MIME_TYPE, "text/plain")
		put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LumaFin")
		put(MediaStore.Downloads.IS_PENDING, 1)
	}
	val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("Unable to create export")
	try {
		writeDiagnosticExport(resolver, uri, text)
		val published = resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
		if (published != 1) throw IOException("Unable to finish export")
	} catch (failure: Exception) {
		runCatching { resolver.delete(uri, null, null) }
		throw failure
	}
}
