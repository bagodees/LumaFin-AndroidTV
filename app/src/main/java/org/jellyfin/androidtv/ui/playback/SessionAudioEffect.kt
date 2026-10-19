package org.jellyfin.androidtv.ui.playback

/** Own one optional effect per player; unsupported devices must still play normally. */
internal class SessionAudioEffect(private val create: (Int) -> AutoCloseable) : AutoCloseable {
	private var effect: AutoCloseable? = null
	private var session: Int? = null

	fun attach(id: Int) {
		if (id > 0 && session == id && effect != null) return
		close()
		if (id <= 0) return // Never attach to the global output mix (session 0).
		effect = runCatching { create(id) }.getOrNull()
		if (effect != null) session = id
	}

	override fun close() {
		val previous = effect
		effect = null
		session = null
		runCatching { previous?.close() }
	}
}
