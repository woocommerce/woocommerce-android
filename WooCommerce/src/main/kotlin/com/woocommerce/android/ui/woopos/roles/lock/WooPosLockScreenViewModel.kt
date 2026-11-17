package com.woocommerce.android.ui.woopos.roles.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosLockScreenViewModel @Inject constructor(
    private val accessSession: WooPosAccessSession,
    selectedSite: SelectedSite,
) : ViewModel() {
    val state = accessSession.state
    init {
        viewModelScope.launch {
            selectedSite.observe().collectLatest { accessSession.initialize() }
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
                is WooPosLockScreenUIEvent.DigitTapped -> accessSession.enterDigit(event.digit)
                WooPosLockScreenUIEvent.DeleteTapped -> accessSession.deleteDigit()
                // TODO WOOMOB-4606: Open PIN recovery.
                WooPosLockScreenUIEvent.ForgotPINTapped -> accessSession.recover()
            }
        }
    }

    companion object {
        private const val COUNTDOWN_INTERVAL_MILLIS = 1000L
    }
}

sealed interface WooPosLockScreenUIEvent {
    data class DigitTapped(val digit: Int) : WooPosLockScreenUIEvent
    data object ForgotPINTapped : WooPosLockScreenUIEvent
    data object DeleteTapped : WooPosLockScreenUIEvent
}
