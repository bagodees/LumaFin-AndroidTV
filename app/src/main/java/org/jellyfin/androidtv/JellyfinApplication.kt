package org.jellyfin.androidtv

import android.app.Application
import android.content.Context
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import org.acra.ACRA
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.telemetry.ExitReporter
import org.jellyfin.androidtv.telemetry.TelemetryService

class JellyfinApplication : Application() {
	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		TelemetryService.init(this)
	}

	override fun onCreate() {
		super.onCreate()
		// Only the app's own process; ACRA sends its reports from a separate one
		if (!ACRA.isACRASenderServiceProcess()) ExitReporter.reportPreviousExits(this)
	}

	/**
	 * When the system says memory is getting low, cached images go first, before Android starts
	 * closing apps. A 2 GB Fire TV Stick closed LumaFin during long seeks in 4K titles.
	 */
	@Suppress("DEPRECATION")
	override fun onTrimMemory(level: Int) {
		super.onTrimMemory(level)
		if (level < ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || ACRA.isACRASenderServiceProcess()) return
		runCatching {
			org.koin.java.KoinJavaComponent.get<ImageLoader>(ImageLoader::class.java).memoryCache?.clear()
			org.koin.java.KoinJavaComponent.get<BackgroundService>(BackgroundService::class.java).releaseMemory()
		}
	}
}
