package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.common.pin.WOO_POS_PIN_LENGTH

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
    val isChecking: Boolean = false,
    val remainingSeconds: Long = 0,
    val error: WooPosLockError? = null,
) {
    val canEnterPIN: Boolean
        get() = isLocked && !isChecking && remainingSeconds == 0L && error != WooPosLockError.STORAGE

    companion object {
        const val PIN_LENGTH = WOO_POS_PIN_LENGTH
    }
}

enum class WooPosLockError { INCORRECT, STORAGE, RECOVERY_UNAVAILABLE }
