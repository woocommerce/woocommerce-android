package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosLockSession @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
) {
    private val _state = MutableStateFlow(WooPosLockScreenState())
    val state = _state.asStateFlow()

    val isEnabled: Boolean
        get() = featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)

    fun lock() {
        if (isEnabled) _state.value = WooPosLockScreenState(isLocked = true)
    }

    fun enterDigit(digit: Int) {
        if (digit !in MIN_DIGIT..MAX_DIGIT) return
        // Until WOOMOB-4605 adds PIN validation, this internal increment accepts any four digits.
        _state.update { current ->
            when {
                !current.isLocked -> current
                current.enteredDigits == WooPosPINHash.PIN_LENGTH - 1 -> WooPosLockScreenState()
                else -> current.copy(enteredDigits = current.enteredDigits + 1)
            }
        }
    }

    fun deleteDigit() {
        _state.update { it.copy(enteredDigits = (it.enteredDigits - 1).coerceAtLeast(0)) }
    }

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
    }
}

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
)
