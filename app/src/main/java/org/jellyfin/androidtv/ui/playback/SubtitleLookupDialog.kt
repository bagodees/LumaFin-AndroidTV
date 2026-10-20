package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.SdkSubtitleLookupGateway
import org.jellyfin.androidtv.data.repository.SubtitleLookup
import org.jellyfin.androidtv.data.repository.subtitleLookupAllowed
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.model.ServerVersion
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo
import org.jellyfin.androidtv.data.repository.subtitleLookupTarget
import org.koin.core.context.GlobalContext
import java.util.Locale

fun canGetMoreSubtitles(item: BaseItemDto?): Boolean {
	val koin = GlobalContext.get()
	val user = koin.get<UserRepository>().currentUser.value
	val version = koin.get<ServerRepository>().currentServer.value?.serverVersion
	return subtitleLookupAllowed(item?.type, user?.policy?.enableSubtitleManagement == true,
		version != null && version >= ServerVersion(10, 9, 0))
}

/** Bound to one user, server, title and source. All dialogs/work end when the invoking view ends. */
class SubtitleLookupDialog(
	private val context: Context,
	private val owner: LifecycleOwner,
	private val item: BaseItemDto,
	private val sourceId: String,
	private val stillCurrent: () -> Boolean,
	private val playback: Boolean = false,
	private val onRefreshed: (BaseItemDto) -> Unit,
	private val onDismiss: () -> Unit = {},
) : DefaultLifecycleObserver {
	private val koin = GlobalContext.get()
	private val sessions = koin.get<SessionRepository>()
	private val servers = koin.get<ServerRepository>()
	private val session = sessions.currentSession.value
	private val server = servers.currentServer.value
	private var dialog: LumaFinMenuDialog? = null
	private var job: Job? = null
	private var closed = false
	// Jellyfin's download route has no mediaSourceId parameter. Address the selected
	// local version by its UUID source ID, never silently download for a grouped title.
	private val targetId = subtitleLookupTarget(sourceId)
	private val lookup = targetId?.let { target -> session?.let { selected -> server?.let { selectedServer ->
		val api = koin.get<Jellyfin>().createApi(selectedServer.address).apply { update(accessToken = selected.accessToken) }
		SubtitleLookup(SdkSubtitleLookupGateway(api, selected.userId, target), ::isCurrent)
	} } }
	private fun isCurrent() = !closed && stillCurrent() && sessions.currentSession.value == session &&
		servers.currentServer.value?.address == server?.address && canGetMoreSubtitles(item)

	fun show() {
		if (!isCurrent()) { dismiss(); return }
		owner.lifecycle.addObserver(this)
		if (lookup == null || sourceId.isBlank()) {
			replace(panel(R.string.subtitle_lookup_more, R.string.subtitle_lookup_unsupported).apply {
				button(context.getString(android.R.string.ok)) { dismiss() }
			})
			return
		}
		languages()
	}
	override fun onStop(owner: LifecycleOwner) = dismiss()
	override fun onDestroy(owner: LifecycleOwner) = dismiss()
	fun dismiss() {
		if (closed) return
		closed = true
		job?.cancel()
		dialog?.onDismiss {}
		dialog?.dismiss()
		owner.lifecycle.removeObserver(this)
		onDismiss()
	}
	private fun replace(next: LumaFinMenuDialog, focus: Int = 0) {
		if (!isCurrent()) { dismiss(); return }
		dialog?.onDismiss {}
		dialog?.dismiss()
		dialog = next
		next.onDismiss { dismiss() }
		next.show(focus)
	}
	private fun panel(title: Int, message: Int? = null) = LumaFinMenuDialog(context, owner, context.getString(title), message?.let(context::getString), fitContent = true)
	private fun languages() {
		val choices = Locale.getISOLanguages().mapNotNull { code -> runCatching {
			val locale = Locale.forLanguageTag(code)
			locale.isO3Language to locale.getDisplayLanguage(Locale.getDefault())
		}.getOrNull() }.filter { it.first.matches(Regex("[a-z]{3}")) }.distinctBy { it.first }.sortedBy { it.second }
		val configured = koin.get<UserRepository>().currentUser.value?.configuration?.subtitleLanguagePreference
		val preferred = if (configured?.length == 3) configured else runCatching {
			(if (configured?.length == 2) Locale.forLanguageTag(configured) else Locale.getDefault()).isO3Language
		}.getOrDefault("eng")
		val next = panel(R.string.subtitle_lookup_language, R.string.subtitle_lookup_help)
		choices.forEach { (code, name) -> next.row(name) { search(code) } }
		next.button(context.getString(android.R.string.cancel)) { dismiss() }
		replace(next, choices.indexOfFirst { it.first == preferred }.coerceAtLeast(0))
	}
	private fun busy(message: Int) = replace(panel(R.string.subtitle_lookup_more, message).apply {
		button(context.getString(android.R.string.cancel)) { dismiss() }
	})
	private fun failure(error: Exception, retry: (() -> Unit)?) {
		if (!isCurrent()) { dismiss(); return }
		val message = when ((error as? InvalidStatusException)?.status) {
			401, 403 -> R.string.subtitle_lookup_denied
			404, 405 -> R.string.subtitle_lookup_unsupported
			else -> R.string.subtitle_lookup_error
		}
		replace(panel(R.string.subtitle_lookup_more, message).apply {
			button(context.getString(android.R.string.cancel)) { dismiss() }
			if (retry != null && message == R.string.subtitle_lookup_error)
				button(context.getString(R.string.connection_retry), true) { retry() }
		})
	}
	private fun search(language: String) {
		busy(R.string.subtitle_lookup_busy)
		job = owner.lifecycleScope.launch {
			try {
				val results = lookup!!.search(language).take(100)
				if (!isCurrent()) { dismiss(); return@launch }
				if (results.isEmpty()) replace(panel(R.string.subtitle_lookup_more, R.string.subtitle_lookup_empty).apply {
					button(context.getString(android.R.string.cancel)) { dismiss() }
					button(context.getString(R.string.subtitle_lookup_language), true) { languages() }
				}) else {
					val next = LumaFinMenuDialog(context, owner,
						context.resources.getQuantityString(R.plurals.subtitle_lookup_results, results.size, results.size), fitContent = true)
					results.forEach { result -> next.row(result.name?.take(180)?.takeIf { it.isNotBlank() }
						?: context.getString(R.string.subtitle_lookup_provider_unknown), label(result, includeName = false)) { confirm(result, language) } }
					next.button(context.getString(android.R.string.cancel)) { dismiss() }
					replace(next)
				}
			} catch (error: TimeoutCancellationException) { failure(error) { search(language) } }
			catch (error: CancellationException) { dismiss(); throw error }
			catch (error: Exception) { failure(error) { search(language) } }
		}
	}
	private fun label(result: RemoteSubtitleInfo, includeName: Boolean = true) = listOfNotNull(
		result.name?.take(180)?.takeIf { includeName }, result.providerName?.take(80) ?: context.getString(R.string.subtitle_lookup_provider_unknown),
		result.format?.take(20),
		context.getString(R.string.subtitle_lookup_hash_match).takeIf { result.isHashMatch == true },
		context.getString(R.string.subtitle_lookup_hearing_impaired).takeIf { result.hearingImpaired == true },
		context.getString(R.string.subtitle_lookup_forced).takeIf { result.forced == true },
	).joinToString(" · ")
	private fun confirm(result: RemoteSubtitleInfo, language: String) {
		replace(LumaFinMenuDialog(context, owner, context.getString(R.string.subtitle_lookup_download),
			context.getString(R.string.subtitle_lookup_confirm, label(result)), fitContent = true).apply {
			button(context.getString(android.R.string.cancel)) { search(language) }
			button(context.getString(R.string.subtitle_lookup_download), true) { download(requireNotNull(result.id), language) }
		})
	}
	private fun download(id: String, language: String) {
		busy(R.string.subtitle_lookup_verifying)
		job = owner.lifecycleScope.launch {
			try {
				val refreshed = lookup!!.download(id, sourceId, language)
				if (!isCurrent()) { dismiss(); return@launch }
				val next = panel(R.string.subtitle_lookup_more,
					if (refreshed == null) R.string.subtitle_lookup_uncertain else if (playback) R.string.subtitle_lookup_playback_help else R.string.subtitle_lookup_verified)
				next.button(context.getString(android.R.string.ok)) { dismiss() }
				if (refreshed != null) {
					if (playback) next.button(context.getString(R.string.subtitle_lookup_reload), true) { if (isCurrent()) onRefreshed(refreshed); dismiss() }
					else onRefreshed(refreshed)
				}
				replace(next)
			} catch (error: CancellationException) {
				if (error is TimeoutCancellationException && isCurrent()) uncertain() else { dismiss(); throw error }
			} catch (error: Exception) {
				// Do not offer a download retry: the server may have completed the first POST.
				if (error is InvalidStatusException) failure(error, null) else uncertain()
			}
		}
	}
	private fun uncertain() = replace(panel(R.string.subtitle_lookup_more, R.string.subtitle_lookup_uncertain).apply {
		button(context.getString(android.R.string.ok)) { dismiss() }
	})
}
