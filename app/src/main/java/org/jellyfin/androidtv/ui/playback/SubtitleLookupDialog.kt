package org.jellyfin.androidtv.ui.playback

import android.app.AlertDialog
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
	private var dialog: AlertDialog? = null
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
			replace(AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_more).setMessage(R.string.subtitle_lookup_unsupported)
				.setNegativeButton(android.R.string.ok, null).create())
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
		dialog?.setOnDismissListener(null)
		dialog?.dismiss()
		owner.lifecycle.removeObserver(this)
		onDismiss()
	}
	private fun replace(next: AlertDialog) {
		if (!isCurrent()) { dismiss(); return }
		dialog?.setOnDismissListener(null)
		dialog?.dismiss()
		dialog = next
		next.setOnDismissListener { dismiss() }
		next.show()
	}
	private fun languages() {
		val choices = Locale.getISOLanguages().mapNotNull { code -> runCatching {
			val locale = Locale.forLanguageTag(code)
			locale.isO3Language to locale.getDisplayLanguage(Locale.getDefault())
		}.getOrNull() }.filter { it.first.matches(Regex("[a-z]{3}")) }.distinctBy { it.first }.sortedBy { it.second }
		val list = android.widget.ArrayAdapter(context, android.R.layout.simple_list_item_1, choices.map { it.second })
		val languages = android.widget.ListView(context).apply { adapter = list }
		val body = android.widget.LinearLayout(context).apply {
			orientation = android.widget.LinearLayout.VERTICAL
			setPadding(24, 8, 24, 8)
			addView(android.widget.TextView(context).apply { setText(R.string.subtitle_lookup_help); setPadding(0, 0, 0, 12) })
			addView(languages, android.widget.LinearLayout.LayoutParams(-1, (context.resources.displayMetrics.heightPixels * .5).toInt()))
		}
		val next = AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_language).setView(body)
			.setNegativeButton(android.R.string.cancel, null).create()
		languages.setOnItemClickListener { _, _, position, _ -> search(choices[position].first) }
		replace(next)
		val configured = koin.get<UserRepository>().currentUser.value?.configuration?.subtitleLanguagePreference
		val preferred = if (configured?.length == 3) configured else runCatching {
			(if (configured?.length == 2) Locale.forLanguageTag(configured) else Locale.getDefault()).isO3Language
		}.getOrDefault("eng")
		val index = choices.indexOfFirst { it.first == preferred }.takeIf { it >= 0 }
		languages.requestFocus()
		if (index != null) languages.setSelection(index)
	}
	private fun busy(message: Int) = replace(AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_more)
		.setMessage(message).setNegativeButton(android.R.string.cancel, null).create())
	private fun failure(error: Exception, retry: (() -> Unit)?) {
		if (!isCurrent()) { dismiss(); return }
		val message = when ((error as? InvalidStatusException)?.status) {
			401, 403 -> R.string.subtitle_lookup_denied
			404, 405 -> R.string.subtitle_lookup_unsupported
			else -> R.string.subtitle_lookup_error
		}
		val builder = AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_more).setMessage(message)
			.setNegativeButton(android.R.string.cancel, null)
		if (retry != null && message == R.string.subtitle_lookup_error) builder.setPositiveButton(R.string.connection_retry) { _, _ -> retry() }
		replace(builder.create())
	}
	private fun search(language: String) {
		busy(R.string.subtitle_lookup_busy)
		job = owner.lifecycleScope.launch {
			try {
				val results = lookup!!.search(language).take(100)
				if (!isCurrent()) { dismiss(); return@launch }
				if (results.isEmpty()) replace(AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_more)
					.setMessage(R.string.subtitle_lookup_empty).setPositiveButton(R.string.subtitle_lookup_language) { _, _ -> languages() }
					.setNegativeButton(android.R.string.cancel, null).create())
				else replace(AlertDialog.Builder(context).setTitle(context.resources.getQuantityString(R.plurals.subtitle_lookup_results, results.size, results.size))
					.setItems(results.map(::label).toTypedArray()) { _, index -> confirm(results[index], language) }
					.setNegativeButton(android.R.string.cancel, null).create())
			} catch (error: TimeoutCancellationException) { failure(error) { search(language) } }
			catch (error: CancellationException) { dismiss(); throw error }
			catch (error: Exception) { failure(error) { search(language) } }
		}
	}
	private fun label(result: RemoteSubtitleInfo) = listOfNotNull(
		result.name?.take(180), result.providerName?.take(80) ?: context.getString(R.string.subtitle_lookup_provider_unknown),
		result.format?.take(20),
		context.getString(R.string.subtitle_lookup_hash_match).takeIf { result.isHashMatch == true },
		context.getString(R.string.subtitle_lookup_hearing_impaired).takeIf { result.hearingImpaired == true },
		context.getString(R.string.subtitle_lookup_forced).takeIf { result.forced == true },
	).joinToString(" · ")
	private fun confirm(result: RemoteSubtitleInfo, language: String) {
		replace(AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_download)
			.setMessage(context.getString(R.string.subtitle_lookup_confirm, label(result)))
			.setPositiveButton(R.string.subtitle_lookup_download) { _, _ -> download(requireNotNull(result.id), language) }
			.setNegativeButton(android.R.string.cancel) { _, _ -> search(language) }.create())
	}
	private fun download(id: String, language: String) {
		busy(R.string.subtitle_lookup_verifying)
		job = owner.lifecycleScope.launch {
			try {
				val refreshed = lookup!!.download(id, sourceId, language)
				if (!isCurrent()) { dismiss(); return@launch }
				val builder = AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_more)
					.setMessage(if (refreshed == null) R.string.subtitle_lookup_uncertain else if (playback) R.string.subtitle_lookup_playback_help else R.string.subtitle_lookup_verified)
					.setNegativeButton(android.R.string.ok, null)
				if (refreshed != null) {
					if (playback) builder.setPositiveButton(R.string.subtitle_lookup_reload) { _, _ -> if (isCurrent()) onRefreshed(refreshed) }
					else onRefreshed(refreshed)
				}
				replace(builder.create())
			} catch (error: CancellationException) {
				if (error is TimeoutCancellationException && isCurrent()) uncertain() else { dismiss(); throw error }
			} catch (error: Exception) {
				// Do not offer a download retry: the server may have completed the first POST.
				if (error is InvalidStatusException) failure(error, null) else uncertain()
			}
		}
	}
	private fun uncertain() = replace(AlertDialog.Builder(context).setTitle(R.string.subtitle_lookup_more)
		.setMessage(R.string.subtitle_lookup_uncertain).setNegativeButton(android.R.string.ok, null).create())
}
