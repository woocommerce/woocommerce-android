package com.woocommerce.android.ui.woopos.roles.pin

sealed interface WooPosPINEntryStatus {
    val canEnterPIN: Boolean get() = this == Idle || this == Incorrect
    val showsBlockedPINPad: Boolean get() = this is Wait || this == StorageUnavailable

    data object Initializing : WooPosPINEntryStatus
    data object Idle : WooPosPINEntryStatus
    data object Checking : WooPosPINEntryStatus
    data object Incorrect : WooPosPINEntryStatus
    data class Wait(val remainingSeconds: Long) : WooPosPINEntryStatus
    data object StorageUnavailable : WooPosPINEntryStatus
}

internal const val PIN_COUNTDOWN_INTERVAL_MILLIS = 1_000L
