package org.jellyfin.androidtv.preference

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.displayPreferencesApi
import org.jellyfin.sdk.model.api.DisplayPreferencesDto
import org.jellyfin.sdk.model.api.ScrollDirection
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

/** The synced copy on the Jellyfin server; null text means no copy. */
internal interface SettingsSyncRemote {
	suspend fun read(): String?
	suspend fun write(text: String?)
}

/** Jellyfin per-user display preferences: visible only to the same user, no plugin required. */
internal class DisplayPreferencesSyncRemote(private val api: ApiClient) : SettingsSyncRemote {
	private suspend fun current(): DisplayPreferencesDto =
		api.displayPreferencesApi.getDisplayPreferences(displayPreferencesId = ID, client = CLIENT).content

	override suspend fun read(): String? = current().customPrefs[KEY]

	override suspend fun write(text: String?) {
		val dto = runCatching { current() }.getOrElse { empty }
		// A save replaces the whole key set, so keep anything else stored under this ID.
		val prefs = dto.customPrefs.filterKeys { it != KEY } + listOfNotNull(text?.let { KEY to it })
		api.displayPreferencesApi.updateDisplayPreferences(displayPreferencesId = ID, client = CLIENT, data = dto.copy(customPrefs = prefs))
	}

	companion object {
		const val ID = "lumafin-settings-sync"
		const val CLIENT = "lumafin-androidtv"
		const val KEY = "lumafinSettingsSync"
		private val empty = DisplayPreferencesDto(
			primaryImageHeight = 0, primaryImageWidth = 0, customPrefs = emptyMap(), rememberIndexing = false,
			scrollDirection = ScrollDirection.HORIZONTAL, rememberSorting = false, showBackdrop = false,
			showSidebar = false, sortOrder = SortOrder.ASCENDING,
		)
	}
}

/**
 * Optional settings sync (#27). Enablement, the last agreed values and the revision live in the active
 * account's preference file, so each Jellyfin user/server is independent and nothing syncs while signed out.
 */
