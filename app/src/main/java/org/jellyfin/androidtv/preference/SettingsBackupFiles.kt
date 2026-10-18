package org.jellyfin.androidtv.preference

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal object SettingsBackupFiles {
	private const val PREFIX = "LumaFin-settings-"
	fun filename() = "$PREFIX${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))}.json"
	data class Saved(val name: String, val uri: Uri)
	fun write(resolver: ContentResolver, uri: Uri, text: String) {
		val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
		stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
	}
	fun read(resolver: ContentResolver, uri: Uri): String {
		val stream = resolver.openInputStream(uri) ?: throw IOException("No input stream")
		return stream.use {
			val output = ByteArrayOutputStream()
			val buffer = ByteArray(4096)
			while (true) {
				val count = it.read(buffer)
				if (count < 0) break
				require(output.size() + count <= SettingsBackup.MAX_BYTES) { "Backup too large" }
				output.write(buffer, 0, count)
			}
			output.toString("UTF-8")
		}
	}
	@RequiresApi(Build.VERSION_CODES.Q)
	fun save(resolver: ContentResolver, text: String) {
		val values = ContentValues().apply {
			put(MediaStore.Downloads.DISPLAY_NAME, filename())
			put(MediaStore.Downloads.MIME_TYPE, "application/json")
			put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LumaFin")
			put(MediaStore.Downloads.IS_PENDING, 1)
		}
		val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("No destination")
		try {
			write(resolver, uri, text)
			check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) == 1)
		} catch (failure: Exception) {
			runCatching { resolver.delete(uri, null, null) }
			throw failure
		}
	}
	/** Scoped storage exposes our own files without requesting access to the user's storage. */
	@RequiresApi(Build.VERSION_CODES.Q)
	fun saved(resolver: ContentResolver): List<Saved> {
		val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
		return resolver.query(collection, arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
			"${MediaStore.Downloads.DISPLAY_NAME} LIKE ? AND ${MediaStore.Downloads.IS_PENDING} = 0",
			arrayOf("$PREFIX%.json"), "${MediaStore.Downloads.DATE_ADDED} DESC")?.use { cursor ->
			buildList {
				while (cursor.moveToNext() && size < 100) add(Saved(cursor.getString(1), ContentUris.withAppendedId(collection, cursor.getLong(0))))
			}
		}.orEmpty()
	}
}
