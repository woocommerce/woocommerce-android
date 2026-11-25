package com.woocommerce.android.ui.woopos.roles.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.pin.PIN_COUNTDOWN_INTERVAL_MILLIS
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.util.analytics.WooPosElapsedTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosLockScreenViewModel @Inject constructor(
    private val accessSession: WooPosAccessSession,
    selectedSite: SelectedSite,
    private val preferences: WooPosLockPreferences,
    private val clock: WooPosElapsedTime,
) : ViewModel() {
    private val _state = MutableStateFlow(
        accessSession.state.value.let { WooPosLockScreenState(isLocked = it.isLocked, status = it.status) }
    )
    val state = _state.asStateFlow()
    private val _exitRequested = MutableStateFlow(false)
    val exitRequested = _exitRequested.asStateFlow()
    val isExitInProgress: Boolean
        get() = _exitRequested.value || accessSession.state.value == WooPosAccessSessionState.Exiting
    private var pin = ""
    private var settings: WooPosAutoLockSettings? = null
    private var foreground: Boolean? = null
    private var lastInteraction: Long? = null
    private var inactivityJob: Job? = null

    init {
        viewModelScope.launch {
            accessSession.approvalScopeChanges.collect { accessSession.retireIfContextChanged() }
        }
        viewModelScope.launch {
            combine(accessSession.state, accessSession.paymentState) { session, _ -> session }.collect { session ->
                if (foreground == false && session is WooPosAccessSessionState.Unlocked &&
                    settings?.onBackground != false
                ) {
                    viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { accessSession.lockOnBackground() }
                    return@collect
                }
                lastInteraction = if (session.isLocked || accessSession.isPaymentPending) null else clock.milliseconds()
                restartInactivity()
                pin = ""
                val isStillChecking = _state.value.status == WooPosPINEntryStatus.Checking &&
                    session.status == WooPosPINEntryStatus.Checking
                val enteredDigits = if (isStillChecking) _state.value.enteredDigits else 0
                _state.value = WooPosLockScreenState(
                    isLocked = session.isLocked,
                    enteredDigits = enteredDigits,
                    status = if (session.isLocked && accessSession.isPaymentPending) {
                        WooPosPINEntryStatus.PaymentPending
                    } else {
                        session.status
                    },
                )
            }
        }
        viewModelScope.launch {
            selectedSite.observe()
                .distinctUntilChangedBy { it?.id }
                .collectLatest { site ->
                    settings = null
                    inactivityJob?.cancel()
                    accessSession.initialize()
                    if (site != null) {
                        preferences.settings(site.id).collect { result ->
                            settings = result.getOrNull()
                            if (result.isFailure) accessSession.blockForStorageFailure()
                            lastInteraction = if (accessSession.state.value is WooPosAccessSessionState.Unlocked &&
                                !accessSession.isPaymentPending
                            ) {
                                clock.milliseconds()
                            } else {
                                null
                            }
                            restartInactivity()
                        }
                    }
                }
        }
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

    fun onForeground() {
        foreground = true
        restartInactivity()
        if (state.value.isLocked && accessSession.isPaymentPending) {
            viewModelScope.launch { accessSession.lockOnBackground() }
        }
    }

    fun onBackground() {
        foreground = false
        inactivityJob?.cancel()
        if (settings?.onBackground != false) {
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { accessSession.lockOnBackground() }
        }
    }

    fun onLauncherEntry() {
        if (settings?.onLaunch != false) {
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { accessSession.lockOnBackground() }
        }
    }

    fun onUserInteraction() {
        lastInteraction = null
        restartInactivity()
    }

    fun onAuthorizedExit() {
        viewModelScope.launch { accessSession.exitPOS { _exitRequested.value = true } }
    }

    private fun restartInactivity() {
        inactivityJob?.cancel()
        val minutes = settings?.inactivityMinutes ?: return
        val canRun = foreground == true && accessSession.canLock &&
            accessSession.state.value is WooPosAccessSessionState.Unlocked
        if (minutes == 0 || !canRun) return
        val now = clock.milliseconds()
        val started = lastInteraction ?: now.also { lastInteraction = it }
        val remaining = (minutes * MILLIS_PER_MINUTE - (now - started)).coerceAtLeast(0)
        inactivityJob = viewModelScope.launch {
            delay(remaining)
            inactivityJob = null
            accessSession.lock()
        }
    }

    fun onUIEvent(event: WooPosLockScreenUIEvent) {
        viewModelScope.launch {
            when (event) {
                is WooPosLockScreenUIEvent.DigitTapped -> enterDigit(event.digit)
                WooPosLockScreenUIEvent.DeleteTapped -> deleteDigit()
                WooPosLockScreenUIEvent.CheckPaymentStatus -> accessSession.lockOnBackground()
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

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}

sealed interface WooPosLockScreenUIEvent {
    data class DigitTapped(val digit: Int) : WooPosLockScreenUIEvent
    data object CheckPaymentStatus : WooPosLockScreenUIEvent
    data object DeleteTapped : WooPosLockScreenUIEvent
}
