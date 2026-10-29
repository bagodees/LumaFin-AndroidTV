package org.jellyfin.androidtv

import android.app.Application
import android.content.Context
import org.acra.ACRA
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
}
