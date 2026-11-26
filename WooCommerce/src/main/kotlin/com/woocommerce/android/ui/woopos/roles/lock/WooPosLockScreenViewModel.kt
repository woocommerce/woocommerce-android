package com.woocommerce.android.ui.woopos.roles.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.pin.PIN_COUNTDOWN_INTERVAL_MILLIS
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
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
    private val autoLock: WooPosAutoLock,
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
                val isStillChecking = _state.value.status == WooPosPINEntryStatus.Checking &&
                    session.status == WooPosPINEntryStatus.Checking
                val enteredDigits = if (isStillChecking) _state.value.enteredDigits else 0
                _state.value = WooPosLockScreenState(
                    isLocked = session.isLocked,
                    enteredDigits = enteredDigits,
                    status = session.status,
                )
            }
        }
        viewModelScope.launch {
            selectedSite.observe()
                .distinctUntilChangedBy { it?.id }
                .collectLatest { accessSession.initialize() }
        }
        viewModelScope.launch { autoLock.lockAfterInactivity() }
        viewModelScope.launch {
            state.map { it.isLocked && it.status is WooPosPINEntryStatus.Wait }
                .distinctUntilChanged()
                .collectLatest { waiting ->
                    if (waiting) {
                        while (state.value.status is WooPosPINEntryStatus.Wait) {
                            delay(PIN_COUNTDOWN_INTERVAL_MILLIS)
                            accessSession.refreshWait()
                        }
                    }
                }
        }
    }

    fun onAppBackgrounded() {
        viewModelScope.launch { autoLock.onAppBackgrounded() }
    }

    override fun onCleared() {
        accessSession.end()
    }

    fun onUIEvent(event: WooPosLockScreenUIEvent) {
        viewModelScope.launch {
            when (event) {
                is WooPosLockScreenUIEvent.DigitTapped -> enterDigit(event.digit)
                WooPosLockScreenUIEvent.DeleteTapped -> deleteDigit()
            }
        }
    }

    private suspend fun enterDigit(digit: Int) {
        if (digit !in WooPosPINHash.PIN_DIGIT_RANGE || !_state.value.canEnterPIN) return
        pin += digit
        _state.value = _state.value.copy(enteredDigits = pin.length, status = WooPosPINEntryStatus.Idle)
        if (pin.length == WooPosPINHash.PIN_LENGTH) {
            val submitted = pin
            pin = ""
            _state.value = _state.value.copy(status = WooPosPINEntryStatus.Checking)
            accessSession.unlock(submitted)
        }
    }

    private fun deleteDigit() {
        if (!_state.value.canEnterPIN) return
        pin = pin.dropLast(1)
        _state.value = _state.value.copy(enteredDigits = pin.length, status = WooPosPINEntryStatus.Idle)
    }
}

sealed interface WooPosLockScreenUIEvent {
    data class DigitTapped(val digit: Int) : WooPosLockScreenUIEvent
    data object DeleteTapped : WooPosLockScreenUIEvent
}
