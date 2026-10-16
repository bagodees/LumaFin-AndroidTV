package org.jellyfin.androidtv.ui.startup

/** Standard activity on top of Home: Back cancels selection without signing out. */
class AccountSelectionActivity : StartupActivity() {
	override val retainCurrentAccount = true
}
