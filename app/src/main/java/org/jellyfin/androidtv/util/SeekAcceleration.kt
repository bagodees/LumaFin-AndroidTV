package org.jellyfin.androidtv.util

/** Shared timeline hold policy. A fresh press always uses the configured interval. */
object SeekAcceleration {
	@JvmStatic
	fun multiplier(repeatCount: Int, heldMs: Long): Int = when {
		repeatCount <= 0 || heldMs < 1000 -> 1
		heldMs < 3000 -> 2
		heldMs < 5000 -> 4
		else -> 8
	}
}
