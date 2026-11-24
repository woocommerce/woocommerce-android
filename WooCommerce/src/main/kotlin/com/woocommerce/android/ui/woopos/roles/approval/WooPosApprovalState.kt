package com.woocommerce.android.ui.woopos.roles.approval

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus

internal data class WooPosApprovalState(
    val action: WooPosApprovalAction,
    val status: WooPosPINEntryStatus,
    val enteredDigits: Int = 0,
) {
    val canDismiss: Boolean
        get() = action != WooPosManagerApprovalAction.DisablePINProtection || status != WooPosPINEntryStatus.Checking
}
