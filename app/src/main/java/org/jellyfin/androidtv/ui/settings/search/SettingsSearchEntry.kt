package org.jellyfin.androidtv.ui.settings.search

import java.text.Normalizer
import java.util.Locale

data class SettingsSearchEntry(
	val id: String,
	val route: String,
	val title: String,
	val section: String,
	val description: String,
	val currentValue: String? = null,
)

private fun String.searchText() = Normalizer.normalize(this, Normalizer.Form.NFD)
	.replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

/** All words must match; setting names rank ahead of category/description matches. */
fun searchSettings(entries: List<SettingsSearchEntry>, query: String): List<SettingsSearchEntry> {
	val words = query.searchText().trim().split(Regex("\\s+")).filter(String::isNotEmpty)
	return entries.filter { entry ->
		val text = "${entry.title} ${entry.section} ${entry.description}".searchText()
		words.all(text::contains)
	}.sortedWith(compareBy<SettingsSearchEntry> { entry ->
		val title = entry.title.searchText()
		when {
			words.isEmpty() -> 0
			title == words.joinToString(" ") -> 0
			words.all(title::contains) -> 1
			else -> 2
		}
	}.thenBy { it.title.searchText() }.thenBy { it.id })
}
