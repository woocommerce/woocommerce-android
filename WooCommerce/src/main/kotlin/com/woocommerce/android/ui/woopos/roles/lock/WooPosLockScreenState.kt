package com.woocommerce.android.ui.woopos.roles.lock

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
    val status: Status = Status.Idle,
) {
    val canEnterPIN: Boolean
        get() = isLocked && status.canEnterPIN

    sealed interface Status {
        val canEnterPIN: Boolean get() = this == Idle || this == Incorrect
        val showsBlockedPINPad: Boolean get() = this is Wait || this == StorageUnavailable

        data object Initializing : Status
        data object Idle : Status
        data object Checking : Status
        data object Incorrect : Status
        data class Wait(val remainingSeconds: Long) : Status
        data object StorageUnavailable : Status
    }
}
