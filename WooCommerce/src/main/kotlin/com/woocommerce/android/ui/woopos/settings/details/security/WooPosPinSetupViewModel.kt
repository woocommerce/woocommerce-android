package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WooPosPinSetupViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WooPosLocalPINStore,
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosPinSetupState())
    val state = _state.asStateFlow()
    private var pin = ""
    private var generation = 0L
    private var isSaving = false

    fun reset() {
        generation++
        pin = ""
        isSaving = false
        _state.value = WooPosPinSetupState()
    }

    fun onUIEvent(event: WooPosPinSetupUIEvent) {
        if (isSaving || !_state.value.canEnterPIN) return
        when (event) {
            is WooPosPinSetupUIEvent.DigitTapped -> {
                if (event.digit !in MIN_DIGIT..MAX_DIGIT) return
                pin += event.digit
                _state.value = _state.value.copy(enteredDigits = pin.length)
                if (pin.length == WooPosPINHash.PIN_LENGTH) save()
            }
            WooPosPinSetupUIEvent.DeleteTapped -> {
                pin = pin.dropLast(1)
                _state.value = _state.value.copy(enteredDigits = pin.length)
            }
            WooPosPinSetupUIEvent.BackTapped -> reset()
        }
    }

    private fun save() {
        val id = selectedSite.getOrNull()?.id ?: return reset()
        val submitted = pin
        pin = ""
        isSaving = true
        val request = generation
        viewModelScope.launch {
            val pins = store.getPINs(id)
            if (!isCurrent(request, id)) {
                if (request == generation) reset()
                return@launch
            }
            val canCreate = pins.getOrNull()?.let { WooPosLocalRole.ADMIN !in it } == true
            val setupSaved = withContext(NonCancellable) {
                val pinSaved = canCreate && store.setPIN(id, WooPosLocalRole.ADMIN, submitted).isSuccess
                if (pinSaved && id == selectedSite.getOrNull()?.id) {
                    accessSession.initialize(unlockWith = submitted)
                }
                pinSaved
            }
            if (request != generation) return@launch
            isSaving = false
            _state.value = if (setupSaved && id == selectedSite.getOrNull()?.id) {
                WooPosPinSetupState(stage = WooPosPinSetupState.Stage.SUCCESS)
            } else {
                WooPosPinSetupState()
            }
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        request == generation && id == selectedSite.getOrNull()?.id

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
    }
}
