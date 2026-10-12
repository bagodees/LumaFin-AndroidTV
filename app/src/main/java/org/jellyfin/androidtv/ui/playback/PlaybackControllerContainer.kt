package org.jellyfin.androidtv.ui.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.java.KoinJavaComponent
import java.util.UUID

class PlaybackControllerContainer {
	var playbackController: PlaybackController? = null
	private val timer = SleepTimerState()
	private val handler = Handler(Looper.getMainLooper())
	private val expire = Runnable {
		if (timer.expired(SystemClock.elapsedRealtime())) {
			cancelSleepTimer()
			val controller = playbackController
			if (controller?.hasFragment() == true) controller.endPlayback(true)
			else KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).goBack()
		}
	}
	val sleepTimerMinutes: Int get() = timer.minutes
	fun setSleepTimer(minutes: Int) {
		cancelSleepTimer()
		timer.set(minutes, SystemClock.elapsedRealtime(), playbackController?.currentlyPlayingItem?.id)
		if (minutes > 0) handler.postDelayed(expire, minutes * 60_000L)
	}
	fun cancelSleepTimer() {
		handler.removeCallbacks(expire)
		timer.cancel()
	}
	fun endsWithEpisode(id: UUID): Boolean = timer.endsWithEpisode(id)
	fun remainingSleepMinutes(): Int = timer.deadline?.let {
		((it - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 59_999).div(60_000).toInt()
	} ?: 0
}
