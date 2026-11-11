package com.woocommerce.android.ui.woopos.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosLockScreenViewModel @Inject constructor(
    private val lockSession: WooPosLockSession,
    selectedSite: SelectedSite,
) : ViewModel() {
    val state = lockSession.state
    private val _exitPOS = Channel<Unit>(Channel.BUFFERED)
    val exitPOS = _exitPOS.receiveAsFlow()

    fun requestExitPOS() {
        viewModelScope.launch {
            if (lockSession.exitPOS()) _exitPOS.send(Unit)
        }
    }

    init {
        viewModelScope.launch {
            selectedSite.observe().collectLatest { lockSession.initialize() }
        }
        viewModelScope.launch {
            state.map { it.isLocked && it.remainingSeconds > 0 }.distinctUntilChanged().collectLatest { waiting ->
                if (waiting) {
                    while (state.value.remainingSeconds > 0) {
                        delay(COUNTDOWN_INTERVAL_MILLIS)
                        lockSession.refreshWait()
                    }
                }
            }
        }
    }

    fun onUIEvent(event: WooPosLockScreenUIEvent) {
        viewModelScope.launch {
            when (event) {
                is WooPosLockScreenUIEvent.DigitTapped -> lockSession.enterDigit(event.digit)
                WooPosLockScreenUIEvent.DeleteTapped -> lockSession.deleteDigit()
                WooPosLockScreenUIEvent.ForgotPINTapped -> lockSession.recover()
            }
        }
    }

    companion object {
        private const val COUNTDOWN_INTERVAL_MILLIS = 1000L
    }
}

sealed interface WooPosLockScreenUIEvent {
    data class DigitTapped(val digit: Int) : WooPosLockScreenUIEvent
    data object DeleteTapped : WooPosLockScreenUIEvent
    data object ForgotPINTapped : WooPosLockScreenUIEvent
}
