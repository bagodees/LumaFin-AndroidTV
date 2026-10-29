package org.jellyfin.androidtv.util

import android.app.ActivityManager
import android.content.Context

/**
 * Devices with less than 3 GB, like the Fire TV Stick 4K Max (2 GB). Playing 4K there leaves
 * little room, so LumaFin keeps a smaller video buffer and image cache and frees images while
 * a video plays.
 */
fun Context.isLowMemoryDevice(): Boolean {
	val activityManager = getSystemService(ActivityManager::class.java) ?: return false
	val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
	return activityManager.isLowRamDevice || memoryInfo.totalMem < 3L * 1024 * 1024 * 1024
}
