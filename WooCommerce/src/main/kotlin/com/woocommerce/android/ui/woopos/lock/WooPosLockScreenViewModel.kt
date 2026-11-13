package com.woocommerce.android.ui.woopos.lock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosLockScreenViewModel @Inject constructor(
    private val lockSession: WooPosLockSession,
) : ViewModel() {
    val state = lockSession.state

    fun onUIEvent(event: WooPosLockScreenUIEvent) {
        when (event) {
            is WooPosLockScreenUIEvent.DigitTapped -> lockSession.enterDigit(event.digit)
            WooPosLockScreenUIEvent.DeleteTapped -> lockSession.deleteDigit()
        }
    }
}

sealed interface WooPosLockScreenUIEvent {
    data class DigitTapped(val digit: Int) : WooPosLockScreenUIEvent
    data object DeleteTapped : WooPosLockScreenUIEvent
}
