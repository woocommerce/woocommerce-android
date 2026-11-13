package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.lock.WooPosLockError
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenState
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenUIEvent
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class WooPosSettingsSecurityViewModel @Inject constructor(
    private val mockSetup: WooPosMockPinSetup,
    featureFlagRepository: FeatureFlagRepository,
) : ViewModel() {
    val isEnabled = featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)
    private val setup: WooPosPinSetup = mockSetup
    val setupState = setup.state
    private val _state = MutableStateFlow(WooPosSettingsSecurityState())
    val state = _state.asStateFlow()
    private var countdown: Job? = null

    fun onSetupEvent(event: WooPosPinSetupUIEvent) {
        if (isEnabled) setup.onUIEvent(event)
    }

    fun showSetup(state: WooPosPinSetupState = WooPosPinSetupState()) {
        if (!isEnabled) return
        countdown?.cancel()
        mockSetup.show(state)
        _state.value = WooPosSettingsSecurityState(showSetup = true)
    }

    fun showLock(state: WooPosLockScreenState) {
        if (!isEnabled) return
        countdown?.cancel()
        _state.value = WooPosSettingsSecurityState(lock = state.copy(isLocked = true))
        if (state.remainingSeconds > 0) {
            countdown = viewModelScope.launch {
                while ((_state.value.lock?.remainingSeconds ?: 0) > 0) {
                    delay(1.seconds)
                    _state.update { current ->
                        current.copy(lock = current.lock?.let { it.copy(remainingSeconds = it.remainingSeconds - 1) })
                    }
                }
            }
        }
    }

    fun onLockEvent(event: WooPosLockScreenUIEvent) {
        if (!isEnabled) return
        _state.update { current ->
            val lock = current.lock ?: return@update current
            val updated = when {
                event == WooPosLockScreenUIEvent.ForgotPINTapped && lock.error != WooPosLockError.STORAGE -> lock.copy(
                    error = WooPosLockError.RECOVERY_UNAVAILABLE
                )
                !lock.canEnterPIN -> lock
                event == WooPosLockScreenUIEvent.DeleteTapped -> lock.copy(
                    enteredDigits = (lock.enteredDigits - 1).coerceAtLeast(0)
                )
                event is WooPosLockScreenUIEvent.DigitTapped && event.digit in 0..MAX_DIGIT -> {
                    val digits = lock.enteredDigits + 1
                    lock.copy(
                        enteredDigits = digits.coerceAtMost(WooPosLockScreenState.PIN_LENGTH),
                        isChecking = digits >= WooPosLockScreenState.PIN_LENGTH,
                        error = null,
                    )
                }
                else -> lock
            }
            current.copy(lock = updated)
        }
    }

    fun close() {
        countdown?.cancel()
        mockSetup.show(WooPosPinSetupState())
        _state.value = WooPosSettingsSecurityState()
    }

    private companion object {
        const val MAX_DIGIT = 9
    }
}

data class WooPosSettingsSecurityState(
    val showSetup: Boolean = false,
    val lock: WooPosLockScreenState? = null,
)
