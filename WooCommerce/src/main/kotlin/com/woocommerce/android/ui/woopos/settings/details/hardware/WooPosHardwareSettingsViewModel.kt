package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosHardwareSettingsViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(WooPosHardwareSettingsState())
    val state: StateFlow<WooPosHardwareSettingsState> = _state.asStateFlow()
}
