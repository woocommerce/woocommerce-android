package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsSecurityPINSetupViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WooPosLocalPINStore,
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsSecurityPINSetupState())
    val state = _state.asStateFlow()
    private var pin = ""
    private var saveJob: Job? = null

    fun reset() {
        saveJob?.cancel()
        saveJob = null
        pin = ""
        _state.value = WooPosSettingsSecurityPINSetupState()
    }

    fun onUIEvent(event: WooPosSettingsSecurityPINSetupUIEvent) {
        if (_state.value.stage != WooPosSettingsSecurityPINSetupState.Stage.CHOOSE) return
        when (event) {
            is WooPosSettingsSecurityPINSetupUIEvent.DigitTapped -> {
                if (event.digit !in MIN_DIGIT..MAX_DIGIT) return
                pin += event.digit
                _state.value = _state.value.copy(enteredDigits = pin.length)
                if (pin.length == WooPosPINHash.PIN_LENGTH) save()
            }
            WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped -> {
                pin = pin.dropLast(1)
                _state.value = _state.value.copy(enteredDigits = pin.length)
            }
            WooPosSettingsSecurityPINSetupUIEvent.BackTapped -> reset()
        }
    }

    private fun save() {
        val id = selectedSite.getOrNull()?.id ?: return reset()
        val submitted = pin
        pin = ""
        _state.value = WooPosSettingsSecurityPINSetupState(stage = WooPosSettingsSecurityPINSetupState.Stage.CHECKING)
        saveJob = viewModelScope.launch {
            val pins = store.getPINs(id)
            ensureActive()
            if (id != selectedSite.getOrNull()?.id) {
                _state.value = WooPosSettingsSecurityPINSetupState()
                return@launch
            }
            val canCreate = pins.getOrNull()?.let { WooPosLocalRole.ADMIN !in it } == true
            val setupSaved = withContext(NonCancellable) {
                val pinSaved = canCreate && store.setPIN(id, WooPosLocalRole.ADMIN, submitted).isSuccess
                if (pinSaved && id == selectedSite.getOrNull()?.id) accessSession.unlock(submitted)
                pinSaved
            }
            ensureActive()
            _state.value = if (setupSaved && id == selectedSite.getOrNull()?.id) {
                WooPosSettingsSecurityPINSetupState(stage = WooPosSettingsSecurityPINSetupState.Stage.SUCCESS)
            } else {
                WooPosSettingsSecurityPINSetupState()
            }
        }
    }

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
    }
}
