package org.jellyfin.androidtv.ui.settings.compat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
	private val _visible = MutableStateFlow(false)
	val visible get() = _visible.asStateFlow()
	var openSetupGuide = false
		private set

	fun show() {
		openSetupGuide = false
		_visible.value = true
	}

	fun showSetupGuide() {
		openSetupGuide = true
		_visible.value = true
	}

	fun hide() {
		_visible.value = false
	}
}
