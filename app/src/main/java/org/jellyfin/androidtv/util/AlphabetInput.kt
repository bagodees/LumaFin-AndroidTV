package org.jellyfin.androidtv.util

import java.text.Normalizer

object AlphabetInput {
	@JvmStatic
	fun normalize(value: String): String? {
		val letter = Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
		return letter.takeIf { it.codePointCount(0, it.length) == 1 && Character.isLetterOrDigit(it.codePointAt(0)) }
	}
}
