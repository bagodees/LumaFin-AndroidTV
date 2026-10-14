package org.jellyfin.androidtv.util

/**
 * The version text shown in the app. Builds made between releases (which carry the placeholder
 * `0.0.0-dev` version) show the version they are heading toward, e.g. "LumaFin 0.2.0 dev debug";
 * a real release shows only its own version, so the dev and debug markers disappear once a
 * version is published.
 */
object AppVersionLabel {
	private const val DEV_VERSION_PREFIX = "0.0.0-dev"

	fun format(versionName: String, buildType: String, nextVersion: String): String =
		if (versionName.startsWith(DEV_VERSION_PREFIX)) "LumaFin $nextVersion dev $buildType"
		else "LumaFin $versionName $buildType"
}
