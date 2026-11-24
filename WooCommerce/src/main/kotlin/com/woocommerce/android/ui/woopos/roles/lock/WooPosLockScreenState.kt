package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
    val status: WooPosPINEntryStatus = WooPosPINEntryStatus.Idle,
) {
    val canEnterPIN: Boolean
        get() = isLocked && status.canEnterPIN
}
