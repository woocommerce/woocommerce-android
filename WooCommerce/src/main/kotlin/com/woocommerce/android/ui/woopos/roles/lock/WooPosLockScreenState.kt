package com.woocommerce.android.ui.woopos.roles.lock

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
    val status: Status = Status.Idle,
) {
    val canEnterPIN: Boolean
        get() = isLocked && (status == Status.Idle || status == Status.Incorrect)

    sealed interface Status {
        data object Initializing : Status
        data object Idle : Status
        data object Checking : Status
        data object Incorrect : Status
        data class Wait(val remainingSeconds: Long) : Status
        data object StorageUnavailable : Status
    }
}
