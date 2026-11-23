package com.woocommerce.android.ui.woopos.roles.approval

import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status

internal data class WooPosManagerApprovalState(
    val action: WooPosApprovalAction,
    val status: Status,
    val enteredDigits: Int = 0,
) {
    val canEnterPIN: Boolean get() = status == Status.Idle || status == Status.Incorrect
}
