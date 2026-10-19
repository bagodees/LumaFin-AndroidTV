package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class CardLongPressAction(override val nameRes: Int, val descriptionRes: Int) : PreferenceEnum {
	MENU(R.string.remote_hold_menu, R.string.remote_hold_menu_help),
	DETAILS(R.string.remote_hold_details, R.string.remote_hold_details_help),
	PLAY(R.string.remote_hold_play, R.string.remote_hold_play_help),
	DISABLED(R.string.remote_hold_disabled, R.string.remote_hold_disabled_help),
}
