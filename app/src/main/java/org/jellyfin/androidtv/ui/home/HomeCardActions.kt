package org.jellyfin.androidtv.ui.home

import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType
import org.jellyfin.androidtv.util.sdk.canPlay
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Remote shortcuts on the LumaFin Home: OK on a video card plays (or resumes) it and holding OK
 * opens its details, so the preview needs no buttons. Other cards keep their usual OK action.
 */
object HomeCardActions {
	private val playTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO, BaseItemKind.TRAILER)

	@JvmStatic
	fun playsOnClick(item: BaseRowItem?): Boolean {
		if (item?.baseRowType != BaseRowType.BaseItem || !SideMenuFrame.isEnabled()) return false
		val baseItem = item.baseItem ?: return false
		return baseItem.type in playTypes && baseItem.canPlay()
	}
}