class SettingsSync internal constructor(
	private val preferences: SwitchingAccountPreferences,
	/** Current server/user identity, or null when signed out. */
	private val identity: () -> String?,
	/** A remote bound to that identity's server and token. */
	private val remoteFor: (String) -> SettingsSyncRemote?,
	private val writer: String,
	private val scope: CoroutineScope,
	private val now: () -> Long = System::currentTimeMillis,
	private val debounceMs: Long = 3_000,
) {
	sealed interface Outcome {
		data object Disabled : Outcome
		data object Synced : Outcome
		data object AccountChanged : Outcome
		data class Failed(val error: Throwable) : Outcome
		/** Differences or conflicts wait for [resolve]. */
		data class NeedsReview(val review: Review) : Outcome
	}

	/** Settings that differ between this TV and the synced copy: key to (this TV, synced). */
	data class Review(val identity: String, val enabling: Boolean, val differences: Map<String, Pair<Any, Any>>)

	data class State(val enabled: Boolean, val lastSynced: Long?, val syncing: Boolean, val failed: Boolean, val review: Review?)

	private val mutex = Mutex()
	private var pending: Pair<SettingsSyncModel.Merge, SettingsSyncModel.Envelope?>? = null
	private var reviewedLocal: Map<String, Any>? = null
	private var applying = false
	private var scheduled: Job? = null
	private val _state = MutableStateFlow(readState())
	val state: StateFlow<State> = _state.asStateFlow()

	private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		when {
			key == null -> { pending = null; refresh(review = null) } // account switched
			key in SettingsSyncModel.syncedKeys && !applying && enabled -> requestSync(debounceMs)
		}
	}

	init {
		preferences.registerOnSharedPreferenceChangeListener(listener)
	}

	private val enabled get() = preferences.getBoolean(ENABLED, false)

	private fun readState(review: Review? = null, syncing: Boolean = false, failed: Boolean = false) = State(
		enabled = preferences.getBoolean(ENABLED, false),
		lastSynced = preferences.getLong(LAST, 0L).takeIf { it > 0 },
		syncing = syncing, failed = failed, review = review,
	)

	private fun refresh(review: Review? = _state.value.review, syncing: Boolean = false, failed: Boolean = false) {
		_state.value = readState(review, syncing, failed)
	}

	private fun base(): Map<String, Any>? = preferences.getString(BASE, null)?.let { text ->
		runCatching { SettingsSyncModel.decodeSettings(Json.parseToJsonElement(text).jsonObject) }.getOrNull()
	}

	private fun local() = SettingsSyncModel.snapshot { preferences.all[it] }

	/** Debounced background sync; never blocks the UI and quietly retries on the next trigger after a failure. */
	fun requestSync(delayMs: Long = 0) {
		if (!enabled) return
		scheduled?.cancel()
		scheduled = scope.launch {
			if (delayMs > 0) delay(delayMs)
			sync()
		}
	}

	suspend fun sync(): Outcome = mutex.withLock {
		if (!enabled) return Outcome.Disabled
		run(enabling = false)
	}

	/**
	 * Turns sync on. With no synced copy this TV's values are uploaded; a copy that differs returns a review
	 * (Use synced / Replace synced) and sync stays off until [resolve].
	 */
	suspend fun enable(): Outcome = mutex.withLock { run(enabling = true) }

	/** Turning sync off keeps local values and the server copy. */
	fun disable() {
		scheduled?.cancel()
		pending = null
		preferences.edit().putBoolean(ENABLED, false).remove(BASE).remove(REVISION).commit()
		refresh(review = null)
	}

	/** Deletes this account's synced copy from the server and turns sync off on this TV. */
	suspend fun removeRemote(): Outcome = mutex.withLock {
		val identity = identity() ?: return Outcome.AccountChanged
		val remote = remoteFor(identity) ?: return Outcome.AccountChanged
		try {
			remote.write(null)
			if (identity() != identity) return Outcome.AccountChanged
			disable()
			Outcome.Synced
		} catch (error: Exception) {
			if (error is CancellationException) throw error
			Timber.w(error, "Unable to remove synced settings")
			Outcome.Failed(error)
		}
	}

	private suspend fun run(enabling: Boolean): Outcome {
		val identity = identity() ?: return Outcome.AccountChanged
		val remote = remoteFor(identity) ?: return Outcome.AccountChanged
		refresh(syncing = true)
		var outcome: Outcome = Outcome.Failed(IllegalStateException("Too many concurrent changes"))
		try {
			repeat(MAX_ATTEMPTS) {
				val remoteEnvelope = remote.read()?.let(SettingsSyncModel::decode)
				if (identity() != identity) return Outcome.AccountChanged.also { refresh() }
				val merge = SettingsSyncModel.merge(if (enabling) null else base(), local(), remoteEnvelope?.settings)
				if (merge.conflicts.isNotEmpty()) {
					// Enabling changes nothing until the viewer chooses; a normal sync still merges everything else.
					val server = if (enabling) Committed(remoteEnvelope) else commit(identity, remote, merge, remoteEnvelope) ?: return@repeat
					val review = Review(identity, enabling, merge.conflicts)
					pending = merge to server.server
					reviewedLocal = local()
					refresh(review = review)
					return Outcome.NeedsReview(review)
				}
				commit(identity, remote, merge, remoteEnvelope) ?: return@repeat
				if (enabling) preferences.edit().putBoolean(ENABLED, true).commit()
				refresh(review = null)
				return Outcome.Synced
			}
		} catch (error: Exception) {
			if (error is CancellationException) { refresh(); throw error }
			Timber.w(error, "Settings sync failed")
			outcome = Outcome.Failed(error)
		}
		refresh(failed = true)
		return outcome
	}

	/** Applies the viewer's choice for the pending review. */
	internal suspend fun resolve(review: Review, resolution: SettingsSyncModel.Resolution): Outcome = mutex.withLock {
		val (merge, remoteEnvelope) = pending ?: return Outcome.Failed(IllegalStateException("Nothing to review"))
		val identity = identity() ?: return Outcome.AccountChanged
		val remote = remoteFor(identity) ?: return Outcome.AccountChanged
		if (identity != review.identity || _state.value.review != review) return Outcome.AccountChanged
		try {
			// Re-check: anything that moved since the review was shown gets a fresh merge instead.
			val current = remote.read()?.let(SettingsSyncModel::decode)
			if (identity() != identity || _state.value.review != review) return Outcome.AccountChanged
			if (current != remoteEnvelope || local() != reviewedLocal) {
				pending = null
				refresh(review = null)
				return run(review.enabling)
			}
			val resolved = SettingsSyncModel.resolve(merge, remoteEnvelope?.settings.orEmpty(), resolution)
			commit(identity, remote, resolved, remoteEnvelope) ?: return Outcome.Failed(IllegalStateException("Synced copy changed"))
			pending = null
			if (review.enabling) preferences.edit().putBoolean(ENABLED, true).commit()
			refresh(review = null)
			Outcome.Synced
		} catch (error: Exception) {
			if (error is CancellationException) { refresh(); throw error }
			Timber.w(error, "Unable to apply settings sync review")
			refresh(failed = true)
			Outcome.Failed(error)
		}
	}

	/** Leaves the review for later; local values stay and nothing is uploaded for the conflicting settings. */
	fun dismissReview() {
		if (_state.value.review?.enabling == true) pending = null
		refresh(review = if (_state.value.review?.enabling == true) null else _state.value.review)
	}

	private class Committed(val server: SettingsSyncModel.Envelope?)

	/**
	 * Writes local changes, re-reads before uploading, then records the new agreed values. Returns the copy now on
	 * the server, or null when it changed underneath and the merge must be retried.
	 */
	private suspend fun commit(identity: String, remote: SettingsSyncRemote, merge: SettingsSyncModel.Merge, read: SettingsSyncModel.Envelope?): Committed? {
		check(identity() == identity) { "Account changed" }
		if (merge.toLocal.isNotEmpty()) {
			applying = true
			try {
				preferences.restoreLocal(merge.toLocal)
			} finally {
				applying = false
			}
		}
		var revision = read?.revision ?: 0
		var written = read
		merge.toRemote?.let { values ->
			val latest = remote.read()?.let(SettingsSyncModel::decode)
			if (identity() != identity) throw IllegalStateException("Account changed")
			if (latest != read) return null
			revision += 1
			val envelope = SettingsSyncModel.Envelope(revision, writer, values)
			remote.write(SettingsSyncModel.encode(envelope))
			written = envelope
		}
		if (identity() != identity) throw IllegalStateException("Account changed")
		preferences.edit()
			.putString(BASE, Json.encodeToString(JsonObject.serializer(), SettingsSyncModel.encodeSettings(merge.base)))
			.putInt(REVISION, revision)
			.putLong(LAST, now())
			.commit()
		return Committed(written)
	}

	companion object {
		private const val ENABLED = "settings_sync_enabled"
		private const val BASE = "settings_sync_base"
		private const val REVISION = "settings_sync_revision"
		private const val LAST = "settings_sync_last"
		private const val MAX_ATTEMPTS = 3
	}
}
