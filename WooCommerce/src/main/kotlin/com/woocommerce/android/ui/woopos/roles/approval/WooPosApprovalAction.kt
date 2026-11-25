package com.woocommerce.android.ui.woopos.roles.approval

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.roles.WooPosCapability

internal sealed class WooPosApprovalAction(
    val capability: WooPosCapability,
    @StringRes val reason: Int,
) {
    open val requiresDurableCompletion: Boolean get() = false

    data object CreateCoupon : WooPosApprovalAction(
        WooPosCapability.CREATE_COUPONS,
        R.string.woopos_approval_create_coupon,
    )

    data class IssueRefund(val orderNumber: String) : WooPosApprovalAction(
        WooPosCapability.ISSUE_REFUNDS,
        R.string.woopos_approval_issue_refund,
    )

    data object ChangeAdminPIN : WooPosApprovalAction(
        WooPosCapability.MANAGE_STORE_SETTINGS,
        R.string.woopos_security_approve_admin_change,
    )

    data object DisablePINProtection : WooPosApprovalAction(
        WooPosCapability.MANAGE_STORE_SETTINGS,
        R.string.woopos_security_approve_disable,
    ) {
        override val requiresDurableCompletion: Boolean get() = true
    }

    data object Exit : WooPosApprovalAction(WooPosCapability.EXIT_POS, R.string.woopos_approval_exit)
}
