package com.woocommerce.android.ui.woopos.roles.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosLockScreenViewModel @Inject constructor(
    private val accessSession: WooPosAccessSession,
    selectedSite: SelectedSite,
) : ViewModel() {
    private val _state = MutableStateFlow(
        accessSession.state.value.let { WooPosLockScreenState(isLocked = it.isLocked, status = it.status) }
    )
    val state = _state.asStateFlow()
    private var pin = ""

    init {
        viewModelScope.launch {
            accessSession.state.collect { session ->
                pin = ""
                _state.value = WooPosLockScreenState(isLocked = session.isLocked, status = session.status)
            }
        }
        viewModelScope.launch {
            selectedSite.observe()
                .distinctUntilChangedBy { it?.id }
                .collectLatest { accessSession.initialize() }
        }
        viewModelScope.launch {
            state.map { it.isLocked && it.status is Status.Wait }
                .distinctUntilChanged()
                .collectLatest { waiting ->
                    if (waiting) {
                        while (state.value.status is Status.Wait) {
                            delay(COUNTDOWN_INTERVAL_MILLIS)
                            accessSession.refreshWait()
                        }
                    }
                }
        }
    }

    fun onUIEvent(event: WooPosLockScreenUIEvent) {
        viewModelScope.launch {
            when (event) {
                is WooPosLockScreenUIEvent.DigitTapped -> enterDigit(event.digit)
                WooPosLockScreenUIEvent.DeleteTapped -> deleteDigit()
                WooPosLockScreenUIEvent.ForgotPINTapped -> {
                    pin = ""
                    accessSession.recover()
                }
            }
        }
    }

    private suspend fun enterDigit(digit: Int) {
        if (digit !in MIN_DIGIT..MAX_DIGIT || !_state.value.canEnterPIN) return
        pin += digit
        _state.value = _state.value.copy(enteredDigits = pin.length, status = Status.Idle)
        if (pin.length == WooPosPINHash.PIN_LENGTH) {
            val submitted = pin
            pin = ""
            _state.value = _state.value.copy(enteredDigits = 0, status = Status.Checking)
            accessSession.unlock(submitted)
        }
    }

    private fun deleteDigit() {
        if (!_state.value.canEnterPIN) return
        pin = pin.dropLast(1)
        _state.value = _state.value.copy(enteredDigits = pin.length, status = Status.Idle)
    }

    companion object {
        private const val COUNTDOWN_INTERVAL_MILLIS = 1000L
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
    }
}

sealed interface WooPosLockScreenUIEvent {
    data class DigitTapped(val digit: Int) : WooPosLockScreenUIEvent
    data object ForgotPINTapped : WooPosLockScreenUIEvent
    data object DeleteTapped : WooPosLockScreenUIEvent
}
