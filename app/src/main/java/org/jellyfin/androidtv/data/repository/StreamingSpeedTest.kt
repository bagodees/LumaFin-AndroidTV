package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SpeedMeasurement(val bytes: Long, val seconds: Double, val intervals: List<Double>) {
	val mbps get() = bytes * 8 / seconds / 1_000_000
	val shortSample get() = seconds < 2
}

/** Bounded memory and transfer; reads stop at the byte limit or elapsed deadline. */
internal fun measureTransfer(
	input: InputStream,
	maxBytes: Long = 64L * 1024 * 1024,
	maxSeconds: Double = 10.0,
	nanoTime: () -> Long = System::nanoTime,
	isCancelled: () -> Boolean = { false },
): SpeedMeasurement {
	val start = nanoTime()
	var last = start
	var bytes = 0L
	var intervalBytes = 0L
	val intervals = mutableListOf<Double>()
	val buffer = ByteArray(64 * 1024)
	while (bytes < maxBytes && (nanoTime() - start) / 1e9 < maxSeconds && !isCancelled()) {
		val read = try {
			input.read(buffer, 0, minOf(buffer.size.toLong(), maxBytes - bytes).toInt())
		} catch (error: IOException) {
			if (bytes > 0 && (nanoTime() - start) / 1e9 >= maxSeconds && !isCancelled()) break
			throw error
		}
		if (read < 0) break
		bytes += read
		val now = nanoTime()
		if (now - last >= 1_000_000_000L) {
			intervals += (bytes - intervalBytes) * 8.0 / ((now - last) / 1e9) / 1_000_000
			last = now
			intervalBytes = bytes
		}
	}
	if (bytes == 0L) throw IOException("Empty transfer")
	return SpeedMeasurement(bytes, ((nanoTime() - start) / 1e9).coerceAtLeast(0.001), intervals)
}

data class StreamingSpeedResult(val title: String, val bitrate: Int?, val measurement: SpeedMeasurement)
class NoSpeedTestMedia : Exception()

/** No playback sessions or progress reports: read a bounded part of a local video through the server. */
class StreamingSpeedTest {
	private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
		.connectTimeout(5, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).callTimeout(16, TimeUnit.SECONDS).build()

	suspend fun run(api: ApiClient, userId: UUID): StreamingSpeedResult {
		val items = api.itemsApi.getItems(userId = userId, recursive = true, limit = 20,
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO),
			fields = setOf(ItemFields.MEDIA_SOURCES), isMissing = false).content.items.orEmpty()
		val candidate = items.asSequence().flatMap { item -> item.mediaSources.orEmpty().asSequence().map { item to it } }
			.filter { (_, source) -> !source.isRemote && source.liveStreamId == null && (source.size ?: 0) >= 1024 * 1024 }
			.maxByOrNull { (_, source) -> source.size ?: 0 } ?: throw NoSpeedTestMedia()
		val (item, source) = candidate
		val url = api.videosApi.getVideoStreamUrl(itemId = item.id, mediaSourceId = source.id, static = true)
		val request = Request.Builder().url(url).header("Range", "bytes=0-67108863")
			.header("X-Emby-Token", requireNotNull(api.accessToken))
			.header("Cache-Control", "no-cache, no-store").header("Accept-Encoding", "identity").build()
		return StreamingSpeedResult(item.name.orEmpty(), source.bitrate, transfer(request))
	}

	internal suspend fun transfer(request: Request): SpeedMeasurement = suspendCancellableCoroutine { continuation ->
			val call = client.newCall(request)
			continuation.invokeOnCancellation { call.cancel() }
			call.enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					if (continuation.isActive) continuation.resumeWithException(e)
				}
				override fun onResponse(call: Call, response: Response) {
					try {
						val result = response.use {
							if (it.code !in setOf(200, 206)) throw IOException("Transfer rejected")
							val type = it.header("Content-Type").orEmpty()
							if (type.startsWith("text/") || type.contains("json")) throw IOException("Not media")
							val body = it.body ?: throw IOException("Missing response")
							body.source().timeout().deadline(10, TimeUnit.SECONDS)
							body.byteStream().use { stream -> measureTransfer(stream, isCancelled = { call.isCanceled() }) }
						}
						if (continuation.isActive) continuation.resume(result)
					} catch (error: Exception) {
						if (continuation.isActive) continuation.resumeWithException(error)
					} finally {
						call.cancel()
					}
				}
			})
		}
}
