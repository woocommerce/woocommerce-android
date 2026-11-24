package com.woocommerce.android.ui.woopos.roles.approval

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.roles.WooPosCapability

internal sealed class WooPosManagerApprovalAction(
    val capability: WooPosCapability,
    @StringRes val reason: Int,
) {
    open val requiresDurableCompletion: Boolean get() = false

    data object CreateCoupon : WooPosManagerApprovalAction(
        WooPosCapability.CREATE_COUPONS,
        R.string.woopos_approval_create_coupon,
    )

    data class IssueRefund(val orderNumber: String) : WooPosManagerApprovalAction(
        WooPosCapability.ISSUE_REFUNDS,
        R.string.woopos_approval_issue_refund,
    )

    data object ChangeAdminPIN : WooPosManagerApprovalAction(
        WooPosCapability.MANAGE_STORE_SETTINGS,
        R.string.woopos_security_approve_admin_change,
    )

    data object DisablePINProtection : WooPosManagerApprovalAction(
        WooPosCapability.MANAGE_STORE_SETTINGS,
        R.string.woopos_security_approve_disable,
    ) {
        override val requiresDurableCompletion: Boolean get() = true
    }

    data object Exit : WooPosManagerApprovalAction(WooPosCapability.EXIT_POS, R.string.woopos_approval_exit)
}
