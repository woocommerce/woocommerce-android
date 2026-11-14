package com.woocommerce.android.ui.woopos.roles.lock

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
    val isChecking: Boolean = false,
    val remainingSeconds: Long = 0,
    val error: WooPosLockError? = null,
) {
    val canEnterPIN: Boolean
        get() = isLocked && !isChecking && remainingSeconds == 0L && error != WooPosLockError.STORAGE
}

enum class WooPosLockError { INCORRECT, STORAGE, RECOVERY_UNAVAILABLE }
