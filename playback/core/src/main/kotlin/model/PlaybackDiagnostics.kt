package org.jellyfin.playback.core.model

/** Optional player observations. Null means unavailable, never a measured zero. */
data class PlaybackDiagnostics(
	val bufferedDurationMs: Long? = null,
	val droppedVideoFrames: Int? = null,
	val renderedVideoFrames: Int? = null,
	val buffering: Boolean? = null,
	val minBufferMs: Long? = null,
	val maxBufferMs: Long? = null,
	val startupBufferMs: Long? = null,
	val rebufferMs: Long? = null,
)
