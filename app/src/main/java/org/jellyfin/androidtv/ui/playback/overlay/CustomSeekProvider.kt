package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.leanback.widget.PlaybackSeekDataProvider
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.maxBitmapSize
import coil3.size.Dimension
import coil3.size.Size
import coil3.toBitmap
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.isLowMemoryDevice
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.trickplayApi
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

class CustomSeekProvider(
	private val videoPlayerAdapter: VideoPlayerAdapter,
	private val imageLoader: ImageLoader,
	private val api: ApiClient,
	private val context: Context,
	private val forwardTime: Long
) : PlaybackSeekDataProvider() {
	private companion object {
		// Decoded preview sheets kept while scrubbing: the current one and the one before it
		const val MAX_SHEETS = 2
	}

	private val lowMemoryDevice by lazy { context.isLowMemoryDevice() }
	private val sheets = LinkedHashMap<Int, Bitmap>()
	private val sheetRequests = mutableMapOf<Int, Disposable>()
	private val sheetWaiters = mutableMapOf<Int, MutableList<(Bitmap?) -> Unit>>()
	private var currentSeekPositions = LongArray(0)
	private var requestSequence = 0L
	private val requestTokens = mutableMapOf<Int, Long>()

	private var cachedPlaceholderThumbnail: Bitmap? = null

	override fun getSeekPositions(): LongArray {
		if (!videoPlayerAdapter.canSeek()) return LongArray(0)

		val currentSeekPosition = videoPlayerAdapter.currentPosition
		val videoEndPosition = videoPlayerAdapter.duration

		val firstSeekPosition = currentSeekPosition % forwardTime
		val lastSeekPosition = videoEndPosition - ((videoEndPosition - currentSeekPosition) % forwardTime)
		val seekPositionCount = ((lastSeekPosition - firstSeekPosition) / forwardTime).toInt() + 1

		val seekPositions = ArrayList<Long>(seekPositionCount + 2) // intermediate seek positions + beginning + end position
		seekPositions.add(0L)
		// Omit the first seek position if it represents the beginning of the video
		if (firstSeekPosition != 0L) {
			seekPositions.add(firstSeekPosition)
		}
		// Add all available seek positions but the last one
		for (i in 1..<seekPositionCount - 1) {
			seekPositions.add(firstSeekPosition + (i * forwardTime))
		}
		// Omit the last seek position if it represents the end of the video
		if (lastSeekPosition != videoEndPosition) {
			seekPositions.add(lastSeekPosition)
		}
		// Omit the video end seek position if it represents the beginning of the video
		if (videoEndPosition != 0L) {
			seekPositions.add(videoEndPosition)
		}

		currentSeekPositions = seekPositions.toLongArray()
		return currentSeekPositions
	}

	override fun getThumbnail(index: Int, callback: ResultCallback) {
		if (index !in currentSeekPositions.indices) return
		val token = ++requestSequence
		requestTokens[index] = token

		val item = videoPlayerAdapter.currentlyPlayingItem
		val mediaSource = videoPlayerAdapter.currentMediaSource
		val mediaSourceId = mediaSource?.id?.toUUIDOrNull()
		if (item == null || mediaSource == null || mediaSourceId == null) return

		val trickPlayResolutions = item.trickplay?.get(mediaSource.id)
		val trickPlayInfo = trickPlayResolutions?.values?.firstOrNull {
			it.interval > 0 && it.tileWidth > 0 && it.tileHeight > 0 && it.width > 0 && it.height > 0
		} ?: return

		val currentTimeMs = currentSeekPositions[index].coerceAtMost((videoPlayerAdapter.duration - 1).coerceAtLeast(0))
		val currentTile = currentTimeMs.floorDiv(trickPlayInfo.interval).toInt()

		val tileSize = trickPlayInfo.tileWidth * trickPlayInfo.tileHeight
		val tileOffset = currentTile % tileSize
		val tileIndex = currentTile / tileSize

		val tileOffsetX = tileOffset % trickPlayInfo.tileWidth
		val tileOffsetY = tileOffset / trickPlayInfo.tileWidth
		val offsetX = tileOffsetX * trickPlayInfo.width
		val offsetY = tileOffsetY * trickPlayInfo.height

		val url = api.trickplayApi.getTrickplayTileImageUrl(
			itemId = item.id,
			width = trickPlayInfo.width,
			index = tileIndex,
			mediaSourceId = mediaSourceId,
		)

		val placeholderThumbnail = getPlaceholderThumbnail(trickPlayInfo.width, trickPlayInfo.height)

		val deliver = { sheet: Bitmap? ->
			if (requestTokens[index] == token) {
				val thumbnail = sheet?.let { crop(it, offsetX, offsetY, trickPlayInfo.width, trickPlayInfo.height) }
				callback.onThumbnailLoaded(thumbnail ?: placeholderThumbnail, index)
			}
		}

		// A preview is one tile of a sheet of previews. Each sheet is decoded once and every preview
		// is cut from it; decoding the whole sheet for every preview while scrubbing got LumaFin
		// closed for low memory on a 2 GB Fire TV Stick
		val sheet = sheets[tileIndex]
		if (sheet != null) {
			sheets.remove(tileIndex)
			sheets[tileIndex] = sheet
			deliver(sheet)
			return
		}

		callback.onThumbnailLoaded(placeholderThumbnail, index)
		val waiting = sheetWaiters.getOrPut(tileIndex) { mutableListOf() }
		waiting += deliver
		if (sheetRequests[tileIndex]?.isDisposed == false) return

		sheetRequests[tileIndex] = imageLoader.enqueue(ImageRequest.Builder(context).apply {
			data(url)
			size(Size.ORIGINAL)
			maxBitmapSize(Size(Dimension.Undefined, Dimension.Undefined))
			memoryCachePolicy(CachePolicy.DISABLED)
			allowHardware(false)
			if (lowMemoryDevice) bitmapConfig(Bitmap.Config.RGB_565)
			httpHeaders(NetworkHeaders.Builder().apply {
				set(
					key = "Authorization",
					value = AuthorizationHeaderBuilder.buildHeader(
						api.clientInfo.name,
						api.clientInfo.version,
						api.deviceInfo.id,
						api.deviceInfo.name,
						api.accessToken
					)
				)
			}.build())

			target(
				onError = { _ ->
					sheetRequests.remove(tileIndex)
					sheetWaiters.remove(tileIndex)?.forEach { it(null) }
				},
				onSuccess = { image ->
					sheetRequests.remove(tileIndex)
					val bitmap = image.toBitmap()
					sheets[tileIndex] = bitmap
					while (sheets.size > MAX_SHEETS) sheets.remove(sheets.keys.first())
					sheetWaiters.remove(tileIndex)?.forEach { it(bitmap) }
				}
			)
		}.build())
	}

	private fun crop(sheet: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap? {
		if (x >= sheet.width || y >= sheet.height) return null
		return Bitmap.createBitmap(sheet, x, y, width.coerceAtMost(sheet.width - x), height.coerceAtMost(sheet.height - y))
	}

	override fun reset() {
		requestTokens.clear()
		for (request in sheetRequests.values) {
			if (!request.isDisposed) request.dispose()
		}
		sheetRequests.clear()
		sheetWaiters.clear()
		sheets.clear()

		currentSeekPositions = LongArray(0)
	}

	fun getPlaceholderThumbnail(width: Int, height: Int): Bitmap {
		if (cachedPlaceholderThumbnail?.width == width && cachedPlaceholderThumbnail?.height == height) {
			return cachedPlaceholderThumbnail!!
		}

		val color = ContextCompat.getColor(context, R.color.black_transparent_light)
		val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		result.eraseColor(color)
		cachedPlaceholderThumbnail = result
		return result
	}
}
